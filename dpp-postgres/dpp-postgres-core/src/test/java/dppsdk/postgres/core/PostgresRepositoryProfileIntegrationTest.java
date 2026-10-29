package dppsdk.postgres.core;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.model.SingleValuedDataElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class PostgresRepositoryProfileIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeEach
    void clearDataset() throws Exception {
        PostgresDppRepository repository = new PostgresDppRepository(dataSource());
        try (Connection connection = dataSource().getConnection(); var statement = connection.createStatement()) {
            statement.execute("truncate table dpp_versions, dpp_passports restart identity cascade");
            statement.executeUpdate("delete from dpp_repository_profile");
        }
    }

    @Test
    void emptyDatasetBindsProfileAndMarkerSurvivesProjectionAndEventTableRemoval() throws Exception {
        DataSource dataSource = dataSource();
        PostgresDppRepository repository = new PostgresDppRepository(dataSource);
        repository.initializeRepositoryProfile("core");
        repository.initializeRepositoryProfile("core");
        assertThrows(IllegalStateException.class, () -> repository.initializeRepositoryProfile("dpp4fun"));

        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("create table dpp_projections (id int)");
            statement.execute("create table dpp_lifecycle_events (id int)");
            statement.execute("drop table dpp_projections");
            statement.execute("drop table dpp_lifecycle_events");
            try (var result = statement.executeQuery(
                    "select repository_profile from dpp_repository_profile where singleton = true")) {
                result.next();
                assertEquals("core", result.getString(1));
            }
        }
    }

    @Test
    void unmarkedPopulatedLegacyDatasetFailsWithoutInstallingAGuessedProfile() throws Exception {
        DataSource dataSource = dataSource();
        PostgresDppRepository repository = new PostgresDppRepository(dataSource);
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        repository.create(passport(), new PostgresDppOperationContext("legacy", createdAt));
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> repository.initializeRepositoryProfile("core"));
        assertEquals(true, failure.getMessage().contains("legacy repository migration"));
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("select count(*) from dpp_repository_profile")) {
            result.next();
            assertEquals(0, result.getInt(1));
        }
    }

    private DataSource dataSource() {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        return source;
    }

    private DigitalProductPassport passport() {
        DppHeader header = new DppHeader.Builder().digitalProductPassportId("urn:test:legacy-profile")
                .uniqueProductIdentifier("urn:test:legacy-product").granularity(Granularity.MODEL)
                .dppSchemaVersion("1").dppStatus("active").lastUpdated(Instant.parse("2026-01-01T00:00:00Z"))
                .economicOperatorId("operator").build();
        return new DigitalProductPassport.Builder().dppHeader(header).dataElements(List.of(
                new SingleValuedDataElement.Builder().elementId("generic").value("value").build())).build();
    }
}
