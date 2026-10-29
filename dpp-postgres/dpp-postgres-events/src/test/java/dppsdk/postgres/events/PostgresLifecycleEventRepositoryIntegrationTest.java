package dppsdk.postgres.events;

import static org.junit.jupiter.api.Assertions.*;
import dppsdk.postgres.core.PostgresDppRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class PostgresLifecycleEventRepositoryIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test void roundTripsAuditDataAndRevisionCorrelation() throws Exception {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl()); dataSource.setUser(POSTGRES.getUsername()); dataSource.setPassword(POSTGRES.getPassword());
        new PostgresDppRepository(dataSource);
        PostgresLifecycleEventRepository repository = new PostgresLifecycleEventRepository();
        Instant at = Instant.parse("2026-04-01T10:00:00Z").plusNanos(123_456_789);
        Instant storedAt = at.truncatedTo(ChronoUnit.MICROS);
        try (var connection = dataSource.getConnection()) {
            repository.initializeSchema(connection);
            connection.setAutoCommit(false);
            repository.append(connection, "urn:test:event", 4L, "op-4", DppLifecycleEventType.DPP_UPDATED, at, Map.of("path", "a.b"));
            connection.commit();
            var result = repository.findByDppId(connection, "urn:test:event").get(0);
            assertEquals(Map.of("path", "a.b"), result.data());
            assertEquals(4L, result.versionNo());
            assertEquals("op-4", result.operationId());
            assertEquals(storedAt, result.occurredAt());
        }
    }
}
