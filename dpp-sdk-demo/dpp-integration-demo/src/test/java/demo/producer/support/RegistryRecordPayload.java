package demo.producer.support;

import java.time.Instant;

/** Test-only projection of mock registry lookup data. */
public record RegistryRecordPayload(
        String registryIdentifier,
        String dppIdentifier,
        String productIdentifier,
        String operatorIdentifier,
        String repoUrl,
        Instant registeredAt,
        Instant lastUpdatedAt
) {
}
