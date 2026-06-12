package dpp.repo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dpp.repo.client.core.DppCodec;
import dpp.repo.client.core.DppValidator;
import dpp.repo.client.internal.HttpSupport;
import dpp.repo.payloads.CreateDppResponse;
import dpp.repo.payloads.DeleteDppResponse;
import dpp.repo.payloads.DppApiResponse;
import dpp.repo.payloads.ReadDppIdsRequest;
import dpp.repo.payloads.ReadDppIdsResponse;
import dpp.repo.payloads.UpdateDataElementRequest;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Java {@link HttpClient}-based implementation of the repository client.
 */
public class HttpDppRepoClient<T> implements DppRepoClient<T> {
    private static final String DPPS_PATH = "/dpps";
    private static final String DPPS_BY_PRODUCT_ID_PATH = "/dppsByProductId/";
    private static final String DPPS_BY_PRODUCT_ID_AND_DATE_PATH = "/dppsByProductIdAndDate/";
    private static final String DPPS_BY_PRODUCT_IDS_PATH = "/dppsByProductIds";

    private final String baseUrl;
    private final DppCodec<T> codec;
    private final DppValidator<T> validator;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpDppRepoClient(
            String baseUrl,
            DppCodec<T> codec,
            DppValidator<T> validator
    ) {
        this(baseUrl, codec, validator, HttpSupport.buildObjectMapper(), HttpSupport.buildHttpClient());
    }

    public HttpDppRepoClient(
            String baseUrl,
            DppCodec<T> codec,
            DppValidator<T> validator,
            ObjectMapper objectMapper
    ) {
        this(baseUrl, codec, validator, objectMapper, HttpSupport.buildHttpClient());
    }

    HttpDppRepoClient(
            String baseUrl,
            DppCodec<T> codec,
            DppValidator<T> validator,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.baseUrl = HttpSupport.normalizeBaseUrl(baseUrl);
        this.codec = Objects.requireNonNull(codec, "codec must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public CreateDppResponse createDpp(T dpp) {
        HttpSupport.validate(validator, dpp);
        String body = HttpSupport.serialize(codec, dpp);
        DppApiResponse<JsonNode> response = sendForApiResponse("POST", DPPS_PATH, Optional.of(body));
        JsonNode payload = response.getPayload();
        HttpSupport.requireTextField(payload, "dppId", "payload.dppId");
        return HttpSupport.deserializeJson(objectMapper, payload, CreateDppResponse.class);
    }

    @Override
    public T readDppById(String dppId) {
        DppApiResponse<JsonNode> response = sendForApiResponse("GET", dppPath(dppId), Optional.empty());
        return decodeDppPayload(response);
    }

    @Override
    public T readDppByProductId(String productId) {
        DppApiResponse<JsonNode> response = sendForApiResponse(
                "GET",
                DPPS_BY_PRODUCT_ID_PATH + HttpSupport.encodePathSegment(productId),
                Optional.empty()
        );
        return decodeDppPayload(response);
    }

    @Override
    public T readDppVersionByProductIdAndDate(String productId, Instant date) {
        String path = DPPS_BY_PRODUCT_ID_AND_DATE_PATH
                + HttpSupport.encodePathSegment(productId)
                + "?date=" + HttpSupport.encodeQueryParam(Objects.requireNonNull(date, "date must not be null").toString());
        DppApiResponse<JsonNode> response = sendForApiResponse("GET", path, Optional.empty());
        return decodeDppPayload(response);
    }

    @Override
    public ReadDppIdsResponse readDppIdsByProductIds(java.util.List<String> productIds, Integer limit, String cursor) {
        ReadDppIdsRequest requestBody = new ReadDppIdsRequest(productIds, limit, cursor);
        String body = HttpSupport.serializeJson(objectMapper, requestBody);
        DppApiResponse<JsonNode> response = sendForApiResponse("POST", DPPS_BY_PRODUCT_IDS_PATH, Optional.of(body));
        JsonNode payload = response.getPayload();
        HttpSupport.requireField(payload, "dppIdentifiers", "payload.dppIdentifiers");
        return HttpSupport.deserializeJson(objectMapper, payload, ReadDppIdsResponse.class);
    }

    @Override
    public T updateDppById(String dppId, JsonNode partialDpp) {
        String body = HttpSupport.serializeJson(objectMapper, Objects.requireNonNull(partialDpp, "partialDpp must not be null"));
        DppApiResponse<JsonNode> response = sendForApiResponse("PATCH", dppPath(dppId), Optional.of(body));
        return decodeDppPayload(response);
    }

    @Override
    public DeleteDppResponse deleteDppById(String dppId) {
        DppApiResponse<JsonNode> response = sendForApiResponse("DELETE", dppPath(dppId), Optional.empty());
        return new DeleteDppResponse(response.getStatusCode(), response.getMessages());
    }

    @Override
    public JsonNode readDataElement(String dppId, String elementPath) {
        DppApiResponse<JsonNode> response = sendForApiResponse("GET", dataElementPath(dppId, elementPath), Optional.empty());
        return HttpSupport.requirePayload(response);
    }

    @Override
    public JsonNode updateDataElement(String dppId, String elementPath, JsonNode payload) {
        String body = HttpSupport.serializeJson(
                objectMapper,
                new UpdateDataElementRequest(Objects.requireNonNull(payload, "payload must not be null"))
        );
        DppApiResponse<JsonNode> response = sendForApiResponse("PATCH", dataElementPath(dppId, elementPath), Optional.of(body));
        return HttpSupport.requirePayload(response);
    }

    private DppApiResponse<JsonNode> sendForApiResponse(String method, String path, Optional<String> body) {
        HttpRequest request = HttpSupport.jsonRequest(HttpSupport.resolve(baseUrl, path), method, body);
        HttpResponse<String> response = HttpSupport.send(httpClient, request);
        HttpSupport.requireSuccess(response);
        DppApiResponse<JsonNode> apiResponse = HttpSupport.parseApiResponse(objectMapper, response.body());
        HttpSupport.requireApiSuccess(apiResponse, response.body());
        return apiResponse;
    }

    private T decodeDppPayload(DppApiResponse<JsonNode> response) {
        JsonNode payload = HttpSupport.requirePayload(response);
        String payloadJson = HttpSupport.payloadToJson(objectMapper, payload);
        return HttpSupport.deserialize(codec, payloadJson);
    }

    private static String dppPath(String dppId) {
        return DPPS_PATH + "/" + HttpSupport.encodePathSegment(dppId);
    }

    private static String dataElementPath(String dppId, String elementPath) {
        return dppPath(dppId) + "/elements/" + HttpSupport.encodePathSegment(elementPath);
    }
}
