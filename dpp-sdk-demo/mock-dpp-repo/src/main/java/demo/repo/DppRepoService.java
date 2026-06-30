package demo.repo;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dpp.repo.payloads.CreateDppResponse;
import dpp.repo.payloads.DppStatusCode;
import dpp.repo.payloads.ReadDppIdsRequest;
import dpp.repo.payloads.ReadDppIdsResponse;
import dpp.repo.payloads.UpdateDataElementRequest;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;

/**
 * Coordinates the mock repository lifecycle behavior.
 *
 * <p>This service is the module boundary between HTTP-facing controllers and in-memory storage. It
 * delegates full DPP parsing and validation to the SDK adapter layer, records version snapshots and
 * basic lifecycle events, and keeps update/delete semantics consistent for the mock API.</p>
 */
@Service
class DppRepoService {

    private final DppRepoBackend backend;
    private final Dpp4FunJsonCodec codec;
    private final Dpp4FunValidationService validationService;
    private final ObjectMapper objectMapper;
    private final DppIdentifierExtractor identifierExtractor;
    private final DppMergePatchService mergePatchService;
    private final DppElementPathService elementPathService;
    private final Clock clock;

    DppRepoService(
            DppRepoBackend backend,
            Dpp4FunJsonCodec codec,
            Dpp4FunValidationService validationService,
            ObjectMapper objectMapper,
            DppIdentifierExtractor identifierExtractor,
            DppMergePatchService mergePatchService,
            DppElementPathService elementPathService,
            Clock clock
    ) {
        this.backend = backend;
        this.codec = codec;
        this.validationService = validationService;
        this.objectMapper = objectMapper;
        this.identifierExtractor = identifierExtractor;
        this.mergePatchService = mergePatchService;
        this.elementPathService = elementPathService;
        this.clock = clock;
    }

    CreateDppResponse create(String jsonPayload) {
        Dpp4Fun dpp = parseAndValidate(jsonPayload);
        String dppId = identifierExtractor.extractDppId(dpp);
        Instant now = Instant.now(clock);

        backend.create(dpp, now);
        return new CreateDppResponse(dppId);
    }

    JsonNode readById(String dppId) {
        Dpp4Fun dpp = backend.findCurrentByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        return dppToJsonNode(dpp);
    }

    boolean hasActiveDpp(String dppId) {
        return backend.existsActiveByDppId(dppId);
    }

    JsonNode readByProductId(String productId) {
        Dpp4Fun dpp = backend.findCurrentByProductId(productId)
                .orElseThrow(() -> new RepoApiException(DppStatusCode.ClientErrorResourceNotFound,
                        "PRODUCT_NOT_FOUND", "No active DPP found for product id " + productId));
        return dppToJsonNode(dpp);
    }

    JsonNode readVersionByProductIdAndDate(String productId, String timestamp) {
        Instant requestedAt;
        try {
            requestedAt = Instant.parse(timestamp);
        } catch (DateTimeParseException exception) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_DATE",
                    "Invalid UTC timestamp " + timestamp);
        }
        Dpp4Fun version = backend.findByProductIdAt(productId, requestedAt)
                .orElseThrow(() -> new RepoApiException(DppStatusCode.ClientErrorResourceNotFound,
                        "DPP_VERSION_NOT_FOUND",
                        "No DPP version found for product id " + productId + " at " + timestamp));
        return dppToJsonNode(version);
    }

    ReadDppIdsResponse readIdsByProductIds(ReadDppIdsRequest request) {
        if (request == null || request.getProductIdentifiers() == null || request.getProductIdentifiers().isEmpty()) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "EMPTY_PRODUCT_IDENTIFIERS",
                    "productIdentifiers must not be empty");
        }
        int limit = request.getLimit() == null ? 50 : request.getLimit();
        if (limit <= 0) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_LIMIT",
                    "limit must be greater than zero");
        }

        int offset = parseCursor(request.getCursor());
        if (offset > request.getProductIdentifiers().size()) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_CURSOR",
                    "cursor is outside the supplied productIdentifiers range");
        }
        DppIdPage page = backend.findActiveDppIdsByProductIds(request.getProductIdentifiers(), offset, limit);
        ReadDppIdsResponse response = new ReadDppIdsResponse();
        response.setDppIdentifiers(page.dppIds());
        response.setNextCursor(page.nextCursor());
        return response;
    }

    JsonNode updateById(String dppId, String patchJson) {
        Dpp4Fun current = backend.findCurrentByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        // Build and validate the full merged DPP before touching stored state so failed patches stay atomic.
        JsonNode merged = mergePatchService.merge(dppToJsonNode(current).deepCopy(), readTree(patchJson));
        Dpp4Fun mergedDpp = parseAndValidate(writeTree(merged));
        String mergedDppId = identifierExtractor.extractDppId(mergedDpp);
        if (!dppId.equals(mergedDppId)) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "DPP_ID_IMMUTABLE",
                    "The DPP id in the patch result must remain " + dppId);
        }
        String mergedProductId = identifierExtractor.extractProductId(mergedDpp);
        if (!identifierExtractor.extractProductId(current).equals(mergedProductId)) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "PRODUCT_ID_IMMUTABLE",
                    "The product id in the patch result must remain " + identifierExtractor.extractProductId(current));
        }

        Instant now = Instant.now(clock);
        backend.appendVersion(mergedDpp, now, "DPP_UPDATED", Map.of("productId", mergedProductId));
        return dppToJsonNode(mergedDpp);
    }

    void deleteById(String dppId) {
        backend.softDelete(dppId, Instant.now(clock));
    }

    JsonNode readDataElement(String dppId, String elementPath) {
        Dpp4Fun dpp = backend.findCurrentByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        return elementPathService.read(dppToJsonNode(dpp), elementPath);
    }

    JsonNode updateDataElement(String dppId, String elementPath, UpdateDataElementRequest request) {
        if (request == null) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_ELEMENT_UPDATE",
                    "Element update request must not be null");
        }
        if (request.getPayload() == null || request.getPayload().isNull()) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_ELEMENT_UPDATE",
                    "payload must be provided for element updates");
        }
        Dpp4Fun current = backend.findCurrentByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        ObjectNode workingTree = (ObjectNode) dppToJsonNode(current);
        // Fine-granular writes still validate the entire resulting DPP before persisting the change.
        JsonNode updatedElement = elementPathService.update(workingTree, elementPath, request.getPayload());
        Dpp4Fun updatedDpp = parseAndValidate(writeTree(workingTree));
        if (!dppId.equals(identifierExtractor.extractDppId(updatedDpp))) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "DPP_ID_IMMUTABLE",
                    "The DPP id must remain unchanged during element updates");
        }
        String currentProductId = identifierExtractor.extractProductId(current);
        if (!currentProductId.equals(identifierExtractor.extractProductId(updatedDpp))) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "PRODUCT_ID_IMMUTABLE",
                    "The product id must remain unchanged during element updates");
        }

        Instant now = Instant.now(clock);
        backend.appendVersion(updatedDpp, now, "DATA_ELEMENT_UPDATED", Map.of("elementPath", elementPath));
        return updatedElement;
    }

    List<LifecycleEventRecord> readEvents(String dppId) {
        if (!backend.existsAnyByDppId(dppId)) {
            throw notFound("No DPP found for id " + dppId);
        }
        return backend.findEventsByDppId(dppId);
    }

    void clear() {
        backend.clear();
    }

    private Dpp4Fun parseAndValidate(String jsonPayload) {
        try {
            Dpp4Fun dpp = codec.fromJson(jsonPayload);
            validationService.validate(dpp);
            return dpp;
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException exception) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "MALFORMED_JSON",
                    "Malformed JSON payload");
        }
    }

    private String writeTree(JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (IOException exception) {
            throw new RepoApiException(DppStatusCode.ServerInternalError, "JSON_SERIALIZATION_FAILED",
                    "Failed to serialize DPP JSON");
        }
    }

    private int parseCursor(String cursor) {
        if (cursor == null) {
            return 0;
        }
        if (cursor.isBlank()) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_CURSOR",
                    "cursor must not be blank");
        }
        try {
            int parsed = Integer.parseInt(cursor);
            if (parsed < 0) {
                throw new NumberFormatException("negative");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_CURSOR",
                    "cursor must be a non-negative integer");
        }
    }

    private RepoApiException notFound(String message) {
        return new RepoApiException(DppStatusCode.ClientErrorResourceNotFound, "DPP_NOT_FOUND", message);
    }

    private JsonNode dppToJsonNode(Dpp4Fun dpp) {
        return readTree(codec.toJson(dpp));
    }
}
