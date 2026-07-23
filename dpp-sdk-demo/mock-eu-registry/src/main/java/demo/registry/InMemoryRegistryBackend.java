package demo.registry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Default mock registry backend that preserves the existing in-memory demo behavior.
 */
final class InMemoryRegistryBackend implements RegistryBackend {

    private final InMemoryRegistryStore store;

    InMemoryRegistryBackend(InMemoryRegistryStore store) {
        this.store = store;
    }

    @Override
    public RegistryRecord create(
            String productIdentifier,
            String dppIdentifier,
            String operatorIdentifier,
            String repoUrl,
            Instant now
    ) {
        return store.create(productIdentifier, dppIdentifier, operatorIdentifier, repoUrl, now);
    }

    @Override
    public Optional<RegistryRecord> findByRegistryId(String registryId) {
        return store.findByRegistryId(registryId);
    }

    @Override
    public Optional<RegistryRecord> findByDppId(String dppId) {
        return store.findByDppId(dppId);
    }

    @Override
    public boolean existsByDppId(String dppId) {
        return store.existsByDppId(dppId);
    }

    @Override
    public List<String> findAllRegisteredDppIds() {
        return store.findAllRegisteredDppIds();
    }

    @Override
    public void seed(
            String registryIdentifier,
            String productIdentifier,
            String dppIdentifier,
            String operatorIdentifier,
            String repoUrl,
            Instant now
    ) {
        store.seed(registryIdentifier, productIdentifier, dppIdentifier, operatorIdentifier, repoUrl, now);
    }

    @Override
    public void clear() {
        store.clear();
    }
}
