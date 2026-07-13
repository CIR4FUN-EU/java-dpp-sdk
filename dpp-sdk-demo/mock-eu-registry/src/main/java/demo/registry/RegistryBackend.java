package demo.registry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Local mock-registry storage seam used only inside {@code mock-eu-registry}.
 *
 * <p>This is not an SDK-wide persistence API.</p>
 */
interface RegistryBackend {

    RegistryRecord create(
            String productIdentifier,
            String dppIdentifier,
            String operatorIdentifier,
            String repoUrl,
            Instant now
    );

    Optional<RegistryRecord> findByRegistryId(String registryId);

    Optional<RegistryRecord> findByDppId(String dppId);

    boolean existsByDppId(String dppId);

    List<String> findAllRegisteredDppIds();

    void seed(
            String registryIdentifier,
            String productIdentifier,
            String dppIdentifier,
            String operatorIdentifier,
            String repoUrl,
            Instant now
    );

    void clear();
}
