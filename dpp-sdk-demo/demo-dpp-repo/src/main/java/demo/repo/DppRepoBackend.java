package demo.repo;

import dppsdk.core.model.DigitalProductPassport;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Small demo-local backend seam used to keep HTTP/service behavior stable across memory and PostgreSQL storage.
 *
 * <p>This is not an SDK-wide abstraction.</p>
 */
interface DppRepoBackend {

    void create(DigitalProductPassport dpp, Instant occurredAt);

    Optional<DigitalProductPassport> findCurrentByDppId(String dppId);

    Optional<CurrentDppSnapshot> findCurrentSnapshotByDppId(String dppId);

    boolean existsActiveByDppId(String dppId);

    boolean existsAnyByDppId(String dppId);

    Optional<DigitalProductPassport> findCurrentByProductId(String productId);

    Optional<DigitalProductPassport> findByDppIdAt(String dppId, Instant timestamp);

    DppIdPage findActiveDppIdsByProductIds(List<String> productIds, int offset, int limit);

    List<String> findAllActiveDppIds();

    void appendVersion(DigitalProductPassport dpp, long expectedCurrentVersion, Instant occurredAt, String eventType,
                        Map<String, String> eventData);

    void softDelete(String dppId, long expectedCurrentVersion, Instant occurredAt);

    List<LifecycleEventRecord> findEventsByDppId(String dppId);

}
