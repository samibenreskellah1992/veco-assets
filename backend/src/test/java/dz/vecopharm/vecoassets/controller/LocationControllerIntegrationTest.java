package dz.vecopharm.vecoassets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dz.vecopharm.vecoassets.AbstractIntegrationTest;
import dz.vecopharm.vecoassets.dto.AssetCreateRequest;
import dz.vecopharm.vecoassets.dto.LocationRequest;
import dz.vecopharm.vecoassets.dto.LoginRequest;
import dz.vecopharm.vecoassets.entity.AssetCategory;
import dz.vecopharm.vecoassets.entity.Building;
import dz.vecopharm.vecoassets.entity.Floor;
import dz.vecopharm.vecoassets.entity.Role;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout-en-bout (Testcontainers) de {@code /api/locations} - Checkpoint 1
 * "locaux scannables" (2026-09), meme discipline que
 * {@link AssetControllerIntegrationTest} : matrice de permissions
 * {@code REFERENTIEL_MANAGE} verifiee cote backend, ET surtout une
 * regression au niveau HTTP du bug de collision de qr_code trouve en
 * verification live du Checkpoint 1 (deux locaux crees successivement sur
 * le meme site doivent recevoir des codes QR distincts et sequentiels -
 * voir aussi {@link dz.vecopharm.vecoassets.service.LocationCodeGeneratorIntegrationTest}
 * pour la version qui isole LocationCodeGenerator lui-meme).
 */
@AutoConfigureMockMvc
@Transactional
class LocationControllerIntegrationTest extends AbstractIntegrationTest {

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
    private String consultationToken;

    @BeforeEach
    void setUp() throws Exception {
        site = siteRepository.save(site());
        building = buildingRepository.save(building(site));
        floor = floorRepository.save(floor(building));
        zone = zoneRepository.save(zone(floor));

        category = new AssetCategory();
        category.setCode("INFO");
        category.setName("Informatique");
        category = categoryRepository.save(category);

        Role gestionnaireRole = roleRepository.findByCode("GESTIONNAIRE_PATRIMOINE").orElseThrow();
        Role consultationRole = roleRepository.findByCode("CONSULTATION").orElseThrow();

        User gestionnaire = newUser("gestionnaire.locaux.test@vecopharm.dz", gestionnaireRole);
        User consultation = newUser("consultation.locaux.test@vecopharm.dz", consultationRole);

        gestionnaireToken = login(gestionnaire.getEmail());
        consultationToken = login(consultation.getEmail());
    }

    @Test
    void createWithReferentielManagePermission_generatesSiteScopedQrCodeAndDefaults() throws Exception {
        mockMvc.perform(post("/api/locations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRequest("BUR-1", "Bureau 1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qrCode").value("LOC-" + site.getCode() + "-000001"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.status").value("ACTIF"))
                .andExpect(jsonPath("$.labeled").value(false))
                .andExpect(jsonPath("$.assetCount").value(0));
    }

    @Test
    void createWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRequest("BUR-1", "Bureau 1"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithConsultationPermissionIsForbidden() throws Exception {
        mockMvc.perform(post("/api/locations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRequest("BUR-1", "Bureau 1"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void consultationCanListAndReadButNotWrite() throws Exception {
        String id = createLocation("BUR-1", "Bureau 1");

        mockMvc.perform(get("/api/locations").header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/locations/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/locations/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRequest("BUR-1-BIS", "Bureau 1 renomme"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/locations/" + id + "/deactivate").header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/locations/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_twoLocationsOnTheSameSite_getDistinctSequentialQrCodes() throws Exception {
        // Regression HTTP directe du bug de collision (voir javadoc de la
        // classe) : deux creations successives sur le MEME site ne doivent
        // jamais retomber sur le meme qr_code.
        String firstId = createLocation("BUR-1", "Bureau 1");
        String secondId = createLocation("BUR-2", "Bureau 2");

        mockMvc.perform(get("/api/locations/" + firstId).header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCode").value("LOC-" + site.getCode() + "-000001"));
        mockMvc.perform(get("/api/locations/" + secondId).header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCode").value("LOC-" + site.getCode() + "-000002"));
    }

    @Test
    void findByCode_returnsTheMatchingLocation_openToAnyAuthenticatedUser() throws Exception {
        String id = createLocation("BUR-1", "Bureau 1");
        String qrCode = "LOC-" + site.getCode() + "-000001";

        mockMvc.perform(get("/api/locations/by-code/" + qrCode).header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void update_neverChangesTheGeneratedQrCode() throws Exception {
        String id = createLocation("BUR-1", "Bureau 1");
        String originalQrCode = "LOC-" + site.getCode() + "-000001";

        mockMvc.perform(put("/api/locations/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRequest("BUR-1-BIS", "Bureau renomme"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bureau renomme"))
                .andExpect(jsonPath("$.qrCode").value(originalQrCode));
    }

    @Test
    void delete_isBlockedWhenAssetsAreAttached() throws Exception {
        String locationId = createLocation("BUR-1", "Bureau 1");

        mockMvc.perform(post("/api/assets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assetAttachedTo(locationId))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/locations/" + locationId).header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }

    // --- Helpers -------------------------------------------------

    private String createLocation(String code, String name) throws Exception {
        String body = mockMvc.perform(post("/api/locations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRequest(code, name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private LocationRequest locationRequest(String code, String name) {
        return new LocationRequest(zone.getId(), code, name, null, null, null);
    }

    private AssetCreateRequest assetAttachedTo(String locationId) {
        return new AssetCreateRequest(
                "Materiel rattache au local test", category.getId(), null, null, "SN-" + UUID.randomUUID(),
                site.getId(), building.getId(), floor.getId(), zone.getId(), UUID.fromString(locationId),
                null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }

    private Site site() {
        Site site = new Site();
        site.setCode("SGL");
        site.setName("Siege Locaux Test");
        return site;
    }

    private Building building(Site site) {
        Building building = new Building();
        building.setSite(site);
        building.setCode("BAT-L");
        building.setName("Batiment Locaux Test");
        return building;
    }

    private Floor floor(Building building) {
        Floor floor = new Floor();
        floor.setBuilding(building);
        floor.setCode("ET-L");
        floor.setName("Etage Locaux Test");
        return floor;
    }

    private Zone zone(Floor floor) {
        Zone zone = new Zone();
        zone.setFloor(floor);
        zone.setCode("ZN-L");
        zone.setName("Zone Locaux Test");
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

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
