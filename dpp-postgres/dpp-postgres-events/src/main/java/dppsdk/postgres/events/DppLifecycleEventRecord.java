package dppsdk.postgres.events;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable lifecycle event record returned by the PostgreSQL persistence layer.
 */
public record DppLifecycleEventRecord(
        String eventId,
        String dppId,
        Long versionNo,
        String operationId,
        DppLifecycleEventType eventType,
        Instant occurredAt,
        Map<String, String> data
) {
    public DppLifecycleEventRecord(String eventId, String dppId, DppLifecycleEventType eventType,
                                   Instant occurredAt, Map<String, String> data) {
        this(eventId, dppId, null, null, eventType, occurredAt, data);
    }
    public DppLifecycleEventRecord {
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
