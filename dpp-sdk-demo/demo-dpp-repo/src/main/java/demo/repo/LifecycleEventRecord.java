package demo.repo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

record LifecycleEventRecord(
        String eventId,
        String dppId,
        String eventType,
        Instant occurredAt,
        JsonNode data
) {
}
