package demo.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.transport.json.DppJsonCodec;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default demo repository backend that keeps the existing in-memory behavior.
 */
final class InMemoryDppRepoBackend implements DppRepoBackend {

    private final InMemoryDppStore store;
    private final DppJsonCodec codec;
    private final ObjectMapper objectMapper;
    private final boolean eventsEnabled;

    InMemoryDppRepoBackend(InMemoryDppStore store, DppJsonCodec codec, ObjectMapper objectMapper,
                           boolean eventsEnabled) {
        this.store = store;
        this.codec = codec;
        this.objectMapper = objectMapper;
        this.eventsEnabled = eventsEnabled;
    }

    @Override
    public void create(DigitalProductPassport dpp, Instant occurredAt) {
        String canonicalJson = codec.toExpandedJson(dpp);
        String dppId = dpp.getDppHeader().getDigitalProductPassportId();
        String productId = dpp.getDppHeader().getUniqueProductIdentifier();
        store.create(dppId, productId, canonicalJson, occurredAt);
        if (eventsEnabled) store.appendEvent(dppId, "DPP_CREATED", occurredAt, objectMapper.valueToTree(Map.of("productId", productId)));
    }

    @Override
    public Optional<DigitalProductPassport> findCurrentByDppId(String dppId) {
        return store.findActiveByDppId(dppId).map(record -> codec.fromExpandedJson(record.dppJson()));
    }

    @Override
    public Optional<CurrentDppSnapshot> findCurrentSnapshotByDppId(String dppId) {
        return store.findCurrentSnapshotByDppId(dppId)
                .map(snapshot -> new CurrentDppSnapshot(codec.fromExpandedJson(snapshot.record().dppJson()), snapshot.versionNo()));
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
    public Optional<DigitalProductPassport> findCurrentByProductId(String productId) {
        return store.findActiveByProductId(productId).map(record -> codec.fromExpandedJson(record.dppJson()));
    }

    @Override
    public Optional<DigitalProductPassport> findByDppIdAt(String dppId, Instant timestamp) {
        return store.findVersionByDppIdAndDate(dppId, timestamp).map(record -> codec.fromExpandedJson(record.dppJson()));
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
    public void appendVersion(DigitalProductPassport dpp, long expectedCurrentVersion, Instant occurredAt,
                              String eventType, Map<String, String> eventData) {
        String canonicalJson = codec.toExpandedJson(dpp);
        String dppId = dpp.getDppHeader().getDigitalProductPassportId();
        store.update(dppId, canonicalJson, occurredAt, expectedCurrentVersion);
        if (eventsEnabled) store.appendEvent(dppId, eventType, occurredAt, objectMapper.valueToTree(eventData));
    }

    @Override
    public void softDelete(String dppId, long expectedCurrentVersion, Instant occurredAt) {
        StoredDppRecord deleted = store.softDelete(dppId, occurredAt, expectedCurrentVersion);
        if (eventsEnabled) store.appendEvent(dppId, "DPP_DELETED", occurredAt, objectMapper.valueToTree(Map.of("productId", deleted.productId())));
    }

    @Override
    public List<LifecycleEventRecord> findEventsByDppId(String dppId) {
        return store.eventsFor(dppId);
    }

}
