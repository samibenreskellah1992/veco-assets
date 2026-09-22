package dz.vecopharm.vecoassets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dz.vecopharm.vecoassets.AbstractIntegrationTest;
import dz.vecopharm.vecoassets.dto.AssetCreateRequest;
import dz.vecopharm.vecoassets.dto.LocationRequest;
import dz.vecopharm.vecoassets.dto.LoginRequest;
import dz.vecopharm.vecoassets.dto.ScanRequest;
import dz.vecopharm.vecoassets.entity.AssetCategory;
import dz.vecopharm.vecoassets.entity.Building;
import dz.vecopharm.vecoassets.entity.Floor;
import dz.vecopharm.vecoassets.entity.Role;
import dz.vecopharm.vecoassets.entity.ScanResult;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.entity.Zone;
import dz.vecopharm.vecoassets.repository.AssetCategoryRepository;
import dz.vecopharm.vecoassets.repository.BuildingRepository;
import dz.vecopharm.vecoassets.repository.FloorRepository;
import dz.vecopharm.vecoassets.repository.RoleRepository;
import dz.vecopharm.vecoassets.repository.SiteRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import dz.vecopharm.vecoassets.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout-en-bout (Testcontainers) de {@code /api/location-inventory-sessions}
 * - Checkpoint 3 "locaux scannables" (2026-09), ajoutes apres la
 * verification live du checkpoint (voir doc de suivi du projet) sur le
 * meme modele que {@link LocationControllerIntegrationTest} et {@code
 * InventoryCampaignControllerIntegrationTest} : matrice de permissions
 * {@code INVENTAIRE_EXECUTE}/{@code INVENTAIRE_VALIDATE} verifiee cote
 * HTTP, ET regression directe (au niveau API, pas seulement service) de
 * deux comportements verifies manuellement pendant la recette live :
 * le blocage d'une seconde session concurrente sur le meme local, et le
 * cheminement complet scan present / mauvaise localisation / non
 * referencee / validation avec mise a jour de {@code
 * Location.lastInventoryAt}.
 */
@AutoConfigureMockMvc
@Transactional
class LocationInventorySessionControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Test#Pass2026";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private FloorRepository floorRepository;
    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private AssetCategoryRepository categoryRepository;

    private Site site;
    private Building building;
    private Floor floor;
    private Zone zone;
    private AssetCategory category;
    private String gestionnaireToken;
    private String inventoristeToken;
    private String consultationToken;

    @BeforeEach
    void setUp() throws Exception {
        site = siteRepository.save(site());
        building = buildingRepository.save(building(site));
        floor = floorRepository.save(floor(building));
        zone = zoneRepository.save(zone(floor));

        category = new AssetCategory();
        category.setCode("INFO-SESS");
        category.setName("Informatique session test");
        category = categoryRepository.save(category);

        Role gestionnaireRole = roleRepository.findByCode("GESTIONNAIRE_PATRIMOINE").orElseThrow();
        Role inventoristeRole = roleRepository.findByCode("INVENTORISTE").orElseThrow();
        Role consultationRole = roleRepository.findByCode("CONSULTATION").orElseThrow();

        gestionnaireToken = login(newUser("gestionnaire.session.test@vecopharm.dz", gestionnaireRole));
        inventoristeToken = login(newUser("inventoriste.session.test@vecopharm.dz", inventoristeRole));
        consultationToken = login(newUser("consultation.session.test@vecopharm.dz", consultationRole));
    }

    @Test
    void open_withInventaireExecutePermission_createsEnCoursSession() throws Exception {
        String locationId = createLocation("BUR-S1", "Bureau session 1");

        mockMvc.perform(post("/api/location-inventory-sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + inventoristeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationId\":\"" + locationId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.locationId").value(locationId))
                .andExpect(jsonPath("$.status").value("EN_COURS"));
    }

    @Test
    void open_withoutTokenIsUnauthorized() throws Exception {
        String locationId = createLocation("BUR-S1", "Bureau session 1");

        mockMvc.perform(post("/api/location-inventory-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationId\":\"" + locationId + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void open_withConsultationPermissionIsForbidden() throws Exception {
        String locationId = createLocation("BUR-S1", "Bureau session 1");

        mockMvc.perform(post("/api/location-inventory-sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationId\":\"" + locationId + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void open_rejectsASecondConcurrentSessionOnTheSameLocation() throws Exception {
        // Regression HTTP directe du controle verifie manuellement pendant
        // la recette live du Checkpoint 3 : une deuxieme session EN_COURS
        // sur le meme local doit etre refusee, meme au niveau API direct
        // (pas seulement empechee par le bouton cote UI).
        String locationId = createLocation("BUR-S1", "Bureau session 1");
        openSession(locationId, inventoristeToken);

        mockMvc.perform(post("/api/location-inventory-sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + inventoristeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationId\":\"" + locationId + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Une session de scan est deja en cours pour ce local"));
    }

    @Test
    void validate_requiresInventaireValidatePermission_inventoristeForbiddenGestionnaireAllowed() throws Exception {
        String locationId = createLocation("BUR-S1", "Bureau session 1");
        String sessionId = openSession(locationId, inventoristeToken);

        mockMvc.perform(post("/api/location-inventory-sessions/" + sessionId + "/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + inventoristeToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/location-inventory-sessions/" + sessionId + "/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDEE"));
    }

    @Test
    void fullScanFlow_presentThenMauvaiseLocalisationThenNonReferencee_thenValidateStampsLastInventoryAt() throws Exception {
        String locationAId = createLocation("BUR-S1", "Bureau session 1");
        String locationBId = createLocation("BUR-S2", "Bureau session 2");

        String inScopeAssetCode = createAssetAt(locationAId);
        String outOfScopeAssetCode = createAssetAt(locationBId);

        String sessionId = openSession(locationAId, inventoristeToken);

        // 1) Immobilisation reellement rattachee a ce local -> PRESENT, pas d'anomalie.
        mockMvc.perform(post("/api/location-inventory-sessions/" + sessionId + "/scans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + inventoristeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScanRequest(inScopeAssetCode, ScanResult.PRESENT, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetRecognized").value(true))
                .andExpect(jsonPath("$.anomaly").doesNotExist());

        // 2) Immobilisation rattachee a un AUTRE local -> anomalie MAUVAISE_LOCALISATION automatique,
        // quel que soit le resultat demande (ici PRESENT), jamais de deplacement automatique (BR-LOC-006).
        mockMvc.perform(post("/api/location-inventory-sessions/" + sessionId + "/scans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + inventoristeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScanRequest(outOfScopeAssetCode, ScanResult.PRESENT, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetRecognized").value(true))
                .andExpect(jsonPath("$.anomaly.anomalyType").value("MAUVAISE_LOCALISATION"));

        // 3) Code inconnu -> anomalie NON_REFERENCEE, aucune ligne de scan.
        mockMvc.perform(post("/api/location-inventory-sessions/" + sessionId + "/scans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + inventoristeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScanRequest("CODE-INCONNU-TEST", ScanResult.PRESENT, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetRecognized").value(false))
                .andExpect(jsonPath("$.scan").doesNotExist())
                .andExpect(jsonPath("$.anomaly.anomalyType").value("NON_REFERENCEE"));

        // Progression : un seul actif attendu dans le perimetre du local A, scanne et present ;
        // les 2 anomalies (mauvaise localisation + non referencee) sont comptees, aucune ne
        // reduit le nombre de "restantes" car ni l'une ni l'autre n'est l'unique actif attendu.
        mockMvc.perform(get("/api/location-inventory-sessions/" + sessionId + "/progress")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssetsExpected").value(1))
                .andExpect(jsonPath("$.scannedAssetsCount").value(1))
                .andExpect(jsonPath("$.presentCount").value(1))
                .andExpect(jsonPath("$.anomaliesCount").value(2))
                .andExpect(jsonPath("$.remainingCount").value(0))
                .andExpect(jsonPath("$.progressPercent").value(100.0));

        mockMvc.perform(get("/api/location-inventory-sessions/" + sessionId + "/anomalies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Validation : fige la session ET stamp Location.lastInventoryAt (jamais ecrit avant ce checkpoint).
        mockMvc.perform(post("/api/location-inventory-sessions/" + sessionId + "/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDEE"))
                .andExpect(jsonPath("$.validatedAt").exists());

        mockMvc.perform(get("/api/locations/" + locationAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastInventoryAt").exists());

        // Une session VALIDEE n'accepte plus aucun scan (verifie ici cote serveur,
        // mirroir exact du message vu en direct dans le navigateur pendant la recette).
        mockMvc.perform(post("/api/location-inventory-sessions/" + sessionId + "/scans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + inventoristeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScanRequest(inScopeAssetCode, ScanResult.PRESENT, null, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }

    // --- Helpers -------------------------------------------------

    private String openSession(String locationId, String token) throws Exception {
        String body = mockMvc.perform(post("/api/location-inventory-sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationId\":\"" + locationId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String createLocation(String code, String name) throws Exception {
        String body = mockMvc.perform(post("/api/locations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LocationRequest(zone.getId(), code, name, null, null, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    /** Cree une immobilisation rattachee au local donne et retourne son assetCode genere. */
    private String createAssetAt(String locationId) throws Exception {
        AssetCreateRequest request = new AssetCreateRequest(
                "Materiel session test " + UUID.randomUUID(), category.getId(), null, null, "SN-" + UUID.randomUUID(),
                site.getId(), building.getId(), floor.getId(), zone.getId(), UUID.fromString(locationId),
                null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        String body = mockMvc.perform(post("/api/assets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("assetCode").asText();
    }

    private Site site() {
        Site site = new Site();
        site.setCode("SSN");
        site.setName("Siege Session Test");
        return site;
    }

    private Building building(Site site) {
        Building building = new Building();
        building.setSite(site);
        building.setCode("BAT-SN");
        building.setName("Batiment Session Test");
        return building;
    }

    private Floor floor(Building building) {
        Floor floor = new Floor();
        floor.setBuilding(building);
        floor.setCode("ET-SN");
        floor.setName("Etage Session Test");
        return floor;
    }

    private Zone zone(Floor floor) {
        Zone zone = new Zone();
        zone.setFloor(floor);
        zone.setCode("ZN-SN");
        zone.setName("Zone Session Test");
        return zone;
    }

    private User newUser(String email, Role role) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    private String login(User user) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user.getEmail(), PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
