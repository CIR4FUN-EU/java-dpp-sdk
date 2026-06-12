package demo.registry;

import dpp.registry.payloads.DppStatusCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * In-memory registry metadata store for internal mock use.
 *
 * <p>This store keeps only registry metadata keyed by registry ID and DPP ID. It is not a persistence
 * layer and does not hold full DPP documents.</p>
 */
@Component
class InMemoryRegistryStore {

    private final ConcurrentMap<String, RegistryRecord> recordsByRegistryId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> registryIdByDppId = new ConcurrentHashMap<>();

    synchronized RegistryRecord create(
            String productIdentifier,
            String dppIdentifier,
            String operatorIdentifier,
            String repoUrl,
            Instant now
    ) {
        if (registryIdByDppId.containsKey(dppIdentifier)) {
            throw new RegistryApiException(DppStatusCode.ClientResourceConflict, "REGISTRY_CONFLICT",
                    "A registry record already exists for dpp id " + dppIdentifier);
        }
        String registryId = UUID.randomUUID().toString();
        RegistryRecord record = new RegistryRecord(
                registryId,
                dppIdentifier,
                productIdentifier,
                operatorIdentifier,
                repoUrl,
                now,
                now
        );
        recordsByRegistryId.put(registryId, record);
        registryIdByDppId.put(dppIdentifier, registryId);
        return record;
    }

    Optional<RegistryRecord> findByRegistryId(String registryId) {
        return Optional.ofNullable(recordsByRegistryId.get(registryId));
    }

    Optional<RegistryRecord> findByDppId(String dppId) {
        String registryId = registryIdByDppId.get(dppId);
        return registryId == null ? Optional.empty() : findByRegistryId(registryId);
    }

    boolean existsByDppId(String dppId) {
        return registryIdByDppId.containsKey(dppId);
    }

    synchronized void seed(
            String registryIdentifier,
            String productIdentifier,
            String dppIdentifier,
            String operatorIdentifier,
            String repoUrl,
            Instant now
    ) {
        if (recordsByRegistryId.containsKey(registryIdentifier) || registryIdByDppId.containsKey(dppIdentifier)) {
            return;
        }
        RegistryRecord record = new RegistryRecord(
                registryIdentifier,
                dppIdentifier,
                productIdentifier,
                operatorIdentifier,
                repoUrl,
                now,
                now
        );
        recordsByRegistryId.put(registryIdentifier, record);
        registryIdByDppId.put(dppIdentifier, registryIdentifier);
    }

    void clear() {
        recordsByRegistryId.clear();
        registryIdByDppId.clear();
    }
}
