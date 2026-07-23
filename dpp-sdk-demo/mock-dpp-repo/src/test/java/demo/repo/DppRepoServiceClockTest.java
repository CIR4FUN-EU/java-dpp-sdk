package demo.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.repo.testsupport.DemoDppFactory;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
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
    void createUpdateAndDeleteUseInjectedClock() {
        Instant fixedNow = Instant.parse("2026-06-30T12:00:00Z");
        RecordingBackend backend = new RecordingBackend();
        Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();
        DppRepoService service = new DppRepoService(
                backend,
                codec,
                new Dpp4FunValidationService(),
                new ObjectMapper(),
                new DppIdentifierExtractor(),
                new DppMergePatchService(),
                new DppElementPathService(),
                new CompressedDppMapper(),
                Clock.fixed(fixedNow, ZoneOffset.UTC)
        );

        Dpp4Fun dpp = new DemoDppFactory().createValidBedDpp();
        backend.current = dpp;

        service.create(codec.toJson(dpp));
        service.updateById(dpp.getDppId(), """
                {
                  "characteristics": {
                    "productName": "Clock controlled update"
                  }
                }
                """);
        service.deleteById(dpp.getDppId());

        assertEquals(fixedNow, backend.createOccurredAt);
        assertEquals(fixedNow, backend.updateOccurredAt);
        assertEquals(fixedNow, backend.deleteOccurredAt);
    }

    private static final class RecordingBackend implements DppRepoBackend {
        private Dpp4Fun current;
        private Instant createOccurredAt;
        private Instant updateOccurredAt;
        private Instant deleteOccurredAt;

        @Override
        public void create(Dpp4Fun dpp, Instant occurredAt) {
            current = dpp;
            createOccurredAt = occurredAt;
        }

        @Override
        public Optional<Dpp4Fun> findCurrentByDppId(String dppId) {
            return Optional.ofNullable(current);
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
        public Optional<Dpp4Fun> findCurrentByProductId(String productId) {
            return Optional.ofNullable(current);
        }

        @Override
        public Optional<Dpp4Fun> findByDppIdAt(String dppId, Instant timestamp) {
            return Optional.ofNullable(current);
        }

        @Override
        public DppIdPage findActiveDppIdsByProductIds(List<String> productIds, int offset, int limit) {
            return new DppIdPage(List.of(), null);
        }

        @Override
        public List<String> findAllActiveDppIds() {
            return current == null ? List.of() : List.of(current.getDppId());
        }

        @Override
        public void appendVersion(Dpp4Fun dpp, Instant occurredAt, String eventType, Map<String, String> eventData) {
            current = dpp;
            updateOccurredAt = occurredAt;
        }

        @Override
        public void softDelete(String dppId, Instant occurredAt) {
            deleteOccurredAt = occurredAt;
        }

        @Override
        public List<LifecycleEventRecord> findEventsByDppId(String dppId) {
            return List.of();
        }

        @Override
        public void clear() {
            current = null;
        }
    }
}
