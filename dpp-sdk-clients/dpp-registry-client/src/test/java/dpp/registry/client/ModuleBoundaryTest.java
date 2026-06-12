package dpp.registry.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ModuleBoundaryTest {
    @Test
    void repoAndRegistryModulesDoNotDependOnEachOther() throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize();

        String repoClientPom = Files.readString(root.resolve("dpp-repo-client/pom.xml"));
        String registryClientPom = Files.readString(root.resolve("dpp-registry-client/pom.xml"));
        String repoPayloadPom = Files.readString(root.resolve("dpp-repo-payloads/pom.xml"));
        String registryPayloadPom = Files.readString(root.resolve("dpp-registry-payloads/pom.xml"));

        assertFalse(repoClientPom.contains("<artifactId>dpp-registry-"));
        assertFalse(registryClientPom.contains("<artifactId>dpp-repo-"));
        assertFalse(repoPayloadPom.contains("<artifactId>dpp-registry-"));
        assertFalse(registryPayloadPom.contains("<artifactId>dpp-repo-"));
        assertFalse(repoPayloadPom.contains("<artifactId>dpp-repo-client</artifactId>"));
        assertFalse(registryPayloadPom.contains("<artifactId>dpp-registry-client</artifactId>"));
    }
}
