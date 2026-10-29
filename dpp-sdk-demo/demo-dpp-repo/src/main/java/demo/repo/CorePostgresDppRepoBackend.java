package demo.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dpp.repo.payloads.DppStatusCode;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.postgres.core.PostgresDppNotFoundException;
import dppsdk.postgres.core.PostgresDppOperationContext;
import dppsdk.postgres.core.PostgresDppRepository;
import dppsdk.postgres.core.PostgresDppVersionConflictException;
import dppsdk.postgres.events.DppLifecycleEventType;
import dppsdk.postgres.events.PostgresDppRepositoryWithEvents;
import org.postgresql.util.PSQLException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class CorePostgresDppRepoBackend implements DppRepoBackend {
    private final PostgresDppRepository repository;
    private final PostgresDppRepositoryWithEvents eventRepository;
    private final ObjectMapper objectMapper;

    CorePostgresDppRepoBackend(PostgresDppRepository repository,
                               PostgresDppRepositoryWithEvents eventRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Override public void create(DigitalProductPassport dpp, Instant at) {
        try {
            if (eventRepository == null) repository.create(dpp, new PostgresDppOperationContext(null, at));
            else eventRepository.create(dpp, new PostgresDppOperationContext(null, at));
        } catch (RuntimeException exception) { throw mapException(exception, dpp.getDppHeader().getDigitalProductPassportId()); }
    }

    @Override public Optional<DigitalProductPassport> findCurrentByDppId(String id) { return repository.findCurrentByDppId(id); }
    @Override public Optional<CurrentDppSnapshot> findCurrentSnapshotByDppId(String id) {
        return repository.findCurrentSnapshotByDppId(id)
                .map(snapshot -> new CurrentDppSnapshot(snapshot.passport(), snapshot.versionNo()));
    }
    @Override public boolean existsActiveByDppId(String id) { return repository.existsActiveByDppId(id); }
    @Override public boolean existsAnyByDppId(String id) { return repository.existsAnyByDppId(id); }
    @Override public Optional<DigitalProductPassport> findCurrentByProductId(String id) { return repository.findCurrentByProductId(id); }
    @Override public Optional<DigitalProductPassport> findByDppIdAt(String id, Instant at) { return repository.findByDppIdAt(id, at); }
    @Override public DppIdPage findActiveDppIdsByProductIds(List<String> ids, int offset, int limit) {
        var page = repository.findActiveDppIdsByProductIds(ids,
                new dppsdk.postgres.core.DppPageRequest(offset == 0 ? null : Integer.toString(offset), limit));
        return new DppIdPage(page.items(), page.nextCursor());
    }
    @Override public List<String> findAllActiveDppIds() { return repository.findAllActiveDppIds(); }
    @Override public void appendVersion(DigitalProductPassport dpp, long expected, Instant at, String type,
                                        Map<String, String> data) {
        try {
            var context = new PostgresDppOperationContext(null, at);
            if (eventRepository == null) repository.appendVersion(dpp, expected, context);
            else eventRepository.appendVersion(dpp, expected, context, DppLifecycleEventType.valueOf(type), data);
        } catch (RuntimeException exception) { throw mapException(exception, dpp.getDppHeader().getDigitalProductPassportId()); }
    }
    @Override public void softDelete(String id, long expected, Instant at) {
        try {
            if (eventRepository == null) repository.softDelete(id, expected, at);
            else eventRepository.softDelete(id, expected, new PostgresDppOperationContext(null, at));
        } catch (RuntimeException exception) { throw mapException(exception, id); }
    }
    @Override public List<LifecycleEventRecord> findEventsByDppId(String id) {
        if (eventRepository == null) return List.of();
        return eventRepository.findEventsByDppId(id).stream().map(event -> new LifecycleEventRecord(
                event.eventId(), event.dppId(), event.eventType().name(), event.occurredAt(),
                objectMapper.valueToTree(event.data()))).toList();
    }

    private RuntimeException mapException(RuntimeException exception, String dppId) {
        SQLException sql = findSqlException(exception);
        if (sql != null && "23505".equals(sql.getSQLState())) {
            String name = constraintName(sql);
            return new RepoApiException(DppStatusCode.ClientResourceConflict,
                    "uq_dpp_passports_active_product_id".equals(name) ? "PRODUCT_CONFLICT" : "DPP_CONFLICT",
                    "uq_dpp_passports_active_product_id".equals(name)
                            ? "An active DPP already exists for the supplied product id"
                            : "A DPP with id " + dppId + " already exists");
        }
        if (exception instanceof PostgresDppVersionConflictException) return new RepoApiException(
                DppStatusCode.ClientResourceConflict, "DPP_VERSION_CONFLICT", "The DPP was modified concurrently and the requested change could not be applied");
        if (exception instanceof PostgresDppNotFoundException) return new RepoApiException(
                DppStatusCode.ClientErrorResourceNotFound, "DPP_NOT_FOUND", "No DPP found for id " + dppId);
        return exception;
    }
    private SQLException findSqlException(Throwable value) {
        for (Throwable current = value; current != null; current = current.getCause()) if (current instanceof SQLException sql) return sql;
        return null;
    }
    private String constraintName(SQLException sql) {
        if (sql instanceof PSQLException pg && pg.getServerErrorMessage() != null) return pg.getServerErrorMessage().getConstraint();
        return sql.getMessage() != null && sql.getMessage().contains("uq_dpp_passports_active_product_id")
                ? "uq_dpp_passports_active_product_id" : null;
    }
}
