package dppsdk.postgres.events;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.postgres.core.PostgresDppOperationContext;
import dppsdk.postgres.core.PostgresDppVersionRepositorySupport;
import dppsdk.postgres.core.PostgresDppVersionRepositorySupport.VersionRecord;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Canonical repository composition that appends lifecycle events in the same transaction as each revision. */
public final class PostgresDppRepositoryWithEvents {
    private final DataSource dataSource;
    private final PostgresDppVersionRepositorySupport versions = new PostgresDppVersionRepositorySupport();
    private final PostgresLifecycleEventRepository events = new PostgresLifecycleEventRepository();
    public PostgresDppRepositoryWithEvents(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    public void initializeSchema() {
        transact("initialize repository events", connection -> {
            versions.initializeSchema(connection);
            events.initializeSchema(connection);
            return null;
        });
    }

    public VersionRecord create(DigitalProductPassport passport, PostgresDppOperationContext context) {
        return transact("create DPP with lifecycle event", connection -> {
            VersionRecord created = versions.create(connection, passport, context);
            events.append(connection, created.dppId(), created.versionNo(), context.operationId(),
                    DppLifecycleEventType.DPP_CREATED, context.occurredAt(), Map.of("productId", created.productId()));
            return created;
        });
    }

    public VersionRecord appendVersion(DigitalProductPassport passport, long expectedVersion,
                                       PostgresDppOperationContext context, DppLifecycleEventType eventType,
                                       Map<String, String> eventData) {
        return transact("append DPP version with lifecycle event", connection -> {
            VersionRecord updated = versions.appendVersion(connection, passport, expectedVersion, context);
            if (updated.versionNo() != expectedVersion) {
                events.append(connection, updated.dppId(), updated.versionNo(), context.operationId(), eventType,
                        context.occurredAt(), eventData);
            }
            return updated;
        });
    }

    public VersionRecord softDelete(String dppId, long expectedVersion, PostgresDppOperationContext context) {
        return transact("soft delete DPP with lifecycle event", connection -> {
            VersionRecord deleted = versions.softDelete(connection, dppId, expectedVersion, context.occurredAt());
            events.append(connection, dppId, deleted.versionNo(), context.operationId(), DppLifecycleEventType.DPP_DELETED,
                    context.occurredAt(), Map.of("productId", deleted.productId()));
            return deleted;
        });
    }

    public List<DppLifecycleEventRecord> findEventsByDppId(String dppId) {
        try (Connection connection = dataSource.getConnection()) { return events.findByDppId(connection, dppId); }
        catch (SQLException exception) { throw new IllegalStateException("Failed to read DPP lifecycle events", exception); }
    }

    private <T> T transact(String operation, SqlWork<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException | Error failure) {
                try { connection.rollback(); } catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
                if (failure instanceof SQLException sqlFailure) throw new IllegalStateException("Failed to " + operation, sqlFailure);
                throw failure;
            }
        } catch (SQLException exception) { throw new IllegalStateException("Failed to " + operation, exception); }
    }

    @FunctionalInterface private interface SqlWork<T> { T run(Connection connection) throws SQLException; }
}
