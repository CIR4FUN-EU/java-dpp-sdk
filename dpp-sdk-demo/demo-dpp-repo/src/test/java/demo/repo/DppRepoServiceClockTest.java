package demo.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import demo.repo.testsupport.DemoDppFactory;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.transport.json.DppJsonCodec;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DppRepoServiceClockTest {

    @Test
    void createUpdateAndDeleteUseInjectedClock() throws Exception {
        Instant fixedNow = Instant.parse("2026-06-30T12:00:00Z");
        RecordingBackend backend = new RecordingBackend();
        DppJsonCodec codec = new DppJsonCodec();
        ObjectMapper objectMapper = new ObjectMapper();
        DppRepoService service = new DppRepoService(
                backend,
                codec,
                new RepositoryProfiles("dpp4fun", List.of(new CoreRepositoryProfile(), new Dpp4FunRepositoryProfile())),
                true,
                objectMapper,
                new DppIdentifierExtractor(),
                new DppMergePatchService(),
                new DppElementPathService(),
                Clock.fixed(fixedNow, ZoneOffset.UTC)
        );

        Dpp4Fun dpp = new DemoDppFactory().createValidBedDpp();
        backend.current = dpp.asDigitalProductPassport();

        service.create(codec.toExpandedJson(dpp.asDigitalProductPassport()));
        Dpp4Fun updated = new DemoDppFactory().withBrand(dpp, "Clock controlled update");
        ObjectNode patch = objectMapper.createObjectNode();
        patch.set("elements", objectMapper.readTree(codec.toExpandedJson(updated.asDigitalProductPassport())).get("elements"));
        service.updateById(dpp.getDigitalProductPassportId(), objectMapper.writeValueAsString(patch));
        service.deleteById(dpp.getDigitalProductPassportId());

        assertEquals(fixedNow, backend.createOccurredAt);
        assertEquals(fixedNow, backend.updateOccurredAt);
        assertEquals(fixedNow, backend.deleteOccurredAt);
    }

    private static final class RecordingBackend implements DppRepoBackend {
        private DigitalProductPassport current;
        private long versionNo;
        private Instant createOccurredAt;
        private Instant updateOccurredAt;
        private Instant deleteOccurredAt;

        @Override
        public void create(DigitalProductPassport dpp, Instant occurredAt) {
            current = dpp;
            versionNo = 1;
            createOccurredAt = occurredAt;
        }

        @Override
        public Optional<DigitalProductPassport> findCurrentByDppId(String dppId) {
            return Optional.ofNullable(current);
        }

        @Override
        public Optional<CurrentDppSnapshot> findCurrentSnapshotByDppId(String dppId) {
            return current == null ? Optional.empty() : Optional.of(new CurrentDppSnapshot(current, versionNo));
        }

        @Override
        public boolean existsActiveByDppId(String dppId) {
            return current != null;
        }

        @Override
        public boolean existsAnyByDppId(String dppId) {
            return current != null;
        }

        @Override
        public Optional<DigitalProductPassport> findCurrentByProductId(String productId) {
            return Optional.ofNullable(current);
        }

        @Override
        public Optional<DigitalProductPassport> findByDppIdAt(String dppId, Instant timestamp) {
            return Optional.ofNullable(current);
        }

        @Override
        public DppIdPage findActiveDppIdsByProductIds(List<String> productIds, int offset, int limit) {
            return new DppIdPage(List.of(), null);
        }

        @Override
        public List<String> findAllActiveDppIds() {
            return current == null ? List.of() : List.of(current.getDppHeader().getDigitalProductPassportId());
        }

        @Override
        public void appendVersion(DigitalProductPassport dpp, long expectedCurrentVersion, Instant occurredAt,
                                  String eventType, Map<String, String> eventData) {
            current = dpp;
            versionNo++;
            updateOccurredAt = occurredAt;
        }

        @Override
        public void softDelete(String dppId, long expectedCurrentVersion, Instant occurredAt) {
            deleteOccurredAt = occurredAt;
        }

        @Override
        public List<LifecycleEventRecord> findEventsByDppId(String dppId) {
            return List.of();
        }

    }
}
