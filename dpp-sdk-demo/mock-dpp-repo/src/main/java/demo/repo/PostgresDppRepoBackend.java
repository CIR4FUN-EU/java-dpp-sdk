package demo.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dpp.repo.payloads.DppStatusCode;
import dppsdk.postgres.core.DppLifecycleEventRecord;
import dppsdk.postgres.core.DppLifecycleEventType;
import dppsdk.postgres.core.DppPageRequest;
import dppsdk.postgres.core.PostgresDppOperationContext;
import dppsdk.postgres.dpp4fun.Dpp4FunPostgresRepository;
import dppsdk.postgres.dpp4fun.Dpp4FunVersionSummary;
import dppsdk.dpp4fun.model.Dpp4Fun;
import org.postgresql.util.PSQLException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class PostgresDppRepoBackend implements DppRepoBackend {

    private final Dpp4FunPostgresRepository repository;
    private final ObjectMapper objectMapper;
    private final DataSource dataSource;

    PostgresDppRepoBackend(Dpp4FunPostgresRepository repository, ObjectMapper objectMapper, DataSource dataSource) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;
    }

    @Override
    public void create(Dpp4Fun dpp, Instant occurredAt) {
        try {
            repository.create(dpp, new PostgresDppOperationContext(null, occurredAt));
        } catch (RuntimeException exception) {
            throw mapException(exception, dpp.getDppId());
        }
    }

    @Override
    public Optional<Dpp4Fun> findCurrentByDppId(String dppId) {
        return repository.findCurrentByDppId(dppId);
    }

    @Override
    public boolean existsActiveByDppId(String dppId) {
        return repository.existsActiveByDppId(dppId);
    }

    @Override
    public boolean existsAnyByDppId(String dppId) {
        return !repository.findHistoryByDppId(dppId).isEmpty();
    }

    @Override
    public Optional<Dpp4Fun> findCurrentByProductId(String productId) {
        return repository.findCurrentByProductId(productId);
    }

    @Override
    public Optional<Dpp4Fun> findByProductIdAt(String productId, Instant timestamp) {
        return repository.findByProductIdAt(productId, timestamp);
    }

    @Override
    public DppIdPage findActiveDppIdsByProductIds(List<String> productIds, int offset, int limit) {
        dppsdk.postgres.core.DppPage<String> page =
                repository.findActiveDppIdsByProductIds(productIds, new DppPageRequest(Integer.toString(offset), limit));
        return new DppIdPage(page.items(), page.nextCursor());
    }

    @Override
    public void appendVersion(Dpp4Fun dpp, Instant occurredAt, String eventType, Map<String, String> eventData) {
        long currentVersion = currentVersionNo(dpp.getDppId());
        try {
            repository.appendVersion(
                    dpp,
                    currentVersion,
                    new PostgresDppOperationContext(null, occurredAt),
                    DppLifecycleEventType.valueOf(eventType),
                    eventData
            );
        } catch (RuntimeException exception) {
            throw mapException(exception, dpp.getDppId());
        }
    }

    @Override
    public void softDelete(String dppId, Instant occurredAt) {
        long currentVersion = currentVersionNo(dppId);
        try {
            repository.softDelete(dppId, currentVersion, occurredAt);
        } catch (RuntimeException exception) {
            throw mapException(exception, dppId);
        }
    }

    @Override
    public List<LifecycleEventRecord> findEventsByDppId(String dppId) {
        return repository.findEventsByDppId(dppId).stream()
                .map(this::toLifecycleEventRecord)
                .toList();
    }

    @Override
    public void clear() {
        try (java.sql.Connection connection = dataSource.getConnection();
             java.sql.Statement statement = connection.createStatement()) {
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
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to clear PostgreSQL mock repo backend", exception);
        }
    }

    private long currentVersionNo(String dppId) {
        List<Dpp4FunVersionSummary> history = repository.findHistoryByDppId(dppId);
        if (history.isEmpty()) {
            throw new RepoApiException(DppStatusCode.ClientErrorResourceNotFound, "DPP_NOT_FOUND", "No DPP found for id " + dppId);
        }
        return history.get(history.size() - 1).versionNo();
    }

    private LifecycleEventRecord toLifecycleEventRecord(DppLifecycleEventRecord eventRecord) {
        return new LifecycleEventRecord(
                eventRecord.eventId(),
                eventRecord.dppId(),
                eventRecord.eventType().name(),
                eventRecord.occurredAt(),
                objectMapper.valueToTree(eventRecord.data())
        );
    }

    private RuntimeException mapException(RuntimeException exception, String dppId) {
        SQLException sqlException = findSqlException(exception);
        if (sqlException != null && "23505".equals(sqlException.getSQLState())) {
            String constraintName = constraintName(sqlException);
            if ("uq_dpp_passports_active_product_id".equals(constraintName)) {
                return new RepoApiException(DppStatusCode.ClientResourceConflict, "PRODUCT_CONFLICT",
                        "An active DPP already exists for the supplied product id");
            }
            return new RepoApiException(DppStatusCode.ClientResourceConflict, "DPP_CONFLICT",
                    "A DPP with id " + dppId + " already exists");
        }
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.contains("Stale expected version")) {
            return new RepoApiException(DppStatusCode.ClientResourceConflict, "DPP_VERSION_CONFLICT",
                    "The DPP was modified concurrently and the requested change could not be applied");
        }
        if (message.contains("No active DPP found")) {
            return new RepoApiException(DppStatusCode.ClientErrorResourceNotFound, "DPP_NOT_FOUND",
                    "No DPP found for id " + dppId);
        }
        return exception;
    }

    private SQLException findSqlException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }

    private String constraintName(SQLException sqlException) {
        if (sqlException instanceof PSQLException pgException && pgException.getServerErrorMessage() != null) {
            return pgException.getServerErrorMessage().getConstraint();
        }
        String message = sqlException.getMessage() == null ? "" : sqlException.getMessage();
        return message.contains("uq_dpp_passports_active_product_id") ? "uq_dpp_passports_active_product_id" : null;
    }
}
