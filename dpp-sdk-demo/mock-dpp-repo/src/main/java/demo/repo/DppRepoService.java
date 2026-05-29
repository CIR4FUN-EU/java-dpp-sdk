package demo.repo;

import java.io.IOException;
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

    private final InMemoryDppStore store;
    private final Dpp4FunJsonCodec codec;
    private final Dpp4FunValidationService validationService;
    private final ObjectMapper objectMapper;
    private final DppIdentifierExtractor identifierExtractor;
    private final DppMergePatchService mergePatchService;
    private final DppElementPathService elementPathService;

    DppRepoService(
            InMemoryDppStore store,
            Dpp4FunJsonCodec codec,
            Dpp4FunValidationService validationService,
            ObjectMapper objectMapper,
            DppIdentifierExtractor identifierExtractor,
            DppMergePatchService mergePatchService,
            DppElementPathService elementPathService
    ) {
        this.store = store;
        this.codec = codec;
        this.validationService = validationService;
        this.objectMapper = objectMapper;
        this.identifierExtractor = identifierExtractor;
        this.mergePatchService = mergePatchService;
        this.elementPathService = elementPathService;
    }

    CreateDppResponse create(String jsonPayload) {
        Dpp4Fun dpp = parseAndValidate(jsonPayload);
        String dppId = identifierExtractor.extractDppId(dpp);
        String productId = identifierExtractor.extractProductId(dpp);
        String canonicalJson = codec.toJson(dpp);
        Instant now = Instant.now();

        store.create(dppId, productId, canonicalJson, now);
        store.appendEvent(dppId, "DPP_CREATED", now, objectNode(Map.of("productId", productId)));
        return new CreateDppResponse(dppId);
    }

    JsonNode readById(String dppId) {
        StoredDppRecord record = store.findActiveByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        return readTree(record.dppJson());
    }

    boolean hasActiveDpp(String dppId) {
        return store.hasActiveDpp(dppId);
    }

    JsonNode readByProductId(String productId) {
        StoredDppRecord record = store.findActiveByProductId(productId)
                .orElseThrow(() -> new RepoApiException(DppStatusCode.ClientErrorResourceNotFound,
                        "PRODUCT_NOT_FOUND", "No active DPP found for product id " + productId));
        return readTree(record.dppJson());
    }

    JsonNode readVersionByProductIdAndDate(String productId, String timestamp) {
        Instant requestedAt;
        try {
            requestedAt = Instant.parse(timestamp);
        } catch (DateTimeParseException exception) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_DATE",
                    "Invalid UTC timestamp " + timestamp);
        }
        DppVersionRecord version = store.findVersionByProductIdAndDate(productId, requestedAt)
                .orElseThrow(() -> new RepoApiException(DppStatusCode.ClientErrorResourceNotFound,
                        "DPP_VERSION_NOT_FOUND",
                        "No DPP version found for product id " + productId + " at " + timestamp));
        return readTree(version.dppJson());
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
        List<String> dppIds = store.findDppIdsByProductIds(request.getProductIdentifiers(), offset, limit);
        ReadDppIdsResponse response = new ReadDppIdsResponse();
        response.setDppIdentifiers(dppIds);
        response.setNextCursor(store.nextCursor(request.getProductIdentifiers(), offset, limit));
        return response;
    }

    JsonNode updateById(String dppId, String patchJson) {
        StoredDppRecord current = store.findActiveByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        // Build and validate the full merged DPP before touching stored state so failed patches stay atomic.
        JsonNode merged = mergePatchService.merge(readTree(current.dppJson()).deepCopy(), readTree(patchJson));
        Dpp4Fun mergedDpp = parseAndValidate(writeTree(merged));
        String mergedDppId = identifierExtractor.extractDppId(mergedDpp);
        if (!dppId.equals(mergedDppId)) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "DPP_ID_IMMUTABLE",
                    "The DPP id in the patch result must remain " + dppId);
        }
        String mergedProductId = identifierExtractor.extractProductId(mergedDpp);
        if (!current.productId().equals(mergedProductId)) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "PRODUCT_ID_IMMUTABLE",
                    "The product id in the patch result must remain " + current.productId());
        }

        String canonicalJson = codec.toJson(mergedDpp);
        Instant now = Instant.now();
        store.update(dppId, canonicalJson, now);
        store.appendEvent(dppId, "DPP_UPDATED", now, objectNode(Map.of("productId", mergedProductId)));
        return readTree(canonicalJson);
    }

    void deleteById(String dppId) {
        StoredDppRecord deleted = store.softDelete(dppId, Instant.now());
        store.appendEvent(dppId, "DPP_DELETED", deleted.deletedAt(), objectNode(Map.of("productId", deleted.productId())));
    }

    JsonNode readDataElement(String dppId, String elementPath) {
        StoredDppRecord record = store.findActiveByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        return elementPathService.read(readTree(record.dppJson()), elementPath);
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
        StoredDppRecord current = store.findActiveByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        ObjectNode workingTree = (ObjectNode) readTree(current.dppJson());
        // Fine-granular writes still validate the entire resulting DPP before persisting the change.
        JsonNode updatedElement = elementPathService.update(workingTree, elementPath, request.getPayload());
        Dpp4Fun updatedDpp = parseAndValidate(writeTree(workingTree));
        if (!dppId.equals(identifierExtractor.extractDppId(updatedDpp))) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "DPP_ID_IMMUTABLE",
                    "The DPP id must remain unchanged during element updates");
        }
        if (!current.productId().equals(identifierExtractor.extractProductId(updatedDpp))) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "PRODUCT_ID_IMMUTABLE",
                    "The product id must remain unchanged during element updates");
        }

        String canonicalJson = codec.toJson(updatedDpp);
        Instant now = Instant.now();
        store.update(dppId, canonicalJson, now);
        store.appendEvent(dppId, "DATA_ELEMENT_UPDATED", now, objectNode(Map.of("elementPath", elementPath)));
        return updatedElement;
    }

    List<LifecycleEventRecord> readEvents(String dppId) {
        if (store.findAnyByDppId(dppId).isEmpty()) {
            throw notFound("No DPP found for id " + dppId);
        }
        return store.eventsFor(dppId);
    }

    List<DppVersionRecord> readVersions(String productId) {
        return store.versionsForProduct(productId);
    }

    void clear() {
        store.clear();
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

    private com.fasterxml.jackson.databind.node.ObjectNode objectNode(Map<String, String> values) {
        com.fasterxml.jackson.databind.node.ObjectNode node = objectMapper.createObjectNode();
        values.forEach(node::put);
        return node;
    }
}
