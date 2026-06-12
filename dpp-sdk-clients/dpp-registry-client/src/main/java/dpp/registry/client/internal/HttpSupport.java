package dpp.registry.client.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dpp.registry.client.exception.DppApiClientException;
import dpp.registry.client.exception.DppHttpClientException;
import dpp.registry.client.exception.DppMappingClientException;
import dpp.registry.client.exception.DppNetworkClientException;
import dpp.registry.payloads.DppApiMessage;
import dpp.registry.payloads.DppApiResponse;
import dpp.registry.payloads.DppStatusCode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry transport and JSON helpers.
 */
public final class HttpSupport {
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private HttpSupport() {
    }

    public static HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .build();
    }

    public static ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return objectMapper;
    }

    public static String normalizeBaseUrl(String baseUrl) {
        String trimmed = Objects.requireNonNull(baseUrl, "baseUrl must not be null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        URI.create(trimmed);
        return trimmed;
    }

    public static URI resolve(String baseUrl, String pathAndQuery) {
        String normalized = pathAndQuery == null || pathAndQuery.isBlank() ? "/" : pathAndQuery;
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return URI.create(baseUrl + normalized);
    }

    public static String serializeJson(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new DppMappingClientException("Client JSON serialization failed before request", ex);
        }
    }

    public static <T> T deserializeJson(ObjectMapper objectMapper, JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (JsonProcessingException ex) {
            throw new DppMappingClientException("Client JSON mapping failed after response", ex);
        }
    }

    public static DppMappingClientException missingRequiredResponseField(String fieldPath) {
        return new DppMappingClientException(
                "Missing required response field: " + fieldPath,
                new IllegalArgumentException("Missing required response field: " + fieldPath)
        );
    }

    public static String requireTextField(JsonNode node, String fieldName, String fieldPath) {
        if (node == null || node.isNull()) {
            throw missingRequiredResponseField(fieldPath);
        }
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull() || !field.isTextual() || field.textValue().isBlank()) {
            throw missingRequiredResponseField(fieldPath);
        }
        return field.textValue();
    }

    public static HttpRequest jsonRequest(URI uri, String method, Optional<String> body) {
        HttpRequest.BodyPublisher publisher = body
                .map(HttpRequest.BodyPublishers::ofString)
                .orElseGet(HttpRequest.BodyPublishers::noBody);

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(DEFAULT_REQUEST_TIMEOUT)
                .method(method, publisher)
                .header("Accept", "application/json");

        body.ifPresent(ignored -> builder.header("Content-Type", "application/json"));
        return builder.build();
    }

    public static HttpResponse<String> send(HttpClient httpClient, HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DppNetworkClientException("DPP HTTP request was interrupted", ex);
        } catch (IOException ex) {
            throw new DppNetworkClientException("DPP HTTP request could not complete", ex);
        }
    }

    public static void requireSuccess(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new DppHttpClientException("DPP endpoint returned non-success HTTP status " + status, status, response.body());
        }
    }

    public static DppApiResponse<JsonNode> parseApiResponse(ObjectMapper objectMapper, String rawBody) {
        try {
            return objectMapper.readValue(
                    rawBody,
                    objectMapper.getTypeFactory().constructParametricType(DppApiResponse.class, JsonNode.class)
            );
        } catch (JsonProcessingException ex) {
            throw new DppMappingClientException("DPP API response could not be mapped", ex);
        }
    }

    public static void requireApiSuccess(DppApiResponse<?> response, String rawBody) {
        DppStatusCode statusCode = response.getStatusCode();
        if (statusCode == null) {
            throw new DppMappingClientException(
                    "DPP API response could not be mapped",
                    new IllegalArgumentException("Missing statusCode")
            );
        }
        if (!statusCode.isSuccess()) {
            List<DppApiMessage> messages = response.getMessages();
            throw new DppApiClientException(
                    "DPP API returned error status " + statusCode,
                    statusCode,
                    messages,
                    rawBody
            );
        }
    }
}
