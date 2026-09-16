package dz.vecopharm.vecoassets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dz.vecopharm.vecoassets.AbstractIntegrationTest;
import dz.vecopharm.vecoassets.dto.LoginRequest;
import dz.vecopharm.vecoassets.dto.MovementCreateRequest;
import dz.vecopharm.vecoassets.entity.Asset;
import dz.vecopharm.vecoassets.entity.AssetCategory;
import dz.vecopharm.vecoassets.entity.AssetStatus;
import dz.vecopharm.vecoassets.entity.MovementType;
import dz.vecopharm.vecoassets.entity.Role;
import dz.vecopharm.vecoassets.entity.Site;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.repository.AssetCategoryRepository;
import dz.vecopharm.vecoassets.repository.AssetRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout-en-bout (Testcontainers, Phase 10 - tache #87) du workflow Demande
 * -> Validation -> Execution de {@code /api/movements} (prompt maitre
 * Phase 8 / section 18) : l'effet du mouvement (ici MAINTENANCE, qui ne
 * requiert aucun champ cible) ne doit apparaitre sur l'immobilisation
 * qu'a l'execution, jamais avant - et {@code MOUVEMENT_VALIDATE} est
 * reserve a GESTIONNAIRE_PATRIMOINE/ADMIN (V3), RESPONSABLE_SITE ne peut
 * que demander (voir la Javadoc de {@code MovementController}).
 */
@AutoConfigureMockMvc
@Transactional
class MovementControllerIntegrationTest extends AbstractIntegrationTest {

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
    @Autowired
    private AssetRepository assetRepository;

    private Asset asset;
    private String gestionnaireToken;
    private String responsableSiteToken;

    @BeforeEach
    void setUp() throws Exception {
        Site site = new Site();
        site.setCode("SGE");
        site.setName("Siege");
        site = siteRepository.save(site);

        AssetCategory category = new AssetCategory();
        category.setCode("INFO");
        category.setName("Informatique");
        category = categoryRepository.save(category);

        asset = new Asset();
        asset.setAssetCode("VECO-IMM-TEST-" + System.nanoTime());
        asset.setDesignation("Imprimante");
        asset.setCategory(category);
        asset.setSite(site);
        asset.setStatus(AssetStatus.EN_STOCK);
        asset = assetRepository.save(asset);

        Role gestionnaireRole = roleRepository.findByCode("GESTIONNAIRE_PATRIMOINE").orElseThrow();
        Role responsableSiteRole = roleRepository.findByCode("RESPONSABLE_SITE").orElseThrow();

        gestionnaireToken = login(newUser("gestionnaire.mvt@vecopharm.dz", gestionnaireRole).getEmail());
        responsableSiteToken = login(newUser("responsable.mvt@vecopharm.dz", responsableSiteRole).getEmail());
    }

    @Test
    void fullWorkflow_effectOnlyAppliesAtExecution_neverAtRequestOrValidation() throws Exception {
        String movementId = requestMaintenance(gestionnaireToken);

        assertAssetStatus(AssetStatus.EN_STOCK);
        mockMvc.perform(get("/api/movements/" + movementId).header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEMANDE"));

        mockMvc.perform(post("/api/movements/" + movementId + "/validate").header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDE"));
        assertAssetStatus(AssetStatus.EN_STOCK);

        mockMvc.perform(post("/api/movements/" + movementId + "/execute").header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTE"));
        assertAssetStatus(AssetStatus.EN_MAINTENANCE);
    }

    @Test
    void executingBeforeValidationIsRejected() throws Exception {
        String movementId = requestMaintenance(gestionnaireToken);

        mockMvc.perform(post("/api/movements/" + movementId + "/execute").header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void responsableSiteCanRequestButCannotValidateOrExecute() throws Exception {
        String movementId = requestMaintenance(responsableSiteToken);

        mockMvc.perform(post("/api/movements/" + movementId + "/validate").header(HttpHeaders.AUTHORIZATION, "Bearer " + responsableSiteToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/movements/" + movementId + "/execute").header(HttpHeaders.AUTHORIZATION, "Bearer " + responsableSiteToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectedMovementCanNeverBeExecuted() throws Exception {
        String movementId = requestMaintenance(gestionnaireToken);

        mockMvc.perform(post("/api/movements/" + movementId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Plus necessaire\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJETE"));

        mockMvc.perform(post("/api/movements/" + movementId + "/execute").header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isUnprocessableEntity());
        assertAssetStatus(AssetStatus.EN_STOCK);
    }

    // --- Helpers -------------------------------------------------

    private String requestMaintenance(String token) throws Exception {
        MovementCreateRequest request = new MovementCreateRequest(
                asset.getId(), MovementType.MAINTENANCE, null, null, null, null, null, null, "Revision periodique", null);
        String body = mockMvc.perform(post("/api/movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DEMANDE"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private void assertAssetStatus(AssetStatus expected) throws Exception {
        mockMvc.perform(get("/api/assets/" + asset.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expected.name()));
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
