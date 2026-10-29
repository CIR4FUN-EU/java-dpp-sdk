package dppsdk.postgres.events;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.postgres.core.PostgresDppOperationContext;
import dppsdk.postgres.core.PostgresDppRepository;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class PostgresDppRepositoryWithEventsIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void canonicalRevisionAndEventsShareLifecycleAndNoOpWritesDoNotCreateEvents() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        PostgresDppRepositoryWithEvents repository = new PostgresDppRepositoryWithEvents(dataSource);
        repository.initializeSchema();
        PostgresDppRepository core = new PostgresDppRepository(dataSource);
        core.initializeRepositoryProfile("core");

        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = t1.plusSeconds(60);
        DigitalProductPassport first = passport("urn:test:events-core", "one");
        repository.create(first, new PostgresDppOperationContext("create", t1));
        DigitalProductPassport second = passport("urn:test:events-core", "two");
        repository.appendVersion(second, 1, new PostgresDppOperationContext("update", t2),
                DppLifecycleEventType.DPP_UPDATED, Map.of("productId", "urn:test:event-product"));
        repository.appendVersion(second, 2, new PostgresDppOperationContext("noop", t2.plusSeconds(1)),
                DppLifecycleEventType.DPP_UPDATED, Map.of());

        assertEquals(second, core.findCurrentByDppId("urn:test:events-core").orElseThrow());
        assertEquals(List.of(1L, 2L), core.findHistoryByDppId("urn:test:events-core")
                .stream().map(row -> row.versionNo()).toList());
        assertEquals(List.of(DppLifecycleEventType.DPP_CREATED, DppLifecycleEventType.DPP_UPDATED),
                repository.findEventsByDppId("urn:test:events-core").stream().map(DppLifecycleEventRecord::eventType).toList());
    }

    private DigitalProductPassport passport(String id, String value) {
        DppHeader header = new DppHeader.Builder().digitalProductPassportId(id)
                .uniqueProductIdentifier("urn:test:event-product").granularity(Granularity.MODEL)
                .dppSchemaVersion("1").dppStatus("active").lastUpdated(Instant.parse("2026-01-01T00:00:00Z"))
                .economicOperatorId("operator").build();
        return new DigitalProductPassport.Builder().dppHeader(header).dataElements(List.of(
                new SingleValuedDataElement.Builder().elementId("generic").value(value).build())).build();
    }
}
