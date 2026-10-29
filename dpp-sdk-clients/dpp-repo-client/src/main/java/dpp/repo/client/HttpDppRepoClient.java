package dpp.repo.client;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dpp.repo.client.internal.HttpSupport;
import dpp.repo.payloads.CreateDppResponse;
import dpp.repo.payloads.DeleteDppResponse;
import dpp.repo.payloads.DppApiResponse;
import dpp.repo.payloads.DppRepresentation;
import dpp.repo.payloads.ReadDppIdsRequest;
import dpp.repo.payloads.ReadDppIdsResponse;

/**
 * Java {@link HttpClient}-based implementation of the repository client.
 */
public class HttpDppRepoClient implements DppRepoClient {
    private static final String API_PREFIX = "/v1";
    private static final String DPPS_PATH = API_PREFIX + "/dpps";
    private static final String DPPS_BY_PRODUCT_ID_PATH = API_PREFIX + "/dppsByProductId/";
    private static final String DPPS_BY_ID_AND_DATE_PATH = API_PREFIX + "/dppsByIdAndDate/";
    private static final String DPPS_BY_PRODUCT_IDS_PATH = API_PREFIX + "/dppsByProductIds";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpDppRepoClient(String baseUrl) {
        this.baseUrl = HttpSupport.normalizeBaseUrl(baseUrl);
        this.objectMapper = HttpSupport.buildObjectMapper();
        this.httpClient = HttpSupport.buildHttpClient();
    }

    @Override
    public CreateDppResponse createDpp(JsonNode dpp) {
        return createDpp(dpp, DppRepresentation.COMPRESSED);
    }

    @Override
    public CreateDppResponse createDpp(JsonNode dpp, DppRepresentation representation) {
        DppRepresentation selected = selected(representation);
        String body = HttpSupport.serializeJson(objectMapper, Objects.requireNonNull(dpp, "dpp must not be null"));
        DppApiResponse<JsonNode> response = sendForApiResponse("POST", DPPS_PATH + selected.querySuffix(), Optional.of(body));
        JsonNode payload = response.getPayload();
        CreateDppResponse result = new CreateDppResponse();
        result.setStatusCode(response.getStatusCode());
        result.setMessage(response.getMessage());
        if (payload == null || payload.isNull()) return result;
        if (!payload.isObject()) throw HttpSupport.missingRequiredResponseField("payload");
        if (payload.hasNonNull("digitalProductPassportId")) {
            HttpSupport.requireTextField(payload, "digitalProductPassportId", "payload.digitalProductPassportId");
        } else if (payload.hasNonNull("dppId")) {
            HttpSupport.requireTextField(payload, "dppId", "payload.dppId");
        }
        CreateDppResponse payloadResult = HttpSupport.deserializeJson(objectMapper, payload, CreateDppResponse.class);
        result.setDigitalProductPassportId(payloadResult.getDigitalProductPassportId());
        return result;
    }

    @Override
    public DppApiResponse<JsonNode> readDppById(String dppId, DppRepresentation representation) {
        return optionalDppResponse("GET", dppPath(dppId) + selected(representation).querySuffix(), Optional.empty());
    }

    @Override
    public DppApiResponse<JsonNode> readDppByProductId(String productId, DppRepresentation representation) {
        return optionalDppResponse("GET", DPPS_BY_PRODUCT_ID_PATH + HttpSupport.encodePathSegment(productId)
                + selected(representation).querySuffix(), Optional.empty());
    }

    @Override
    public DppApiResponse<JsonNode> readDppVersionByIdAndDate(String dppId, Instant date, DppRepresentation representation) {
        String path = DPPS_BY_ID_AND_DATE_PATH + HttpSupport.encodePathSegment(dppId)
                + "?date=" + HttpSupport.encodeQueryParam(Objects.requireNonNull(date, "date").toString());
        if (selected(representation) == DppRepresentation.FULL) path += "&representation=full";
        return optionalDppResponse("GET", path, Optional.empty());
    }

    @Override
    public DppApiResponse<JsonNode> updateDppById(String dppId, JsonNode partialDpp, DppRepresentation representation) {
        String body = HttpSupport.serializeJson(objectMapper, Objects.requireNonNull(partialDpp, "partialDpp"));
        return optionalDppResponse("PATCH", dppPath(dppId) + selected(representation).querySuffix(), Optional.of(body));
    }

    @Override
    public DppApiResponse<JsonNode> readDataElement(String dppId, String elementIdPath, DppRepresentation representation) {
        return requiredPayloadResponse("GET", dataElementPath(dppId, elementIdPath)
                + selected(representation).querySuffix(), Optional.empty());
    }

    @Override
    public DppApiResponse<JsonNode> updateDataElement(String dppId, String elementIdPath, JsonNode dataElement,
            DppRepresentation representation) {
        String body = HttpSupport.serializeJson(objectMapper, Objects.requireNonNull(dataElement, "dataElement"));
        return requiredPayloadResponse("PATCH", dataElementPath(dppId, elementIdPath)
                + selected(representation).querySuffix(), Optional.of(body));
    }

    private static DppRepresentation selected(DppRepresentation representation) {
        return representation == null ? DppRepresentation.COMPRESSED : representation;
    }

    private DppApiResponse<JsonNode> optionalDppResponse(String method, String path, Optional<String> body) {
        return sendForApiResponse(method, path, body);
    }

    private DppApiResponse<JsonNode> requiredPayloadResponse(String method, String path, Optional<String> body) {
        DppApiResponse<JsonNode> response = sendForApiResponse(method, path, body);
        HttpSupport.requirePayload(response);
        return response;
    }

    @Override
    public DppApiResponse<JsonNode> readDppById(String dppId) {
        return readDppById(dppId, DppRepresentation.COMPRESSED);
    }

    @Override
    public DppApiResponse<JsonNode> readDppByProductId(String productId) {
        return readDppByProductId(productId, DppRepresentation.COMPRESSED);
    }

    @Override
    public DppApiResponse<JsonNode> readDppVersionByIdAndDate(String dppId, Instant date) {
        return readDppVersionByIdAndDate(dppId, date, DppRepresentation.COMPRESSED);
    }

    @Override
    public ReadDppIdsResponse readDppIdsByProductIds(List<String> productIds, Integer limit, String cursor) {
        ReadDppIdsRequest requestBody = new ReadDppIdsRequest(productIds, limit, cursor);
        String body = HttpSupport.serializeJson(objectMapper, requestBody);
        DppApiResponse<JsonNode> response = sendForApiResponse("POST", DPPS_BY_PRODUCT_IDS_PATH, Optional.of(body));
        JsonNode payload = response.getPayload();
        HttpSupport.requireField(payload, "dppIdentifiers", "payload.dppIdentifiers");
        ReadDppIdsResponse result = HttpSupport.deserializeJson(objectMapper, payload, ReadDppIdsResponse.class);
        result.setStatusCode(response.getStatusCode());
        result.setMessage(response.getMessage());
        return result;
    }

    @Override
    public DppApiResponse<JsonNode> updateDppById(String dppId, JsonNode partialDpp) {
        return updateDppById(dppId, partialDpp, DppRepresentation.COMPRESSED);
    }

    @Override
    public DeleteDppResponse deleteDppById(String dppId) {
        DppApiResponse<JsonNode> response = sendForApiResponse("DELETE", dppPath(dppId), Optional.empty());
        return new DeleteDppResponse(response.getStatusCode(), response.getMessage());
    }

    @Override
    public DppApiResponse<JsonNode> readDataElement(String dppId, String elementIdPath) {
        return readDataElement(dppId, elementIdPath, DppRepresentation.COMPRESSED);
    }

    @Override
    public DppApiResponse<JsonNode> updateDataElement(String dppId, String elementIdPath, JsonNode dataElement) {
        return updateDataElement(dppId, elementIdPath, dataElement, DppRepresentation.COMPRESSED);
    }

    private DppApiResponse<JsonNode> sendForApiResponse(String method, String path, Optional<String> body) {
        HttpRequest request = HttpSupport.jsonRequest(HttpSupport.resolve(baseUrl, path), method, body);
        HttpResponse<String> response = HttpSupport.send(httpClient, request);
        HttpSupport.requireSuccess(response, objectMapper);
        if (response.statusCode() == 204) {
            DppApiResponse<JsonNode> noContent = new DppApiResponse<>();
            noContent.setStatusCode(dpp.repo.payloads.DppStatusCode.SuccessNoContent);
            return noContent;
        }
        DppApiResponse<JsonNode> apiResponse = HttpSupport.parseApiResponse(objectMapper, response.body());
        HttpSupport.requireApiSuccess(apiResponse, response.body());
        return apiResponse;
    }

    private static String dppPath(String dppId) {
        return DPPS_PATH + "/" + HttpSupport.encodePathSegment(dppId);
    }

    private static String dataElementPath(String dppId, String elementIdPath) {
        String path = Objects.requireNonNull(elementIdPath, "elementIdPath must not be null");
        if (path.isBlank()) {
            throw new IllegalArgumentException("elementIdPath must not be blank");
        }
        return dppPath(dppId) + "/elements/" + HttpSupport.encodePathSegment(path);
    }
}
