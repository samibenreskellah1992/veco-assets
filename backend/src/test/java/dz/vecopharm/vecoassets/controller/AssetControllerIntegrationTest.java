package dz.vecopharm.vecoassets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dz.vecopharm.vecoassets.AbstractIntegrationTest;
import dz.vecopharm.vecoassets.dto.AssetCreateRequest;
import dz.vecopharm.vecoassets.dto.AssetUpdateRequest;
import dz.vecopharm.vecoassets.dto.LoginRequest;
import dz.vecopharm.vecoassets.entity.AssetCategory;
import dz.vecopharm.vecoassets.entity.AssetCondition;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import dz.vecopharm.vecoassets.entity.Role;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.repository.AssetCategoryRepository;
import dz.vecopharm.vecoassets.repository.RoleRepository;
import dz.vecopharm.vecoassets.repository.SiteRepository;
import dz.vecopharm.vecoassets.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout-en-bout (Testcontainers, prompt maitre Phase 10 - taches #87) de
 * {@code /api/assets} : creation (code genere automatiquement via la vraie
 * sequence PostgreSQL, jamais fourni par le client), et la matrice de
 * permissions {@code IMMOBILISATION_VIEW/CREATE/EDIT/ARCHIVE} verifiee
 * cote backend (jamais seulement au niveau frontend - prompt maitre section
 * 28), avec les roles et permissions reels du seed Phase 2
 * (V3__roles_permissions_users.sql), meme discipline que
 * {@code AuthenticationIntegrationTest} (Phase 3).
 *
 * Necessite un demon Docker (Testcontainers) - non disponible dans le
 * sandbox ou ce projet a ete developpe (voir docs/ROADMAP.md section 13),
 * s'execute normalement sur un poste de developpement ou en CI.
 */
@AutoConfigureMockMvc
@Transactional
class AssetControllerIntegrationTest extends AbstractIntegrationTest {

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
    private AssetCategoryRepository categoryRepository;

    private Site site;
    private AssetCategory category;
    private String gestionnaireToken;
    private String consultationToken;

    @BeforeEach
    void setUp() throws Exception {
        site = new Site();
        site.setCode("SGE");
        site.setName("Siege");
        site = siteRepository.save(site);

        category = new AssetCategory();
        category.setCode("INFO");
        category.setName("Informatique");
        category = categoryRepository.save(category);

        Role gestionnaireRole = roleRepository.findByCode("GESTIONNAIRE_PATRIMOINE").orElseThrow();
        Role consultationRole = roleRepository.findByCode("CONSULTATION").orElseThrow();

        User gestionnaire = newUser("gestionnaire.test@vecopharm.dz", gestionnaireRole);
        User consultation = newUser("consultation.test@vecopharm.dz", consultationRole);

        gestionnaireToken = login(gestionnaire.getEmail());
        consultationToken = login(consultation.getEmail());
    }

    @Test
    void createWithGestionnairePermissionGeneratesCodeAndDefaultsToNeufEnStock() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetCode").isNotEmpty())
                .andExpect(jsonPath("$.condition").value("NEUF"))
                .andExpect(jsonPath("$.status").value("EN_STOCK"));
    }

    @Test
    void createWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithConsultationPermissionIsForbidden() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void consultationCanListAndReadButNotWrite() throws Exception {
        String id = createAsset();

        mockMvc.perform(get("/api/assets").header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/assets/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/assets/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/assets/" + id + "/archive").header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void gestionnaireCanUpdateAndArchiveButNotArchiveTwice() throws Exception {
        String id = createAsset();

        mockMvc.perform(put("/api/assets/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_MAINTENANCE"));

        mockMvc.perform(post("/api/assets/" + id + "/archive").header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk());

        // Immobilisation deja archivee : suppression toujours logique, jamais
        // physique (prompt maitre section 26), donc une deuxieme archive doit
        // etre rejetee comme regle metier plutot que de reussir silencieusement.
        mockMvc.perform(post("/api/assets/" + id + "/archive").header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }

    // --- Helpers -------------------------------------------------

    private String createAsset() throws Exception {
        String body = mockMvc.perform(post("/api/assets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private AssetCreateRequest createRequest() {
        return new AssetCreateRequest(
                "Ordinateur portable", category.getId(), "Dell", "Latitude 5540", "SN-" + java.util.UUID.randomUUID(),
                site.getId(), null, null, null, null,
                "Direction IT", "Support", "Helpdesk", null, null,
                null, null, null, null, null, null, null, null);
    }

    private AssetUpdateRequest updateRequest() {
        return new AssetUpdateRequest(
                "Ordinateur portable", category.getId(), "Dell", "Latitude 5540", "SN-" + java.util.UUID.randomUUID(),
                site.getId(), null, null, null, null,
                "Direction IT", "Support", "Helpdesk", null, null,
                null, null, null, null, null, null,
                AssetCondition.BON, AssetStatus.EN_MAINTENANCE, null, "Passage en maintenance (test)");
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
