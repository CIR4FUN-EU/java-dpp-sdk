package dppsdk.postgres.core;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable lifecycle event record returned by the PostgreSQL persistence layer.
 */
public record DppLifecycleEventRecord(
        String eventId,
        String dppId,
        DppLifecycleEventType eventType,
        Instant occurredAt,
        Map<String, String> data
) {
    public DppLifecycleEventRecord {
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
