package dppsdk.postgres.dpp4fun;

import static org.junit.jupiter.api.Assertions.*;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.dpp4fun.model.aspect.BillOfMaterials;
import dppsdk.dpp4fun.model.composite.Component;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.composite.Material;
import dppsdk.dpp4fun.model.composite.Part;
import dppsdk.postgres.core.DppPage;
import dppsdk.postgres.core.DppPageRequest;
import dppsdk.postgres.core.PostgresDppOperationContext;
import dppsdk.postgres.core.PostgresDppStatus;
import dppsdk.postgres.core.PostgresDppVersionConflictException;
import dppsdk.postgres.events.DppLifecycleEventRecord;
import dppsdk.postgres.events.DppLifecycleEventType;
import dppsdk.postgres.core.PostgresDppRepository;
import dppsdk.postgres.projection.DppProjectionDefinition;
import dppsdk.postgres.projection.DppProjectionDocument;
import dppsdk.postgres.projection.DppProjectionValueType;
import dppsdk.postgres.projection.PostgresDppProjectionRepository;
import dppsdk.postgres.projection.PostgresProjectionSchemaManager;
import dppsdk.support.TestDataFactory;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Dpp4FunPostgresRepositoryIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test void canonicalSnapshotsRoundTripAndAppendWithHistoryAndStaleVersionCheck() {
        Dpp4FunPostgresRepository repository = repository();
        Dpp4Fun v1 = dpp("history", "North", "Bed", "Steel", "Frame", "Slat");
        Instant t1 = Instant.parse("2026-02-01T09:00:00Z");
        Instant t2 = t1.plusSeconds(600);
        repository.create(v1, new PostgresDppOperationContext("create-history", t1));
        var currentSnapshot = repository.findCurrentPassportSnapshotByDppId(v1.getDigitalProductPassportId()).orElseThrow();
        assertEquals(v1.asDigitalProductPassport(), currentSnapshot.passport());
        assertEquals(1L, currentSnapshot.versionNo());
        Dpp4Fun v2 = v1.toBuilder().nameplate(v1.getNameplate().toBuilder().brand("South").build()).build();
        repository.appendVersion(v2, 1L, new PostgresDppOperationContext("append-history", t2));
        assertEquals(v1, repository.findByDppIdAt(v1.getDigitalProductPassportId(), t2.minusNanos(1_000)).orElseThrow());
        assertEquals(v2, repository.findByDppIdAt(v1.getDigitalProductPassportId(), t2).orElseThrow());
        assertEquals(v2, repository.findCurrentByDppId(v1.getDigitalProductPassportId()).orElseThrow());
        assertEquals(v2, repository.findCurrentByProductId(v1.getUniqueProductIdentifier()).orElseThrow());
        assertThrows(PostgresDppVersionConflictException.class, () -> repository.appendVersion(v2, 1L,
                new PostgresDppOperationContext("stale", t2.plusSeconds(10))));
        var history = repository.findHistoryByDppId(v1.getDigitalProductPassportId());
        assertEquals(List.of(1L, 2L), history.stream().map(Dpp4FunVersionSummary::versionNo).toList());
        assertEquals(PostgresDppStatus.SUPERSEDED, history.get(0).status());
        DppLifecycleEventRecord appendEvent = repository.findEventsByDppId(v1.getDigitalProductPassportId()).get(1);
        assertEquals(v1.getDigitalProductPassportId(), appendEvent.dppId());
        assertEquals(2L, appendEvent.versionNo());
        assertEquals("append-history", appendEvent.operationId());
        assertEquals(t2, appendEvent.occurredAt());
    }

    @Test void searchPreservesExactMatchOrderingLimitOffsetAndAllSupportedFields() {
        Dpp4FunPostgresRepository repository = repository();
        Dpp4Fun bed = dpp("bed", "North", "Bed", "Steel", "Frame", "Slat");
        Dpp4Fun chair = dpp("chair", "South", "Chair", "Oak", "Leg", "Seat");
        Dpp4Fun sofa = dpp("sofa", "West", "Sofa", "Wool", "Cushion", "Cover");
        Instant now = Instant.parse("2026-03-01T08:00:00Z");
        repository.create(sofa, new PostgresDppOperationContext("create-sofa", now));
        repository.create(chair, new PostgresDppOperationContext("create-chair", now.plusSeconds(1)));
        repository.create(bed, new PostgresDppOperationContext("create-bed", now.plusSeconds(2)));

        assertEquals(bed.getDigitalProductPassportId(), repository.search(criteria(null, "Steel", null, null, 10, 0)).get(0).dppId());
        assertEquals(chair.getDigitalProductPassportId(), repository.search(criteria("South", null, null, null, 10, 0)).get(0).dppId());
        assertEquals(sofa.getDigitalProductPassportId(), repository.search(criteria(null, null, null, "Cover", 10, 0)).get(0).dppId());
        assertEquals(chair.getDigitalProductPassportId(), repository.search(criteria(null, null, "Leg", null, 10, 0)).get(0).dppId());

        List<Dpp4FunSearchResult> first = repository.search(criteria(null, null, null, null, 2, 0));
        List<Dpp4FunSearchResult> second = repository.search(criteria(null, null, null, null, 2, 2));
        assertEquals(List.of(bed.getDigitalProductPassportId(), chair.getDigitalProductPassportId()), first.stream().map(Dpp4FunSearchResult::dppId).toList());
        assertEquals(List.of(sofa.getDigitalProductPassportId()), second.stream().map(Dpp4FunSearchResult::dppId).toList());
        assertTrue(repository.search(criteria("unknown-brand", null, null, null, 10, 0)).isEmpty());
    }

    @Test void softDeleteRemovesCurrentProjectionButKeepsCanonicalHistoryAndEvents() {
        Dpp4FunPostgresRepository repository = repository();
        Dpp4Fun dpp = dpp("delete", "North", "Bed", "Steel", "Frame", "Slat");
        Instant createdAt = Instant.parse("2026-04-01T08:00:00Z");
        repository.create(dpp, new PostgresDppOperationContext("create-delete", createdAt));
        repository.recordLifecycleEvent(dpp.getDigitalProductPassportId(), DppLifecycleEventType.DATA_ELEMENT_UPDATED,
                createdAt.plusSeconds(1), Map.of("elementPath", "nameplate.manufacturerProductDescription"));
        repository.softDelete(dpp.getDigitalProductPassportId(), 1L,
                new PostgresDppOperationContext("delete-delete", createdAt.plusSeconds(2)));

        assertFalse(repository.existsActiveByDppId(dpp.getDigitalProductPassportId()));
        assertTrue(repository.existsAnyByDppId(dpp.getDigitalProductPassportId()));
        assertTrue(repository.findCurrentByDppId(dpp.getDigitalProductPassportId()).isEmpty());
        assertTrue(repository.search(criteria(null, null, null, null, 20, 0)).stream()
                .noneMatch(row -> row.dppId().equals(dpp.getDigitalProductPassportId())));
        assertEquals(dpp, repository.findByDppIdAt(dpp.getDigitalProductPassportId(), createdAt.plusSeconds(1)).orElseThrow());
        List<DppLifecycleEventRecord> events = repository.findEventsByDppId(dpp.getDigitalProductPassportId());
        assertEquals(List.of(DppLifecycleEventType.DPP_CREATED, DppLifecycleEventType.DATA_ELEMENT_UPDATED, DppLifecycleEventType.DPP_DELETED),
                events.stream().map(DppLifecycleEventRecord::eventType).toList());
        assertEquals("delete-delete", events.get(2).operationId());
        assertEquals(createdAt.plusSeconds(2), events.get(2).occurredAt());
    }

    @Test void rebuildRestoresMissingProjectionFromCanonicalCurrentRevision() throws Exception {
        Dpp4FunPostgresRepository repository = repository();
        Dpp4Fun dpp = dpp("rebuild", "North", "Bed", "Steel", "Frame", "Slat");
        repository.create(dpp, new PostgresDppOperationContext("create-rebuild", Instant.parse("2026-05-01T08:00:00Z")));
        try (Connection connection = dataSource().getConnection(); var statement = connection.prepareStatement("delete from dpp_projections pr using dpp_passports p where pr.passport_id=p.id and p.dpp_id=?")) {
            statement.setString(1, dpp.getDigitalProductPassportId()); statement.executeUpdate();
        }
        assertEquals(1, repository.rebuildProjection());
        assertEquals(1, repository.search(criteria("North", null, null, null, 10, 0)).size());
    }

    @Test void batchLookupUsesActiveProjectedRowsAndStableInputOrdering() {
        Dpp4FunPostgresRepository repository = repository();
        Dpp4Fun one = dpp("batch-one", "North", "Bed", "Steel", "Frame", "Slat");
        Dpp4Fun two = dpp("batch-two", "North", "Bed", "Steel", "Frame", "Slat");
        Instant now = Instant.parse("2026-06-01T08:00:00Z");
        repository.create(one, new PostgresDppOperationContext("batch-one", now));
        repository.create(two, new PostgresDppOperationContext("batch-two", now.plusSeconds(1)));
        DppPage<String> page = repository.findActiveDppIdsByProductIds(
                List.of(two.getUniqueProductIdentifier(), "missing", one.getUniqueProductIdentifier()), new DppPageRequest(null, 1));
        assertEquals(List.of(two.getDigitalProductPassportId()), page.items());
        DppPage<String> next = repository.findActiveDppIdsByProductIds(
                List.of(two.getUniqueProductIdentifier(), "missing", one.getUniqueProductIdentifier()), new DppPageRequest(page.nextCursor(), 1));
        assertEquals(List.of(one.getDigitalProductPassportId()), next.items());
    }

    @Test void sameGenericStoreSupportsDpp4FunAndUnrelatedSyntheticProjectionDefinitions() {
        Dpp4FunPostgresRepository repository = repository();
        Dpp4Fun dpp4fun = dpp("generic-furniture", "North", "Bed", "Steel", "Frame", "Slat");
        Instant now = Instant.parse("2026-07-01T08:00:00Z");
        repository.create(dpp4fun, new PostgresDppOperationContext("create-furniture", now));

        DppHeader syntheticHeader = TestDataFactory.validHeader().toBuilder()
                .digitalProductPassportId("urn:test:dpp:synthetic:" + UUID.randomUUID())
                .uniqueProductIdentifier("urn:test:product:synthetic:" + UUID.randomUUID())
                .economicOperatorId("Example Metals").lastUpdated(now.plusSeconds(1)).build();
        DigitalProductPassport synthetic = new DigitalProductPassport.Builder().dppHeader(syntheticHeader).build();
        PostgresDppRepository core = new PostgresDppRepository(dataSource());
        var version = core.create(synthetic, new PostgresDppOperationContext("create-synthetic", now.plusSeconds(1)));
        DppProjectionDefinition catalog = DppProjectionDefinition.builder("catalog")
                .extract(passport -> DppProjectionDocument.builder()
                        .text("manufacturer", passport.getDppHeader().getEconomicOperatorId())
                        .number("metric", 12.5)
                        .strings("tags", List.of("recycled", "steel"))
                        .build())
                .btree("manufacturer", DppProjectionValueType.TEXT)
                .btree("metric", DppProjectionValueType.NUMBER)
                .gin("tags").build();
        new PostgresProjectionSchemaManager(dataSource()).apply(catalog);
        PostgresDppProjectionRepository generic = new PostgresDppProjectionRepository(dataSource());
        generic.upsertCurrent("catalog", syntheticHeader.getDigitalProductPassportId(), version.versionId(), catalog.project(synthetic));
        assertEquals("Example Metals", generic.find("catalog", syntheticHeader.getDigitalProductPassportId()).orElseThrow().data().get("manufacturer"));
        assertEquals(2, generic.rebuild(catalog));
        assertTrue(generic.find("dpp4fun", dpp4fun.getDigitalProductPassportId()).isPresent());
    }

    @Test void projectionOrEventFailureRollsBackRevisionAndOtherWrites() throws Exception {
        Dpp4FunPostgresRepository repository = repository();
        Dpp4Fun v1 = dpp("rollback", "North", "Bed", "Steel", "Frame", "Slat");
        Instant t1 = Instant.parse("2026-08-01T08:00:00Z");
        repository.create(v1, new PostgresDppOperationContext("create-rollback", t1));
        Dpp4Fun v2 = v1.toBuilder().nameplate(v1.getNameplate().toBuilder().brand("Changed").build()).build();
        try (Connection connection = dataSource().getConnection(); var statement = connection.createStatement()) { statement.execute("drop table dpp_projections"); }
        assertThrows(RuntimeException.class, () -> repository.appendVersion(v2, 1L,
                new PostgresDppOperationContext("projection-failure", t1.plusSeconds(10))));
        assertEquals(1, repository.findHistoryByDppId(v1.getDigitalProductPassportId()).size());
        assertEquals(v1, repository.findCurrentByDppId(v1.getDigitalProductPassportId()).orElseThrow());

        repository.initializeSchema();
        repository.appendVersion(v2, 1L, new PostgresDppOperationContext("append-v2", t1.plusSeconds(20)));
        Dpp4Fun v3 = v2.toBuilder().nameplate(v2.getNameplate().toBuilder().brand("Third").build()).build();
        var projectionBeforeEventFailure = new PostgresDppProjectionRepository(dataSource())
                .find("dpp4fun", v1.getDigitalProductPassportId()).orElseThrow();
        try (Connection connection = dataSource().getConnection(); var statement = connection.createStatement()) { statement.execute("drop table dpp_lifecycle_events"); }
        assertThrows(RuntimeException.class, () -> repository.appendVersion(v3, 2L,
                new PostgresDppOperationContext("event-failure", t1.plusSeconds(30))));
        assertEquals(2, repository.findHistoryByDppId(v1.getDigitalProductPassportId()).size());
        assertEquals(v2, repository.findCurrentByDppId(v1.getDigitalProductPassportId()).orElseThrow());
        repository.initializeSchema();
        assertEquals(projectionBeforeEventFailure, new PostgresDppProjectionRepository(dataSource())
                .find("dpp4fun", v1.getDigitalProductPassportId()).orElseThrow());
    }

    @Test void disabledEventsDoNotAffectCanonicalHistoryOrProjectionUpdates() {
        try (Connection connection = dataSource().getConnection()) {
            new dppsdk.postgres.events.PostgresLifecycleEventRepository().initializeSchema(connection);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to prepare event table for disabled-event assertion", exception);
        }
        Dpp4FunPostgresRepository repository = new Dpp4FunPostgresRepository(dataSource(), false);
        repository.initializeSchema();
        repository.clearAll();
        Dpp4Fun first = dpp("events-disabled", "North", "Bed", "Steel", "Frame", "Slat");
        Instant t1 = Instant.parse("2026-09-01T08:00:00Z");
        repository.create(first, new PostgresDppOperationContext("create-no-events", t1));
        Dpp4Fun second = first.toBuilder().nameplate(first.getNameplate().toBuilder().brand("Updated").build()).build();
        repository.appendVersion(second, 1, new PostgresDppOperationContext("update-no-events", t1.plusSeconds(10)));
        assertThrows(PostgresDppVersionConflictException.class, () -> repository.appendVersion(first, 1,
                new PostgresDppOperationContext("stale-no-events", t1.plusSeconds(15))));
        repository.softDelete(first.getDigitalProductPassportId(), 2,
                new PostgresDppOperationContext("delete-no-events", t1.plusSeconds(20)));

        assertEquals(2, repository.findHistoryByDppId(first.getDigitalProductPassportId()).size());
        assertTrue(repository.findCurrentByDppId(first.getDigitalProductPassportId()).isEmpty());
        assertEquals(second, repository.findByDppIdAt(first.getDigitalProductPassportId(), t1.plusSeconds(15)).orElseThrow());
        assertFalse(repository.existsActiveByDppId(first.getDigitalProductPassportId()));
        assertTrue(repository.existsAnyByDppId(first.getDigitalProductPassportId()));
        assertTrue(new PostgresDppProjectionRepository(dataSource()).find("dpp4fun", first.getDigitalProductPassportId()).isEmpty());
        try (Connection connection = dataSource().getConnection(); var statement = connection.prepareStatement(
                "select count(*) from dpp_lifecycle_events where dpp_id = ?")) {
            statement.setString(1, first.getDigitalProductPassportId());
            try (var result = statement.executeQuery()) {
                result.next();
                assertEquals(0, result.getInt(1));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to verify events stayed disabled", exception);
        }
        assertThrows(IllegalStateException.class, () -> repository.findEventsByDppId(first.getDigitalProductPassportId()));
        assertThrows(IllegalStateException.class, () -> repository.recordLifecycleEvent(first.getDigitalProductPassportId(),
                DppLifecycleEventType.DPP_UPDATED, t1.plusSeconds(20), Map.of()));
    }

    private Dpp4FunSearchCriteria criteria(String brand, String material, String component, String part,
                                           int limit, int offset) {
        return new Dpp4FunSearchCriteria(brand, material, component, part, limit, offset);
    }
    private Dpp4Fun dpp(String suffix, String brand, String productType,
                         String material, String component, String part) {
        Dpp4Fun base = TestDataFactory.validDpp();
        DppHeader header = base.getHeader().toBuilder()
                .digitalProductPassportId("urn:test:dpp:" + suffix)
                .uniqueProductIdentifier("urn:test:product:" + suffix).build();
        return Dpp4Fun.builder(header)
                .nameplate(dppsdk.dpp4fun.model.aspect.Nameplate.builder().brand(brand)
                        .productType(java.util.Map.of("en", productType)).build())
                .billOfMaterials(BillOfMaterials.builder()
                        .addMaterial(new Material.Builder().name(material).build())
                        .addComponent(new Component.Builder().name(component).build())
                        .addPart(new Part.Builder().name(part).build()).build())
                .build();
    }
    private Dpp4FunPostgresRepository repository() {
        Dpp4FunPostgresRepository repository = new Dpp4FunPostgresRepository(dataSource());
        repository.initializeSchema();
        repository.clearAll();
        return repository;
    }
    private PGSimpleDataSource dataSource() {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setURL(POSTGRES.getJdbcUrl()); source.setUser(POSTGRES.getUsername()); source.setPassword(POSTGRES.getPassword());
        return source;
    }
}
