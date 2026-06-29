package demo.repo;

import dppsdk.dpp4fun.model.Dpp4Fun;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Small mock-local backend seam used to keep HTTP/service behavior stable across memory and PostgreSQL storage.
 *
 * <p>This is not an SDK-wide abstraction.</p>
 */
interface DppRepoBackend {

    void create(Dpp4Fun dpp, Instant occurredAt);

    Optional<Dpp4Fun> findCurrentByDppId(String dppId);

    boolean existsActiveByDppId(String dppId);

    boolean existsAnyByDppId(String dppId);

    Optional<Dpp4Fun> findCurrentByProductId(String productId);

    Optional<Dpp4Fun> findByProductIdAt(String productId, Instant timestamp);

    DppIdPage findActiveDppIdsByProductIds(List<String> productIds, int offset, int limit);

    void appendVersion(Dpp4Fun dpp, Instant occurredAt, String eventType, Map<String, String> eventData);

    void softDelete(String dppId, Instant occurredAt);

    List<LifecycleEventRecord> findEventsByDppId(String dppId);

    void clear();
}
