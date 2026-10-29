package dppsdk.postgres.dpp4fun;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.postgres.core.DppPage;
import dppsdk.postgres.core.DppPageRequest;
import dppsdk.postgres.core.PostgresDppCoreMapper;
import dppsdk.postgres.core.PostgresDppOperationContext;
import dppsdk.postgres.core.PostgresDppSnapshot;
import dppsdk.postgres.core.PostgresDppVersionRepositorySupport;
import dppsdk.postgres.core.PostgresDppVersionRepositorySupport.VersionRecord;
import dppsdk.postgres.events.DppLifecycleEventRecord;
import dppsdk.postgres.events.DppLifecycleEventType;
import dppsdk.postgres.events.PostgresLifecycleEventRepository;
import dppsdk.postgres.projection.PostgresDppProjectionRepository;
import dppsdk.postgres.projection.PostgresProjectionSchemaManager;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

/** Convenient Dpp4Fun composition over canonical snapshots, optional projections, and audit events. */
public final class Dpp4FunPostgresRepository {
    private final DataSource dataSource;
    private final PostgresDppVersionRepositorySupport versions = new PostgresDppVersionRepositorySupport();
    private final PostgresDppCoreMapper coreMapper = new PostgresDppCoreMapper();
    private final Dpp4FunPostgresMapper mapper = new Dpp4FunPostgresMapper();
    private final PostgresLifecycleEventRepository events = new PostgresLifecycleEventRepository();
    private final PostgresDppProjectionRepository projections;
    private final Dpp4FunQueryRepository queries = new Dpp4FunQueryRepository();
    private final boolean eventsEnabled;

    public Dpp4FunPostgresRepository(DataSource dataSource) {
        this(dataSource, true);
    }

    public Dpp4FunPostgresRepository(DataSource dataSource, boolean eventsEnabled) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.projections = new PostgresDppProjectionRepository(dataSource);
        this.eventsEnabled = eventsEnabled;
    }

    /** Explicit development/test bootstrap. Repository construction never creates projection indexes. */
    public void initializeSchema() {
        try (Connection connection = dataSource.getConnection()) {
            versions.initializeSchema(connection);
            PostgresProjectionSchemaManager projectionSchema = new PostgresProjectionSchemaManager(dataSource);
            projectionSchema.createTable(connection);
            if (eventsEnabled) events.initializeSchema(connection);
            projectionSchema.applyIndexes(connection, mapper.projectionDefinition());
        } catch (SQLException exception) { throw new IllegalStateException("Failed to initialize Dpp4Fun PostgreSQL schemas", exception); }
    }

    public Dpp4Fun create(Dpp4Fun dpp, PostgresDppOperationContext context) {
        DigitalProductPassport passport = dpp.asDigitalProductPassport();
        inTransaction(connection -> {
            VersionRecord created = versions.create(connection, passport, context);
            projections.upsertCurrent(connection, "dpp4fun", created.dppId(), created.versionId(), mapper.project(passport));
            if (eventsEnabled) events.append(connection, created.dppId(), created.versionNo(), context.operationId(), DppLifecycleEventType.DPP_CREATED,
                    context.occurredAt(), Map.of("productId", created.productId()));
            return null;
        });
        return dpp;
    }

    public Dpp4Fun appendVersion(Dpp4Fun dpp, long expectedCurrentVersion, PostgresDppOperationContext context) {
        return appendVersion(dpp, expectedCurrentVersion, context, DppLifecycleEventType.DPP_UPDATED,
                Map.of("productId", dpp.getUniqueProductIdentifier()));
    }

    public Dpp4Fun appendVersion(Dpp4Fun dpp, long expectedCurrentVersion, PostgresDppOperationContext context,
                                 DppLifecycleEventType eventType, Map<String, String> eventData) {
        DigitalProductPassport passport = dpp.asDigitalProductPassport();
        inTransaction(connection -> {
            VersionRecord updated = versions.appendVersion(connection, passport, expectedCurrentVersion, context);
            if (updated.versionNo() != expectedCurrentVersion) {
                projections.upsertCurrent(connection, "dpp4fun", updated.dppId(), updated.versionId(), mapper.project(passport));
                if (eventsEnabled) events.append(connection, updated.dppId(), updated.versionNo(), context.operationId(), eventType,
                        context.occurredAt(), eventData);
            }
            return null;
        });
        return dpp;
    }

    public Optional<Dpp4Fun> findCurrentByDppId(String dppId) {
        return query(connection -> versions.findCurrentByDppId(connection, dppId).map(v -> readVersion(connection, v.versionId())));
    }
    public Optional<PostgresDppSnapshot> findCurrentPassportSnapshotByDppId(String dppId) {
        return query(connection -> versions.findCurrentByDppId(connection, dppId).map(version -> {
            try {
                return new PostgresDppSnapshot(coreMapper.readVersionData(connection, version.versionId()),
                        version.versionNo());
            } catch (SQLException exception) {
                throw new IllegalStateException("Failed to read current DPP snapshot " + dppId, exception);
            }
        }));
    }
    public Optional<Dpp4Fun> findCurrentByProductId(String productId) {
        return query(connection -> versions.findCurrentByProductId(connection, productId).map(v -> readVersion(connection, v.versionId())));
    }
    public Optional<Dpp4Fun> findByProductIdAt(String productId, Instant at) {
        return query(connection -> versions.findByProductIdAt(connection, productId, at).map(v -> readVersion(connection, v.versionId())));
    }
    public Optional<Dpp4Fun> findByDppIdAt(String dppId, Instant at) {
        return query(connection -> versions.findByDppIdAt(connection, dppId, at).map(v -> readVersion(connection, v.versionId())));
    }
    public boolean existsActiveByDppId(String dppId) { return query(c -> versions.existsActiveByDppId(c, dppId)); }
    public boolean existsAnyByDppId(String dppId) { return query(c -> versions.existsAnyByDppId(c, dppId)); }
    public Optional<Long> findCurrentVersionNoByDppId(String dppId) { return query(c -> versions.findCurrentVersionNoByDppId(c, dppId)); }
    public List<String> findAllActiveDppIds() { return query(versions::findAllActiveDppIds); }
    public DppPage<String> findActiveDppIdsByProductIds(List<String> productIds, DppPageRequest pageRequest) {
        return query(c -> queries.findActiveDppIdsByProductIds(c, productIds, pageRequest));
    }
    public List<Dpp4FunVersionSummary> findHistoryByDppId(String dppId) {
        return query(c -> versions.findHistoryByDppId(c, dppId).stream().map(this::summary).toList());
    }
    public List<Dpp4FunSearchResult> search(Dpp4FunSearchCriteria criteria) { return query(c -> queries.search(c, criteria)); }
    public int rebuildProjection() { return projections.rebuild(mapper.projectionDefinition()); }

    public void softDelete(String dppId, long expectedCurrentVersion, Instant deletedAt) {
        softDelete(dppId, expectedCurrentVersion, new PostgresDppOperationContext(null, deletedAt));
    }
    public void softDelete(String dppId, long expectedCurrentVersion, PostgresDppOperationContext context) {
        inTransaction(connection -> {
            VersionRecord deleted = versions.softDelete(connection, dppId, expectedCurrentVersion, context.occurredAt());
            projections.remove(connection, "dpp4fun", dppId);
            if (eventsEnabled) events.append(connection, dppId, deleted.versionNo(), context.operationId(), DppLifecycleEventType.DPP_DELETED,
                    context.occurredAt(), Map.of("productId", deleted.productId()));
            return null;
        });
    }
    public List<DppLifecycleEventRecord> findEventsByDppId(String dppId) {
        if (!eventsEnabled) throw new IllegalStateException("Lifecycle events are disabled");
        return query(c -> events.findByDppId(c, dppId));
    }
    public DppLifecycleEventRecord recordLifecycleEvent(String dppId, DppLifecycleEventType type, Instant occurredAt, Map<String, String> data) {
        if (!eventsEnabled) throw new IllegalStateException("Lifecycle events are disabled");
        return inTransaction(c -> events.append(c, dppId, type, occurredAt, data));
    }

    void clearAll() {
        query(connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("truncate table dpp_lifecycle_events, dpp_projections, dpp_versions, dpp_passports restart identity cascade");
            }
            return null;
        });
    }

    private Dpp4Fun readVersion(Connection connection, long versionId) {
        try { return Dpp4Fun.from(coreMapper.readVersionData(connection, versionId)); }
        catch (SQLException exception) { throw new IllegalStateException("Failed to read Dpp4Fun revision " + versionId, exception); }
    }
    private Dpp4FunVersionSummary summary(VersionRecord value) {
        return new Dpp4FunVersionSummary(value.dppId(), value.productId(), value.versionNo(), value.status(), value.validFrom(), value.validTo());
    }
    private <T> T query(SqlWork<T> work) {
        try (Connection connection = dataSource.getConnection()) { return work.run(connection); }
        catch (SQLException exception) { throw new IllegalStateException("Dpp4Fun PostgreSQL query failed", exception); }
    }
    private <T> T inTransaction(SqlWork<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try { T result = work.run(connection); connection.commit(); return result; }
            catch (SQLException | RuntimeException | Error failure) { try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); } throw failure; }
        } catch (SQLException exception) { throw new IllegalStateException("Dpp4Fun PostgreSQL transaction failed", exception); }
    }
    @FunctionalInterface private interface SqlWork<T> { T run(Connection connection) throws SQLException; }
}
