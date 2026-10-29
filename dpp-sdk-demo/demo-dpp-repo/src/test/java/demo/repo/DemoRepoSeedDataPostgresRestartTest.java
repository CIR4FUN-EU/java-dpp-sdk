package demo.repo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class DemoRepoSeedDataPostgresRestartTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    @DisplayName("PostgreSQL seed data restart does not fail after a seeded DPP was soft-deleted")
    void postgresSeedDataSurvivesRestartAfterSeededDelete() {
        startPostgresIfNeeded();

        try (ConfigurableApplicationContext firstContext = startApplication()) {
            DppRepoService firstService = firstContext.getBean(DppRepoService.class);
            assertTrue(firstService.readAllActiveDppIds().contains(PostmanSeedData.DELETE_EXAMPLE_DPP_ID));

            firstService.deleteById(PostmanSeedData.DELETE_EXAMPLE_DPP_ID);

            assertFalse(firstService.readAllActiveDppIds().contains(PostmanSeedData.DELETE_EXAMPLE_DPP_ID));
        }

        try (ConfigurableApplicationContext secondContext = assertDoesNotThrow(() -> startApplication())) {
            DppRepoService secondService = secondContext.getBean(DppRepoService.class);
            assertFalse(secondService.readAllActiveDppIds().contains(PostmanSeedData.DELETE_EXAMPLE_DPP_ID));
            assertTrue(secondService.readAllActiveDppIds().contains(PostmanSeedData.DPP_ID));
        }
    }

    @Test
    @DisplayName("A profile mismatch fails application startup before core-profile seeds or writes")
    void mismatchedProfileFailsStartupBeforeSeeding() throws Exception {
        startPostgresIfNeeded();
        try (ConfigurableApplicationContext context = startApplication("dpp4fun")) {
            assertTrue(context.getBean(DppRepoService.class).readAllActiveDppIds().contains(PostmanSeedData.DPP_ID));
        }

        long before = passportCount();
        RuntimeException failure = assertThrows(RuntimeException.class, () -> startApplication("core"));
        assertTrue(causeMessages(failure).contains("was initialized for profile 'dpp4fun'"),
                causeMessages(failure));
        assertEquals(before, passportCount());
    }

    private static void startPostgresIfNeeded() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }

    private ConfigurableApplicationContext startApplication() {
        return startApplication("dpp4fun");
    }

    private ConfigurableApplicationContext startApplication(String profile) {
        return new SpringApplicationBuilder(DemoRepoApplication.class)
                .properties(
                        "debug=false",
                        "logging.level.root=WARN",
                        "logging.level.org.springframework=WARN",
                        "server.port=0",
                        "dpp.repo.backend=postgres",
                        "dpp.repo.profile=" + profile,
                        "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "spring.datasource.username=" + POSTGRES.getUsername(),
                        "spring.datasource.password=" + POSTGRES.getPassword()
                )
                .run();
    }

    private long passportCount() throws Exception {
        try (var connection = dataSource().getConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("select count(*) from dpp_passports")) {
            result.next();
            return result.getLong(1);
        }
    }

    private DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }

    private String causeMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            messages.append(cause.getMessage()).append('\n');
        }
        return messages.toString();
    }
}
