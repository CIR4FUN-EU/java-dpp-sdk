package demo.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.producer.support.RegistryRecordPayload;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Demo-only helper for the mock registry lookup endpoints that are not part of the split upstream registry client.
 */
final class MockRegistryLookupClient {

    private final String registryUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    MockRegistryLookupClient(String registryUrl, ObjectMapper objectMapper) {
        this.registryUrl = stripTrailingSlash(registryUrl);
        this.objectMapper = objectMapper;
    }

    Optional<RegistryRecordPayload> readByRegistryId(String registryId) {
        return read("/registry/dpps/" + registryId);
    }

    Optional<RegistryRecordPayload> readByDppId(String dppId) {
        return read("/registry/dpps/by-dpp-id/" + dppId);
    }

    private Optional<RegistryRecordPayload> read(String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(registryUrl + path))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Mock registry lookup failed with HTTP " + response.statusCode());
            }
            JsonNode payload = objectMapper.readTree(response.body()).path("payload");
            return Optional.of(objectMapper.treeToValue(payload, RegistryRecordPayload.class));
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Mock registry lookup request failed", exception);
        }
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
