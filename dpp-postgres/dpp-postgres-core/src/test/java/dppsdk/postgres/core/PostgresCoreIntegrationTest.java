package dppsdk.postgres.core;

import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.model.RelatedResource;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.transport.json.DppJsonCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresCoreIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void storesCompleteImmutableSnapshotsAndResolvesHalfOpenBoundaries() throws Exception {
        PostgresDppRepository repository = new PostgresDppRepository(dataSource());
        Instant t1 = Instant.parse("2026-01-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T11:00:00Z");
        Instant t3 = Instant.parse("2026-01-01T12:00:00Z");
        DigitalProductPassport v1 = passport("urn:test:passport:snapshots", "urn:test:product:snapshots", t1, "one");
        DigitalProductPassport v2 = passport("urn:test:passport:snapshots", "urn:test:product:snapshots", t2, "two");
        DigitalProductPassport v3 = passport("urn:test:passport:snapshots", "urn:test:product:snapshots", t3, "three");

        repository.create(v1, new PostgresDppOperationContext("create-v1", t1));
        var v1Summary = repository.findHistoryByDppId(v1.getDppHeader().getDigitalProductPassportId()).get(0);
        repository.appendVersion(v2, 1L, new PostgresDppOperationContext("append-v2", t2));
        repository.appendVersion(v3, 2L, new PostgresDppOperationContext("append-v3", t3));

        String dppId = v1.getDppHeader().getDigitalProductPassportId();
        assertEquals(v1, repository.findByDppIdAt(dppId, t2.minusNanos(1_000)).orElseThrow());
        assertEquals(v2, repository.findByDppIdAt(dppId, t2).orElseThrow());
        assertEquals(v3, repository.findByDppIdAt(dppId, t3).orElseThrow());
        assertEquals(v3, repository.findCurrentByDppId(dppId).orElseThrow());
        assertEquals(v3, repository.findCurrentByProductId("urn:test:product:snapshots").orElseThrow());
        assertEquals(List.of(v1, v2, v3), List.of(
                repository.findVersion(dppId, 1L).orElseThrow(),
                repository.findVersion(dppId, 2L).orElseThrow(),
                repository.findVersion(dppId, 3L).orElseThrow()));
        assertEquals(List.of(1L, 2L, 3L), repository.findHistoryByDppId(dppId).stream()
                .map(PostgresDppVersionRepositorySupport.VersionRecord::versionNo).toList());
        assertEquals(t2, repository.findHistoryByDppId(dppId).get(0).validTo());
        assertEquals(PostgresDppStatus.SUPERSEDED, repository.findHistoryByDppId(dppId).get(0).status());
        assertEquals("append-v2", repository.findHistoryByDppId(dppId).get(1).operationId());
        assertEquals(3L, repository.findCurrentVersionNoByDppId(dppId).orElseThrow());
        assertDatabaseVersionState(dataSource(), dppId, 1L, PostgresDppStatus.SUPERSEDED, t2, null);
        assertDatabaseVersionState(dataSource(), dppId, 2L, PostgresDppStatus.SUPERSEDED, t3, null);
        assertDatabaseVersionState(dataSource(), dppId, 3L, PostgresDppStatus.ACTIVE, null, null);
        assertTrue(jsonbEquals(dataSource(), v1Summary.versionId(), new DppJsonCodec().toExpandedJson(v1)));
        assertEquals(64, v1Summary.contentHash().length());
        assertEquals(v1Summary.contentHash(), contentHash(dataSource(), v1Summary.versionId()));

        var second = passport("urn:test:passport:snapshots-b", "urn:test:product:snapshots-b", t1, "other");
        repository.create(second, new PostgresDppOperationContext("create-other", t1));
        DppPage<String> firstPage = repository.findActiveDppIds(new DppPageRequest(null, 1));
        assertEquals(1, firstPage.items().size());
        assertEquals("1", firstPage.nextCursor());
        assertEquals(1, repository.findActiveDppIds(new DppPageRequest(firstPage.nextCursor(), 1)).items().size());
        assertNotEquals(v1, v2);
    }

    @Test
    void batchProductLookupPreservesInputOrderingAndExistingInputOffsetCursorSemantics() {
        PostgresDppRepository repository = new PostgresDppRepository(dataSource());
        Instant at = Instant.parse("2026-01-02T10:00:00Z");
        DigitalProductPassport first = passport("urn:test:batch:first", "urn:test:batch:product:first", at, "one");
        DigitalProductPassport second = passport("urn:test:batch:second", "urn:test:batch:product:second", at.plusSeconds(1), "two");
        repository.create(first, new PostgresDppOperationContext("batch-first", at));
        repository.create(second, new PostgresDppOperationContext("batch-second", at.plusSeconds(1)));

        List<String> input = List.of("missing-before", "urn:test:batch:product:first",
                "missing-middle", "urn:test:batch:product:second");
        DppPage<String> firstPage = repository.findActiveDppIdsByProductIds(input, new DppPageRequest(null, 1));
        assertEquals(List.of("urn:test:batch:first"), firstPage.items());
        assertEquals("2", firstPage.nextCursor());
        DppPage<String> secondPage = repository.findActiveDppIdsByProductIds(input,
                new DppPageRequest(firstPage.nextCursor(), 1));
        assertEquals(List.of("urn:test:batch:second"), secondPage.items());
        assertEquals("4", secondPage.nextCursor());
    }

    @Test
    void sameContentDoesNotCreateRevisionAndSoftDeletePreservesCanonicalHistory() throws Exception {
        PostgresDppRepository repository = new PostgresDppRepository(dataSource());
        Instant createdAt = Instant.parse("2026-02-01T10:00:00Z");
        Instant deletedAt = createdAt.plusSeconds(60);
        Map<String, Object> firstContent = new LinkedHashMap<>();
        firstContent.put("zeta", new BigDecimal("1.0"));
        firstContent.put("alpha", Map.of("second", 2, "first", 1));
        DigitalProductPassport passport = passportWithValue("urn:test:passport:delete", "urn:test:product:delete",
                createdAt, firstContent);
        Map<String, Object> equivalentContent = new LinkedHashMap<>();
        equivalentContent.put("alpha", Map.of("first", 1, "second", 2));
        equivalentContent.put("zeta", new BigDecimal("1.00"));
        DigitalProductPassport equivalentPassport = passportWithValue("urn:test:passport:delete",
                "urn:test:product:delete", createdAt, equivalentContent);
        assertEquals(passport, equivalentPassport);
        assertNotEquals(new DppJsonCodec().toExpandedJson(passport),
                new DppJsonCodec().toExpandedJson(equivalentPassport));
        String dppId = passport.getDppHeader().getDigitalProductPassportId();
        repository.create(passport, new PostgresDppOperationContext("create-delete", createdAt));

        var sameContent = repository.appendVersion(equivalentPassport, 1L,
                new PostgresDppOperationContext("same-content", createdAt.plusSeconds(30)));
        assertEquals(1L, sameContent.versionNo());
        assertEquals(1, repository.findHistoryByDppId(dppId).size());

        var deleted = repository.softDelete(dppId, 1L, deletedAt);
        assertEquals(PostgresDppStatus.DELETED, deleted.status());
        assertEquals(deletedAt, deleted.validTo());
        assertDatabaseVersionState(dataSource(), dppId, 1L, PostgresDppStatus.DELETED, deletedAt, deletedAt);
        assertFalse(repository.existsActiveByDppId(dppId));
        assertTrue(repository.existsAnyByDppId(dppId));
        assertTrue(repository.findCurrentByDppId(dppId).isEmpty());
        assertEquals(passport, repository.findVersion(dppId, 1L).orElseThrow());
        assertEquals("active", repository.findVersion(dppId, 1L).orElseThrow().getDppHeader().getDppStatus());
        assertEquals(passport, repository.findByDppIdAt(dppId, deletedAt.minusSeconds(1)).orElseThrow());
        assertTrue(repository.findByDppIdAt(dppId, deletedAt).isEmpty());
        assertEquals(1, repository.findHistoryByDppId(dppId).size());
    }

    @Test
    void physicalPassportDeleteCannotCascadeIntoArchivedRevisions() throws Exception {
        DataSource dataSource = dataSource();
        PostgresDppRepository repository = new PostgresDppRepository(dataSource);
        Instant createdAt = Instant.parse("2026-02-15T10:00:00Z");
        DigitalProductPassport passport = passport("urn:test:passport:retained", "urn:test:product:retained",
                createdAt, "retained");
        String dppId = passport.getDppHeader().getDigitalProductPassportId();
        repository.create(passport, new PostgresDppOperationContext("create-retained", createdAt));

        assertThrows(java.sql.SQLException.class, () -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "delete from dpp_passports where dpp_id = ?")) {
                statement.setString(1, dppId);
                statement.executeUpdate();
            }
        });

        assertEquals(passport, repository.findVersion(dppId, 1L).orElseThrow());
        assertEquals(1, repository.findHistoryByDppId(dppId).size());
    }

    @Test
    void externalPassportResourceIsPreservedWithoutCreatingDatabaseOwnership() throws Exception {
        PostgresDppRepository repository = new PostgresDppRepository(dataSource());
        Instant createdAt = Instant.parse("2026-02-20T10:00:00Z");
        DigitalProductPassport referenced = passport("urn:test:passport:resource-target",
                "urn:test:product:resource-target", createdAt, "target");
        DigitalProductPassport sourceBase = passport("urn:test:passport:resource-source",
                "urn:test:product:resource-source", createdAt, "source");
        RelatedResource externalPassport = new RelatedResource.Builder().elementId("relatedPassport")
                .contentType("application/json")
                .url("https://example.test/dpp/" + referenced.getDppHeader().getDigitalProductPassportId())
                .resourceTitle("Related passport")
                .build();
        DigitalProductPassport source = sourceBase.toBuilder().dataElements(List.of(externalPassport)).build();

        repository.create(referenced, new PostgresDppOperationContext("create-target", createdAt));
        repository.create(source, new PostgresDppOperationContext("create-source", createdAt.plusSeconds(1)));
        repository.softDelete(referenced.getDppHeader().getDigitalProductPassportId(), 1L,
                createdAt.plusSeconds(2));

        DigitalProductPassport restoredSource = repository.findVersion(
                source.getDppHeader().getDigitalProductPassportId(), 1L).orElseThrow();
        assertEquals(source, restoredSource);
        assertEquals(referenced.getDppHeader().getDigitalProductPassportId(),
                ((RelatedResource) restoredSource.getDataElements().get(0)).getUrl()
                        .substring("https://example.test/dpp/".length()));
        assertEquals(1, repository.findHistoryByDppId(
                referenced.getDppHeader().getDigitalProductPassportId()).size());
    }

    @Test
    void canonicalSnapshotHashDetectsContentChangesMadeOutsideTheRepository() throws Exception {
        DataSource dataSource = dataSource();
        PostgresDppRepository repository = new PostgresDppRepository(dataSource);
        Instant createdAt = Instant.parse("2026-02-25T10:00:00Z");
        DigitalProductPassport passport = passport("urn:test:passport:tampered", "urn:test:product:tampered",
                createdAt, "original");
        var stored = repository.create(passport, new PostgresDppOperationContext("create-tampered", createdAt));

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "update dpp_versions set content = content || '{\"tampered\":true}'::jsonb where id = ?")) {
            statement.setLong(1, stored.versionId());
            assertEquals(1, statement.executeUpdate());
        }

        assertThrows(IllegalStateException.class, () -> repository.findVersion(
                passport.getDppHeader().getDigitalProductPassportId(), 1L));
    }

    @Test
    void timestampsUsePostgresMicrosecondPrecisionForWritesAndPointInTimeReads() {
        PostgresDppRepository repository = new PostgresDppRepository(dataSource());
        Instant requestedAt = Instant.parse("2026-02-26T10:00:00Z").plusNanos(123_456_789);
        Instant storedAt = requestedAt.truncatedTo(ChronoUnit.MICROS);
        DigitalProductPassport first = passport("urn:test:passport:precision", "urn:test:product:precision",
                requestedAt, "first");
        String dppId = first.getDppHeader().getDigitalProductPassportId();

        var created = repository.create(first, new PostgresDppOperationContext("create-precision", requestedAt));

        assertEquals(storedAt, created.validFrom());
        assertTrue(repository.findByDppIdAt(dppId, storedAt.minusNanos(1)).isEmpty());
        assertEquals(first, repository.findByDppIdAt(dppId, requestedAt).orElseThrow());
        DigitalProductPassport tooSoon = passport(dppId, "urn:test:product:precision", requestedAt.plusNanos(100), "too-soon");
        assertThrows(IllegalArgumentException.class, () -> repository.appendVersion(tooSoon, 1L,
                new PostgresDppOperationContext("too-soon", requestedAt.plusNanos(100))));
        assertEquals(1, repository.findHistoryByDppId(dppId).size());

        DigitalProductPassport second = passport(dppId, "urn:test:product:precision", requestedAt.plusNanos(1_000), "second");
        repository.appendVersion(second, 1L,
                new PostgresDppOperationContext("append-precision", requestedAt.plusNanos(1_000)));
        assertEquals(storedAt.plus(1, ChronoUnit.MICROS), repository.findHistoryByDppId(dppId).get(0).validTo());
        Instant nextBoundary = storedAt.plus(1, ChronoUnit.MICROS);
        assertEquals(first, repository.findByDppIdAt(dppId, nextBoundary.minusNanos(1)).orElseThrow());
        assertEquals(second, repository.findByDppIdAt(dppId, nextBoundary).orElseThrow());
    }

    @Test
    void concurrentWritersUsingOneExpectedVersionProduceOneRevisionAndOneConflict() throws Exception {
        DataSource dataSource = dataSource();
        PostgresDppRepository repository = new PostgresDppRepository(dataSource);
        Instant t1 = Instant.parse("2026-03-01T10:00:00Z");
        DigitalProductPassport base = passport("urn:test:passport:concurrent", "urn:test:product:concurrent", t1, "base");
        repository.create(base, new PostgresDppOperationContext("create-concurrent", t1));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> appendOutcome(repository,
                    passport("urn:test:passport:concurrent", "urn:test:product:concurrent", t1.plusSeconds(1), "first"),
                    "append-first", ready, start));
            var second = pool.submit(() -> appendOutcome(repository,
                    passport("urn:test:passport:concurrent", "urn:test:product:concurrent", t1.plusSeconds(2), "second"),
                    "append-second", ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            assertNotEquals(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
            assertEquals(2, repository.findHistoryByDppId("urn:test:passport:concurrent").size());
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean appendOutcome(PostgresDppRepository repository, DigitalProductPassport dpp, String operationId,
                                 CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("writer start timed out");
        try {
            repository.appendVersion(dpp, 1L,
                    new PostgresDppOperationContext(operationId, dpp.getDppHeader().getLastUpdated()));
            return true;
        } catch (PostgresDppVersionConflictException expected) {
            return false;
        }
    }

    private boolean jsonbEquals(DataSource dataSource, long versionId, String expandedJson) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select content = cast(? as jsonb) from dpp_versions where id = ?")) {
            statement.setString(1, expandedJson);
            statement.setLong(2, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getBoolean(1);
            }
        }
    }

    private String contentHash(DataSource dataSource, long versionId) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select content_hash from dpp_versions where id = ?")) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getString(1);
            }
        }
    }

    private void assertDatabaseVersionState(DataSource dataSource, String dppId, long versionNo,
                                           PostgresDppStatus status, Instant validTo, Instant deletedAt)
            throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     select v.status, v.valid_to, p.deleted_at
                     from dpp_passports p join dpp_versions v on v.passport_id = p.id
                     where p.dpp_id = ? and v.version_no = ?
                     """)) {
            statement.setString(1, dppId);
            statement.setLong(2, versionNo);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertEquals(status, PostgresDppStatus.valueOf(resultSet.getString("status")));
                var actualValidTo = resultSet.getTimestamp("valid_to");
                assertEquals(validTo, actualValidTo == null ? null : actualValidTo.toInstant());
                var actualDeletedAt = resultSet.getTimestamp("deleted_at");
                assertEquals(deletedAt, actualDeletedAt == null ? null : actualDeletedAt.toInstant());
            }
        }
    }

    private DigitalProductPassport passport(String dppId, String productId, Instant updatedAt, String value) {
        return passportWithValue(dppId, productId, updatedAt, value);
    }

    private DigitalProductPassport passportWithValue(String dppId, String productId, Instant updatedAt, Object value) {
        DppHeader header = new DppHeader.Builder().digitalProductPassportId(dppId)
                .uniqueProductIdentifier(productId).granularity(Granularity.ITEM)
                .dppSchemaVersion("EN18223:2026").dppStatus("active").lastUpdated(updatedAt)
                .economicOperatorId("operator-test").build();
        return new DigitalProductPassport.Builder().dppHeader(header).dataElements(List.of(
                new DataElementCollection.Builder().elementId("profileSpecificElement")
                        .dataElements(List.of(new SingleValuedDataElement.Builder()
                                .elementId("nestedValue").value(value).build()))
                        .build())).build();
    }

    private DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }
}
