package demo.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dpp.repo.payloads.DppStatusCode;
import dppsdk.postgres.core.DppLifecycleEventRecord;
import dppsdk.postgres.core.DppLifecycleEventType;
import dppsdk.postgres.core.DppPageRequest;
import dppsdk.postgres.core.PostgresDppNotFoundException;
import dppsdk.postgres.core.PostgresDppOperationContext;
import dppsdk.postgres.core.PostgresDppVersionConflictException;
import dppsdk.postgres.dpp4fun.Dpp4FunPostgresRepository;
import dppsdk.dpp4fun.model.Dpp4Fun;
import org.postgresql.util.PSQLException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Optional mock repository backend that delegates durable storage to {@code Dpp4FunPostgresRepository}.
 *
 * <p>The surrounding mock service still owns validation, JSON Merge Patch, and fine-granular element-path logic.</p>
 */
final class PostgresDppRepoBackend implements DppRepoBackend {

    private final Dpp4FunPostgresRepository repository;
    private final ObjectMapper objectMapper;

    PostgresDppRepoBackend(Dpp4FunPostgresRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
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
        return repository.existsAnyByDppId(dppId);
    }

    @Override
    public Optional<Dpp4Fun> findCurrentByProductId(String productId) {
        return repository.findCurrentByProductId(productId);
    }

    @Override
    public Optional<Dpp4Fun> findByDppIdAt(String dppId, Instant timestamp) {
        return repository.findByDppIdAt(dppId, timestamp);
    }

    @Override
    public DppIdPage findActiveDppIdsByProductIds(List<String> productIds, int offset, int limit) {
        dppsdk.postgres.core.DppPage<String> page =
                repository.findActiveDppIdsByProductIds(productIds, new DppPageRequest(Integer.toString(offset), limit));
        return new DppIdPage(page.items(), page.nextCursor());
    }

    @Override
    public List<String> findAllActiveDppIds() {
        return repository.findAllActiveDppIds();
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
        repository.clearAll();
    }

    private long currentVersionNo(String dppId) {
        return repository.findCurrentVersionNoByDppId(dppId)
                .orElseThrow(() -> new RepoApiException(DppStatusCode.ClientErrorResourceNotFound,
                        "DPP_NOT_FOUND", "No DPP found for id " + dppId));
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
        if (exception instanceof PostgresDppVersionConflictException) {
            return new RepoApiException(DppStatusCode.ClientResourceConflict, "DPP_VERSION_CONFLICT",
                    "The DPP was modified concurrently and the requested change could not be applied");
        }
        if (exception instanceof PostgresDppNotFoundException) {
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
