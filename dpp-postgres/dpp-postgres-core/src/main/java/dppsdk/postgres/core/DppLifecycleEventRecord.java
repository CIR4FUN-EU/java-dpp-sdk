package dppsdk.postgres.core;

import java.time.Instant;
import java.util.Map;

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
