package demo.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HttpServiceDemoRunnerTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("DPP_REPO_PORT");
        System.clearProperty("DPP_REGISTRY_PORT");
    }

    @Test
    @DisplayName("Default Docker and localhost URLs use the configured repo and registry ports")
    void defaultUrlsUseConfiguredPorts() {
        System.setProperty("DPP_REGISTRY_PORT", "18081");
        System.setProperty("DPP_REPO_PORT", "18080");

        HttpServiceDemoRunner configuredRunner = new HttpServiceDemoRunner();

        assertEquals("http://mock-eu-registry:18081", configuredRunner.defaultRegistryDockerUrl());
        assertEquals("http://localhost:18081", configuredRunner.defaultRegistryLocalUrl());
        assertEquals("http://mock-dpp-repo:18080", configuredRunner.defaultRepoDockerUrl());
        assertEquals("http://localhost:18080", configuredRunner.defaultRepoLocalUrl());
    }
}
