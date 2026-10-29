package demo.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dpp.repo.payloads.DppStatusCode;
import dppsdk.postgres.events.DppLifecycleEventRecord;
import dppsdk.postgres.events.DppLifecycleEventType;
import dppsdk.postgres.core.DppPageRequest;
import dppsdk.postgres.core.PostgresDppNotFoundException;
import dppsdk.postgres.core.PostgresDppOperationContext;
import dppsdk.postgres.core.PostgresDppVersionConflictException;
import dppsdk.postgres.dpp4fun.Dpp4FunPostgresRepository;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.dpp4fun.model.Dpp4Fun;
import org.postgresql.util.PSQLException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Optional demo repository backend that delegates durable storage to {@code Dpp4FunPostgresRepository}.
 *
 * <p>The surrounding demo service still owns validation, JSON Merge Patch, and fine-granular element-path logic.</p>
 */
final class PostgresDppRepoBackend implements DppRepoBackend {

    private final Dpp4FunPostgresRepository repository;
    private final ObjectMapper objectMapper;

    PostgresDppRepoBackend(Dpp4FunPostgresRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void create(DigitalProductPassport passport, Instant occurredAt) {
        Dpp4Fun dpp = Dpp4Fun.from(passport);
        try {
            repository.create(dpp, new PostgresDppOperationContext(null, occurredAt));
        } catch (RuntimeException exception) {
            throw mapException(exception, dpp.getDigitalProductPassportId());
        }
    }

    @Override
    public Optional<DigitalProductPassport> findCurrentByDppId(String dppId) {
        return repository.findCurrentByDppId(dppId).map(Dpp4Fun::asDigitalProductPassport);
    }

    @Override
    public Optional<CurrentDppSnapshot> findCurrentSnapshotByDppId(String dppId) {
        return repository.findCurrentPassportSnapshotByDppId(dppId)
                .map(snapshot -> new CurrentDppSnapshot(snapshot.passport(), snapshot.versionNo()));
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
    public Optional<DigitalProductPassport> findCurrentByProductId(String productId) {
        return repository.findCurrentByProductId(productId).map(Dpp4Fun::asDigitalProductPassport);
    }

    @Override
    public Optional<DigitalProductPassport> findByDppIdAt(String dppId, Instant timestamp) {
        return repository.findByDppIdAt(dppId, timestamp).map(Dpp4Fun::asDigitalProductPassport);
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
    public void appendVersion(DigitalProductPassport passport, long expectedCurrentVersion, Instant occurredAt,
                              String eventType, Map<String, String> eventData) {
        Dpp4Fun dpp = Dpp4Fun.from(passport);
        try {
            repository.appendVersion(
                    dpp,
                    expectedCurrentVersion,
                    new PostgresDppOperationContext(null, occurredAt),
                    DppLifecycleEventType.valueOf(eventType),
                    eventData
            );
        } catch (RuntimeException exception) {
            throw mapException(exception, dpp.getDigitalProductPassportId());
        }
    }

    @Override
    public void softDelete(String dppId, long expectedCurrentVersion, Instant occurredAt) {
        try {
            repository.softDelete(dppId, expectedCurrentVersion, occurredAt);
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
