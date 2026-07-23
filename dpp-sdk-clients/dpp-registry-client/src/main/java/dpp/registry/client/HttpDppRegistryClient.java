package dpp.registry.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dpp.registry.client.exception.DppMappingClientException;
import dpp.registry.client.internal.HttpSupport;
import dpp.registry.payloads.DppApiResponse;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.registry.payloads.RegisterDppResponse;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.Optional;

/**
 * Java {@link HttpClient}-based implementation of the registry registration client.
 */
public class HttpDppRegistryClient implements DppRegistryClient {
    private static final String REGISTER_PATH = "/v1/registerDPP";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpDppRegistryClient(String baseUrl) {
        this(baseUrl, HttpSupport.buildObjectMapper(), HttpSupport.buildHttpClient());
    }

    public HttpDppRegistryClient(String baseUrl, ObjectMapper objectMapper) {
        this(baseUrl, objectMapper, HttpSupport.buildHttpClient());
    }

    HttpDppRegistryClient(
            String baseUrl,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.baseUrl = HttpSupport.normalizeBaseUrl(baseUrl);
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public RegisterDppResponse postNewDppToRegistry(RegisterDppRequest request) {
        if (request == null) {
            throw new DppMappingClientException(
                    "DPP registry request could not be mapped before request",
                    new IllegalArgumentException("request must not be null")
            );
        }
        String body = HttpSupport.serializeJson(objectMapper, request);
        DppApiResponse<JsonNode> response = sendForApiResponse("POST", REGISTER_PATH, Optional.of(body));
        JsonNode payload = response.getPayload();
        requireRegistrationId(payload);
        return HttpSupport.deserializeJson(objectMapper, payload, RegisterDppResponse.class);
    }

    private static void requireRegistrationId(JsonNode payload) {
        if (payload != null && payload.hasNonNull("registrationId") && payload.get("registrationId").isTextual()) {
            HttpSupport.requireTextField(payload, "registrationId", "payload.registrationId");
            return;
        }
        if (payload != null && payload.hasNonNull("registryIdentifier") && payload.get("registryIdentifier").isTextual()) {
            HttpSupport.requireTextField(payload, "registryIdentifier", "payload.registryIdentifier");
            return;
        }
        HttpSupport.requireTextField(payload, "registrationId", "payload.registrationId");
    }

    private DppApiResponse<JsonNode> sendForApiResponse(String method, String path, Optional<String> body) {
        HttpRequest request = HttpSupport.jsonRequest(HttpSupport.resolve(baseUrl, path), method, body);
        HttpResponse<String> response = HttpSupport.send(httpClient, request);
        HttpSupport.requireSuccess(response);
        DppApiResponse<JsonNode> apiResponse = HttpSupport.parseApiResponse(objectMapper, response.body());
        HttpSupport.requireApiSuccess(apiResponse, response.body());
        return apiResponse;
    }
}
