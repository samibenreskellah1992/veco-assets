package dz.vecopharm.vecoassets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Phase 1 smoke test: the Spring context must load (config, security filter
 * chain, JPA/Flyway wiring) against the test datasource.
 */
@SpringBootTest
@ActiveProfiles("test")
class VecoAssetsApplicationTests {

    @Test
    void contextLoads() {
    }
}
