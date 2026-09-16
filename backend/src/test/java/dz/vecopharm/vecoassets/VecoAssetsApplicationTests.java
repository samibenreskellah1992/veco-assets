package dz.vecopharm.vecoassets;

import org.junit.jupiter.api.Test;

/**
 * Phase 1 smoke test, now against a real container (Phase 2+): the Spring
 * context must load (config, security filter chain, JPA/Flyway wiring)
 * with every real migration applied.
 */
class VecoAssetsApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
