package demo.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default mock repository backend that keeps the existing in-memory behavior.
 */
final class InMemoryDppRepoBackend implements DppRepoBackend {

    private final InMemoryDppStore store;
    private final Dpp4FunJsonCodec codec;
    private final ObjectMapper objectMapper;

    InMemoryDppRepoBackend(InMemoryDppStore store, Dpp4FunJsonCodec codec, ObjectMapper objectMapper) {
        this.store = store;
        this.codec = codec;
        this.objectMapper = objectMapper;
    }

    @Override
    public void create(Dpp4Fun dpp, Instant occurredAt) {
        String canonicalJson = codec.toJson(dpp);
        store.create(dpp.getDppId(), dpp.getProductId(), canonicalJson, occurredAt);
        store.appendEvent(dpp.getDppId(), "DPP_CREATED", occurredAt, objectMapper.valueToTree(Map.of("productId", dpp.getProductId())));
    }

    @Override
    public Optional<Dpp4Fun> findCurrentByDppId(String dppId) {
        return store.findActiveByDppId(dppId).map(record -> codec.fromJson(record.dppJson()));
    }

    @Override
    public boolean existsActiveByDppId(String dppId) {
        return store.hasActiveDpp(dppId);
    }

    @Override
    public boolean existsAnyByDppId(String dppId) {
        return store.findAnyByDppId(dppId).isPresent();
    }

    @Override
    public Optional<Dpp4Fun> findCurrentByProductId(String productId) {
        return store.findActiveByProductId(productId).map(record -> codec.fromJson(record.dppJson()));
    }

    @Override
    public Optional<Dpp4Fun> findByDppIdAt(String dppId, Instant timestamp) {
        return store.findVersionByDppIdAndDate(dppId, timestamp).map(record -> codec.fromJson(record.dppJson()));
    }

    @Override
    public DppIdPage findActiveDppIdsByProductIds(List<String> productIds, int offset, int limit) {
        return new DppIdPage(
                store.findDppIdsByProductIds(productIds, offset, limit),
                store.nextCursor(productIds, offset, limit)
        );
    }

    @Override
    public List<String> findAllActiveDppIds() {
        return store.findAllActiveDppIds();
    }

    @Override
    public void appendVersion(Dpp4Fun dpp, Instant occurredAt, String eventType, Map<String, String> eventData) {
        String canonicalJson = codec.toJson(dpp);
        store.update(dpp.getDppId(), canonicalJson, occurredAt);
        store.appendEvent(dpp.getDppId(), eventType, occurredAt, objectMapper.valueToTree(eventData));
    }

    @Override
    public void softDelete(String dppId, Instant occurredAt) {
        StoredDppRecord deleted = store.softDelete(dppId, occurredAt);
        store.appendEvent(dppId, "DPP_DELETED", occurredAt, objectMapper.valueToTree(Map.of("productId", deleted.productId())));
    }

    @Override
    public List<LifecycleEventRecord> findEventsByDppId(String dppId) {
        return store.eventsFor(dppId);
    }

    @Override
    public void clear() {
        store.clear();
    }
}
