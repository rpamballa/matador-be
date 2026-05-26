package com.matador.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for full-context integration tests. Boots the application against a real
 * PostGIS-enabled Postgres via Testcontainers; Flyway applies all migrations.
 *
 * <p>Uses the singleton-container pattern: the container is started once in a static
 * initializer and kept running for the whole JVM. This keeps the Spring context cache
 * valid across test classes (a per-class container would be torn down and leave cached
 * contexts pointing at a dead port).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    static {
        POSTGRES.start();
    }
}
