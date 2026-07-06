package demo.repo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class MockRepoSeedDataPostgresRestartTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    @DisplayName("PostgreSQL seed data restart does not fail after a seeded DPP was soft-deleted")
    void postgresSeedDataSurvivesRestartAfterSeededDelete() {
        startPostgresIfNeeded();

        try (ConfigurableApplicationContext firstContext = startApplication()) {
            DppRepoService firstService = firstContext.getBean(DppRepoService.class);
            assertTrue(firstService.hasActiveDpp(PostmanSeedData.DELETE_EXAMPLE_DPP_ID));

            firstService.deleteById(PostmanSeedData.DELETE_EXAMPLE_DPP_ID);

            assertFalse(firstService.hasActiveDpp(PostmanSeedData.DELETE_EXAMPLE_DPP_ID));
        }

        try (ConfigurableApplicationContext secondContext = assertDoesNotThrow(this::startApplication)) {
            DppRepoService secondService = secondContext.getBean(DppRepoService.class);
            assertFalse(secondService.hasActiveDpp(PostmanSeedData.DELETE_EXAMPLE_DPP_ID));
            assertTrue(secondService.hasActiveDpp(PostmanSeedData.DPP_ID));
        }
    }

    private static void startPostgresIfNeeded() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }

    private ConfigurableApplicationContext startApplication() {
        return new SpringApplicationBuilder(MockRepoApplication.class)
                .properties(
                        "debug=false",
                        "logging.level.root=WARN",
                        "logging.level.org.springframework=WARN",
                        "server.port=0",
                        "dpp.repo.backend=postgres",
                        "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "spring.datasource.username=" + POSTGRES.getUsername(),
                        "spring.datasource.password=" + POSTGRES.getPassword()
                )
                .run();
    }
}
