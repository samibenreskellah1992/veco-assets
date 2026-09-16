package dz.vecopharm.vecoassets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dz.vecopharm.vecoassets.AbstractIntegrationTest;
import dz.vecopharm.vecoassets.dto.LoginRequest;
import dz.vecopharm.vecoassets.entity.Role;
import dz.vecopharm.vecoassets.entity.User;
import dz.vecopharm.vecoassets.repository.RoleRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout-en-bout (Testcontainers, Phase 10 - tache #87) des permissions du
 * tableau de bord et des rapports (prompt maitre Phase 9) : {@code
 * REPORT_VIEW} (consultation) et {@code REPORT_EXPORT} (export CSV/Excel/
 * PDF, susceptible de sortir de l'application) sont deux permissions
 * distinctes depuis le seed Phase 2 - RESPONSABLE_SITE/RESPONSABLE_SERVICE/
 * CONSULTATION n'ont que la premiere (voir V3__roles_permissions_users.sql
 * et la Javadoc de {@code ReportController}).
 */
@AutoConfigureMockMvc
@Transactional
class ReportingPermissionsIntegrationTest extends AbstractIntegrationTest {

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

    private String consultationToken;
    private String gestionnaireToken;
    private String inventoristeToken;

    @BeforeEach
    void setUp() throws Exception {
        Role consultationRole = roleRepository.findByCode("CONSULTATION").orElseThrow();
        Role gestionnaireRole = roleRepository.findByCode("GESTIONNAIRE_PATRIMOINE").orElseThrow();
        Role inventoristeRole = roleRepository.findByCode("INVENTORISTE").orElseThrow();

        consultationToken = login(newUser("consultation.rpt@vecopharm.dz", consultationRole).getEmail());
        gestionnaireToken = login(newUser("gestionnaire.rpt@vecopharm.dz", gestionnaireRole).getEmail());
        // INVENTORISTE (scan/inventaire uniquement) ne possede pas REPORT_VIEW.
        inventoristeToken = login(newUser("inventoriste.rpt@vecopharm.dz", inventoristeRole).getEmail());
    }

    @Test
    void dashboard_requiresReportView() throws Exception {
        mockMvc.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, "Bearer " + inventoristeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reportView_allowedForConsultation_butExportIsNot() throws Exception {
        mockMvc.perform(get("/api/reports/PAR_SITE").header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/PAR_SITE/export?format=CSV").header(HttpHeaders.AUTHORIZATION, "Bearer " + consultationToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void reportExport_allowedForGestionnaire() throws Exception {
        // L'export renvoie des octets bruts (CSV/Excel/PDF), pas du JSON - on
        // verifie le statut et l'en-tete de telechargement, pas le corps.
        mockMvc.perform(get("/api/reports/PAR_SITE/export?format=CSV").header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaireToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("rapport-par_site")));
    }

    // --- Helpers -------------------------------------------------

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
