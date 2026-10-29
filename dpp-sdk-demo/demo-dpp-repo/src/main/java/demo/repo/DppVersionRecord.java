package demo.repo;

import java.time.Instant;

record DppVersionRecord(
        String versionId,
        String dppId,
        String productId,
        String dppJson,
        Instant validFrom,
        Instant createdAt
) {
}
