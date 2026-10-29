package dppsdk.postgres.core;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Small operation context passed into PostgreSQL write operations.
 */
public record PostgresDppOperationContext(
        String operationId,
        Instant occurredAt
) {
    public PostgresDppOperationContext {
        occurredAt = (occurredAt == null ? Instant.now() : occurredAt).truncatedTo(ChronoUnit.MICROS);
    }

    public static PostgresDppOperationContext now() {
        return new PostgresDppOperationContext(null, Instant.now());
    }
}
