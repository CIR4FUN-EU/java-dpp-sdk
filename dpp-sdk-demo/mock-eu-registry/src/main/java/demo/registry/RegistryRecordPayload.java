package demo.registry;

import java.time.Instant;

record RegistryRecordPayload(
        String registryIdentifier,
        String dppIdentifier,
        String productIdentifier,
        String operatorIdentifier,
        String repoUrl,
        Instant registeredAt,
        Instant lastUpdatedAt
) {
}
