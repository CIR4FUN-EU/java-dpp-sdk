package demo.repo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * Thread-safe in-memory storage for the mock repository service.
 *
 * <p>This store is intentionally simple and non-persistent. It keeps the active DPP view, historical
 * snapshots by product, and basic lifecycle events needed by the internal demo APIs.</p>
 */
@Component
class InMemoryDppStore {

    private final ConcurrentMap<String, StoredDppRecord> dppsByDppId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> activeDppIdByProductId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<DppVersionRecord>> versionsByProductId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<DppVersionRecord>> versionsByDppId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<LifecycleEventRecord>> eventsByDppId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<Instant>> deletionsByProductId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<Instant>> deletionsByDppId = new ConcurrentHashMap<>();

    synchronized void create(String dppId, String productId, String dppJson, Instant now) {
        if (dppsByDppId.containsKey(dppId)) {
            throw new RepoApiException(dpp.repo.payloads.DppStatusCode.ClientResourceConflict, "DPP_CONFLICT",
                    "A DPP with id " + dppId + " already exists");
        }
        String activeDppId = activeDppIdByProductId.get(productId);
        if (activeDppId != null && !activeDppId.equals(dppId)) {
            throw new RepoApiException(dpp.repo.payloads.DppStatusCode.ClientResourceConflict, "PRODUCT_CONFLICT",
                    "An active DPP already exists for product id " + productId);
        }

        dppsByDppId.put(dppId, new StoredDppRecord(dppId, productId, dppJson, now, now, null, false));
        activeDppIdByProductId.put(productId, dppId);
        appendVersion(dppId, productId, dppJson, now);
    }

    synchronized StoredDppRecord update(String dppId, String updatedJson, Instant now) {
        StoredDppRecord existing = requireActiveRecord(dppId);
        StoredDppRecord updated = existing.withUpdatedJson(updatedJson, now);
        dppsByDppId.put(dppId, updated);
        // Version history is append-only so snapshot reads can answer "state at time T" queries.
        appendVersion(dppId, existing.productId(), updatedJson, now);
        return updated;
    }

    synchronized StoredDppRecord softDelete(String dppId, Instant now) {
        StoredDppRecord existing = requireActiveRecord(dppId);
        activeDppIdByProductId.remove(existing.productId(), dppId);
        StoredDppRecord deleted = existing.withDeleted(now);
        dppsByDppId.put(dppId, deleted);
        // Product-level deletion timestamps let version-by-date distinguish "no version yet" from "was deleted".
        deletionsByProductId.computeIfAbsent(existing.productId(), ignored -> new CopyOnWriteArrayList<>()).add(now);
        deletionsByDppId.computeIfAbsent(dppId, ignored -> new CopyOnWriteArrayList<>()).add(now);
        return deleted;
    }

    synchronized void appendEvent(String dppId, String eventType, Instant occurredAt, com.fasterxml.jackson.databind.JsonNode data) {
        eventsByDppId.computeIfAbsent(dppId, ignored -> new CopyOnWriteArrayList<>())
                .add(new LifecycleEventRecord(UUID.randomUUID().toString(), dppId, eventType, occurredAt, data));
    }

    Optional<StoredDppRecord> findActiveByDppId(String dppId) {
        StoredDppRecord record = dppsByDppId.get(dppId);
        return record == null || record.deleted() ? Optional.empty() : Optional.of(record);
    }

    boolean hasActiveDpp(String dppId) {
        return findActiveByDppId(dppId).isPresent();
    }

    Optional<StoredDppRecord> findAnyByDppId(String dppId) {
        return Optional.ofNullable(dppsByDppId.get(dppId));
    }

    Optional<StoredDppRecord> findActiveByProductId(String productId) {
        String dppId = activeDppIdByProductId.get(productId);
        return dppId == null ? Optional.empty() : findActiveByDppId(dppId);
    }

    Optional<DppVersionRecord> findVersionByProductIdAndDate(String productId, Instant at) {
        CopyOnWriteArrayList<DppVersionRecord> versions = versionsByProductId.get(productId);
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }
        Optional<DppVersionRecord> candidate = versions.stream()
                .filter(version -> !version.validFrom().isAfter(at))
                .max(Comparator.comparing(DppVersionRecord::validFrom));
        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        Instant candidateStart = candidate.get().validFrom();
        // A snapshot is only valid if the product was not deleted between that snapshot and the requested time.
        boolean deletedBeforeRequestedTime = deletionsByProductId.getOrDefault(productId, new CopyOnWriteArrayList<>()).stream()
                .anyMatch(deletedAt -> deletedAt.isAfter(candidateStart) && !deletedAt.isAfter(at));
        return deletedBeforeRequestedTime ? Optional.empty() : candidate;
    }

    Optional<DppVersionRecord> findVersionByDppIdAndDate(String dppId, Instant at) {
        CopyOnWriteArrayList<DppVersionRecord> versions = versionsByDppId.get(dppId);
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }
        Optional<DppVersionRecord> candidate = versions.stream()
                .filter(version -> !version.validFrom().isAfter(at))
                .max(Comparator.comparing(DppVersionRecord::validFrom));
        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        Instant candidateStart = candidate.get().validFrom();
        boolean deletedBeforeRequestedTime = deletionsByDppId.getOrDefault(dppId, new CopyOnWriteArrayList<>()).stream()
                .anyMatch(deletedAt -> deletedAt.isAfter(candidateStart) && !deletedAt.isAfter(at));
        return deletedBeforeRequestedTime ? Optional.empty() : candidate;
    }

    List<String> findDppIdsByProductIds(List<String> productIds, int offset, int limit) {
        List<String> result = new ArrayList<>();
        for (int index = offset; index < productIds.size() && result.size() < limit; index++) {
            String productId = productIds.get(index);
            String dppId = activeDppIdByProductId.get(productId);
            if (dppId != null) {
                result.add(dppId);
            }
        }
        return result;
    }

    String nextCursor(List<String> productIds, int offset, int limit) {
        int next = Math.min(productIds.size(), offset + limit);
        return next >= productIds.size() ? null : Integer.toString(next);
    }

    List<LifecycleEventRecord> eventsFor(String dppId) {
        CopyOnWriteArrayList<LifecycleEventRecord> events = eventsByDppId.get(dppId);
        return events == null ? List.of() : List.copyOf(events);
    }

    List<DppVersionRecord> versionsForProduct(String productId) {
        CopyOnWriteArrayList<DppVersionRecord> versions = versionsByProductId.get(productId);
        return versions == null ? List.of() : List.copyOf(versions);
    }

    List<String> findAllActiveDppIds() {
        return dppsByDppId.values().stream()
                .filter(record -> !record.deleted())
                .map(StoredDppRecord::dppId)
                .sorted()
                .toList();
    }

    void clear() {
        dppsByDppId.clear();
        activeDppIdByProductId.clear();
        versionsByProductId.clear();
        versionsByDppId.clear();
        eventsByDppId.clear();
        deletionsByProductId.clear();
        deletionsByDppId.clear();
    }

    private StoredDppRecord requireActiveRecord(String dppId) {
        return findActiveByDppId(dppId)
                .orElseThrow(() -> new RepoApiException(dpp.repo.payloads.DppStatusCode.ClientErrorResourceNotFound,
                        "DPP_NOT_FOUND", "No DPP found for id " + dppId));
    }

    private void appendVersion(String dppId, String productId, String dppJson, Instant now) {
        versionsByProductId.computeIfAbsent(productId, ignored -> new CopyOnWriteArrayList<>())
                .add(new DppVersionRecord(UUID.randomUUID().toString(), dppId, productId, dppJson, now, now));
        versionsByDppId.computeIfAbsent(dppId, ignored -> new CopyOnWriteArrayList<>())
                .add(new DppVersionRecord(UUID.randomUUID().toString(), dppId, productId, dppJson, now, now));
    }
}
