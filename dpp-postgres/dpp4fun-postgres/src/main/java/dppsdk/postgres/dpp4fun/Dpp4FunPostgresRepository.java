package dppsdk.postgres.dpp4fun;

import dppsdk.core.model.DppCore;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.postgres.core.DppLifecycleEventRecord;
import dppsdk.postgres.core.DppLifecycleEventType;
import dppsdk.postgres.core.DppPage;
import dppsdk.postgres.core.DppPageRequest;
import dppsdk.postgres.core.PostgresDppCoreMapper;
import dppsdk.postgres.core.PostgresDppOperationContext;
import dppsdk.postgres.core.PostgresDppVersionRepositorySupport;
import dppsdk.postgres.core.PostgresDppVersionRepositorySupport.VersionRecord;
import dppsdk.postgres.core.PostgresJdbcSupport;
import dppsdk.postgres.core.PostgresLifecycleEventRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PostgreSQL repository for canonical {@link Dpp4Fun} objects.
 *
 * <p>The repository expects already validated domain objects. JSON handling, merge patching, and fine-granular
 * element-path logic remain outside this module.</p>
 */
public final class Dpp4FunPostgresRepository {

    private final DataSource dataSource;
    private final PostgresDppVersionRepositorySupport versionSupport;
    private final PostgresDppCoreMapper coreMapper;
    private final Dpp4FunPostgresMapper dpp4FunMapper;
    private final PostgresLifecycleEventRepository lifecycleEventRepository;
    private final Dpp4FunQueryRepository queryRepository;

    public Dpp4FunPostgresRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        this.versionSupport = new PostgresDppVersionRepositorySupport();
        this.coreMapper = new PostgresDppCoreMapper();
        this.dpp4FunMapper = new Dpp4FunPostgresMapper();
        this.lifecycleEventRepository = new PostgresLifecycleEventRepository();
        this.queryRepository = new Dpp4FunQueryRepository();
        initializeSchema();
    }

    public Dpp4Fun create(Dpp4Fun dpp, PostgresDppOperationContext context) {
        return inTransaction(connection -> {
            VersionRecord created = versionSupport.create(connection, dpp, context);
            coreMapper.insertVersionData(connection, created.versionId(), dpp.getCoreDpp());
            dpp4FunMapper.insertVersionData(connection, created.versionId(), dpp);
            lifecycleEventRepository.append(connection, dpp.getDppId(), DppLifecycleEventType.DPP_CREATED, context.occurredAt(), Map.of("productId", dpp.getProductId()));
            return dpp;
        });
    }

    public Dpp4Fun appendVersion(Dpp4Fun dpp, long expectedCurrentVersion, PostgresDppOperationContext context) {
        return appendVersion(
                dpp,
                expectedCurrentVersion,
                context,
                DppLifecycleEventType.DPP_UPDATED,
                Map.of("productId", dpp.getProductId())
        );
    }

    public Dpp4Fun appendVersion(
            Dpp4Fun dpp,
            long expectedCurrentVersion,
            PostgresDppOperationContext context,
            DppLifecycleEventType eventType,
            Map<String, String> eventData
    ) {
        return inTransaction(connection -> {
            VersionRecord created = versionSupport.appendVersion(connection, dpp, expectedCurrentVersion, context);
            coreMapper.insertVersionData(connection, created.versionId(), dpp.getCoreDpp());
            dpp4FunMapper.insertVersionData(connection, created.versionId(), dpp);
            lifecycleEventRepository.append(connection, dpp.getDppId(), eventType, context.occurredAt(), eventData);
            return dpp;
        });
    }

    public Optional<Dpp4Fun> findCurrentByDppId(String dppId) {
        return withConnection(connection -> versionSupport.findCurrentByDppId(connection, dppId)
                .map(record -> readVersion(connection, record.versionId())));
    }

    public boolean existsActiveByDppId(String dppId) {
        return withConnection(connection -> versionSupport.existsActiveByDppId(connection, dppId));
    }

    public boolean existsAnyByDppId(String dppId) {
        return withConnection(connection -> versionSupport.existsAnyByDppId(connection, dppId));
    }

    public Optional<Long> findCurrentVersionNoByDppId(String dppId) {
        return withConnection(connection -> versionSupport.findCurrentVersionNoByDppId(connection, dppId));
    }

    public Optional<Dpp4Fun> findCurrentByProductId(String productId) {
        return withConnection(connection -> versionSupport.findCurrentByProductId(connection, productId)
                .map(record -> readVersion(connection, record.versionId())));
    }

    public Optional<Dpp4Fun> findByProductIdAt(String productId, java.time.Instant timestamp) {
        return withConnection(connection -> versionSupport.findByProductIdAt(connection, productId, timestamp)
                .map(record -> readVersion(connection, record.versionId())));
    }

    public Optional<Dpp4Fun> findByDppIdAt(String dppId, java.time.Instant timestamp) {
        return withConnection(connection -> versionSupport.findByDppIdAt(connection, dppId, timestamp)
                .map(record -> readVersion(connection, record.versionId())));
    }

    public DppPage<String> findActiveDppIdsByProductIds(List<String> productIds, DppPageRequest pageRequest) {
        return withConnection(connection -> queryRepository.findActiveDppIdsByProductIds(connection, productIds, pageRequest));
    }

    public List<String> findAllActiveDppIds() {
        return withConnection(versionSupport::findAllActiveDppIds);
    }

    public List<Dpp4FunVersionSummary> findHistoryByDppId(String dppId) {
        return withConnection(connection -> versionSupport.findHistoryByDppId(connection, dppId).stream()
                .map(record -> new Dpp4FunVersionSummary(
                        record.dppId(),
                        record.productId(),
                        record.versionNo(),
                        record.status(),
                        record.validFrom(),
                        record.validTo()
                ))
                .toList());
    }

    public void softDelete(String dppId, long expectedCurrentVersion, java.time.Instant deletedAt) {
        inTransaction(connection -> {
            VersionRecord deleted = versionSupport.softDelete(connection, dppId, expectedCurrentVersion, deletedAt);
            lifecycleEventRepository.append(connection, dppId, DppLifecycleEventType.DPP_DELETED, deletedAt, Map.of("productId", deleted.productId()));
            return null;
        });
    }

    public List<DppLifecycleEventRecord> findEventsByDppId(String dppId) {
        return withConnection(connection -> lifecycleEventRepository.findByDppId(connection, dppId));
    }

    public DppLifecycleEventRecord recordLifecycleEvent(String dppId, DppLifecycleEventType eventType, java.time.Instant occurredAt, Map<String, String> data) {
        return inTransaction(connection -> lifecycleEventRepository.append(connection, dppId, eventType, occurredAt, data));
    }

    public List<Dpp4FunSearchResult> search(Dpp4FunSearchCriteria criteria) {
        return withConnection(connection -> queryRepository.search(connection, criteria));
    }

    public void clearAll() {
        withConnection(connection -> {
            try (java.sql.Statement statement = connection.createStatement()) {
                statement.execute("""
                        truncate table
                            dpp_lifecycle_events,
                            dpp4fun_parts,
                            dpp4fun_components,
                            dpp4fun_materials,
                            dpp4fun_bill_of_materials,
                            dpp4fun_features,
                            dpp4fun_dimensions,
                            dpp4fun_characteristics,
                            dpp4fun_classification_tags,
                            dpp4fun_classifications,
                            dpp_documentation,
                            dpp_nameplates,
                            dpp_organizations,
                            dpp_contacts,
                            dpp_addresses,
                            dpp_emails,
                            dpp_telephones,
                            dpp_passport_update_dates,
                            dpp_passport_metadata,
                            dpp_versions,
                            dpp_passports
                        restart identity cascade
                        """);
            }
            return null;
        });
    }

    private void initializeSchema() {
        try (Connection connection = dataSource.getConnection()) {
            versionSupport.initializeSchema(connection);
            PostgresJdbcSupport.executeSqlScript(connection, "/postgres/dpp4fun-schema.sql");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize Dpp4Fun PostgreSQL schema", exception);
        }
    }

    private Dpp4Fun readVersion(Connection connection, long versionId) {
        try {
            DppCore coreDpp = coreMapper.readVersionData(connection, versionId);
            return dpp4FunMapper.readVersionData(connection, versionId, coreDpp);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read Dpp4Fun version " + versionId, exception);
        }
    }

    private <T> T inTransaction(SqlFunction<Connection, T> action) {
        try (Connection connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = action.apply(connection);
                connection.commit();
                return result;
            } catch (Exception exception) {
                connection.rollback();
                throw wrap(exception);
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Database transaction failed", exception);
        }
    }

    private <T> T withConnection(SqlFunction<Connection, T> action) {
        try (Connection connection = dataSource.getConnection()) {
            return action.apply(connection);
        } catch (Exception exception) {
            throw wrap(exception);
        }
    }

    private IllegalStateException wrap(Exception exception) {
        return exception instanceof IllegalStateException stateException
                ? stateException
                : new IllegalStateException(exception.getMessage(), exception);
    }

    @FunctionalInterface
    private interface SqlFunction<T, R> {
        R apply(T value) throws Exception;
    }
}
