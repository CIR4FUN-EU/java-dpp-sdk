package demo.producer.support;

import java.time.Instant;

/**
 * Demo-local payload for internal mock registry lookup endpoints.
 */
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
