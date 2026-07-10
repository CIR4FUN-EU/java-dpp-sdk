package dppsdk.postgres.dpp4fun;

import dppsdk.postgres.core.PostgresDppStatus;
import java.time.Instant;

/**
 * Lightweight version metadata summary for one stored Dpp4Fun passport version.
 */
public record Dpp4FunVersionSummary(
        String dppId,
        String productId,
        long versionNo,
        PostgresDppStatus status,
        Instant validFrom,
        Instant validTo
) {
}
