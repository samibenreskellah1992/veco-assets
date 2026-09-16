package dz.vecopharm.vecoassets;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for every test that needs a real database: starts one
 * PostgreSQL 16 container (matching production, matching docker-compose.yml)
 * shared for the whole test class, and points Spring's datasource at it.
 * Flyway then runs the real migrations against it on context startup, so
 * every subclass also doubles as a "do the migrations apply cleanly" check.
 *
 * Requires a Docker daemon reachable by Testcontainers - not available in
 * the sandbox this project was scaffolded in (see docs/ROADMAP.md section
 * 13); runs normally in any environment with Docker (dev machine, CI).
 */
@Tag("integration")
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
