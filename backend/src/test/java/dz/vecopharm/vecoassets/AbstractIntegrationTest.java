package dz.vecopharm.vecoassets;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for every test that needs a real database: starts one
 * PostgreSQL 16 container (matching production, matching docker-compose.yml)
 * and points Spring's datasource at it. Flyway then runs the real migrations
 * against it on context startup, so every subclass also doubles as a "do the
 * migrations apply cleanly" check.
 *
 * Singleton container pattern (deliberate, see below) - the container is
 * started ONCE via a static initializer and never stopped explicitly; the
 * Testcontainers Ryuk reaper cleans it up when the whole JVM/test run exits.
 *
 * Why: this base class is extended by several *different* concrete test
 * classes (AssetControllerIntegrationTest, MovementControllerIntegrationTest,
 * ReportingPermissionsIntegrationTest, AuthenticationIntegrationTest,
 * VecoAssetsApplicationTests). Because none of them add any distinguishing
 * @SpringBootTest/@ActiveProfiles config, Spring's TestContext framework
 * treats them all as the SAME cache key and reuses a single ApplicationContext
 * (and its HikariCP pool) across all of them - it does NOT re-invoke
 * @DynamicPropertySource per class. Previously the container was declared
 * with @Container + @Testcontainers, which (for a static field) starts the
 * container in beforeAll and STOPS it in afterAll of each concrete class.
 * The first class to run got a live container and a working pool; every
 * subsequent class's beforeAll then spun up a brand-new container on a new
 * port (visible in its own logs), but the *reused* cached Spring context kept
 * its HikariPool wired to the first (now-dead) container's port, so every
 * later class failed with
 * "HikariPool-1 - Connection is not available, request timed out" /
 * "Connection to localhost:<old-port> refused".
 *
 * Starting the container exactly once for the whole test run (and never
 * stopping/restarting it between classes) means the port never changes, so
 * the reused Spring context's datasource always points at a live container -
 * whether Spring caches the context (fast path) or not.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
