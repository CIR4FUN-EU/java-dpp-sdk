package dppsdk.postgres.dpp4fun;

import dppsdk.postgres.core.PostgresDppStatus;
import java.time.Instant;

public record Dpp4FunVersionSummary(
        String dppId,
        String productId,
        long versionNo,
        PostgresDppStatus status,
        Instant validFrom,
        Instant validTo
) {
}
