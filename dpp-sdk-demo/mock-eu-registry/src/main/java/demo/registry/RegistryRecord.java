package demo.registry;

import java.time.Instant;

record RegistryRecord(
        String registryIdentifier,
        String dppIdentifier,
        String productIdentifier,
        String operatorIdentifier,
        String repoUrl,
        Instant registeredAt,
        Instant lastUpdatedAt
) {
}
