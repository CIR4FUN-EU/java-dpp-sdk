package demo.repo;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dpp.repo.payloads.CreateDppResponse;
import dpp.repo.payloads.DppStatusCode;
import dpp.repo.payloads.ReadDppIdsRequest;
import dpp.repo.payloads.ReadDppIdsResponse;
import dpp.repo.payloads.DppRepresentation;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.transport.json.DppJsonCodec;
import dppsdk.core.transport.json.CompressedExpansionResult;
import dppsdk.core.semantic.SemanticResolver;

/**
 * Coordinates the demo repository lifecycle behavior.
 *
 * <p>This service is the module boundary between HTTP-facing controllers and in-memory storage. It
 * delegates full DPP parsing and validation to the SDK adapter layer, records version snapshots and
 * basic lifecycle events, and keeps update/delete semantics consistent for the demo API.</p>
 */
@Service
class DppRepoService {

    private static final Logger log = LoggerFactory.getLogger(DppRepoService.class);

    private final DppRepoBackend backend;
    private final DppJsonCodec codec;
    private final RepositoryProfiles profiles;
    private final boolean eventsEnabled;
    private final ObjectMapper objectMapper;
    private final DppIdentifierExtractor identifierExtractor;
    private final DppMergePatchService mergePatchService;
    private final DppElementPathService elementPathService;
    private final Clock clock;

    DppRepoService(
            DppRepoBackend backend,
            DppJsonCodec codec,
            RepositoryProfiles profiles,
            @org.springframework.beans.factory.annotation.Value("${dpp.repo.events.enabled:true}") boolean eventsEnabled,
            ObjectMapper objectMapper,
            DppIdentifierExtractor identifierExtractor,
            DppMergePatchService mergePatchService,
            DppElementPathService elementPathService,
            Clock clock
    ) {
        this.backend = backend;
        this.codec = codec;
        this.profiles = profiles;
        this.eventsEnabled = eventsEnabled;
        this.objectMapper = objectMapper;
        this.identifierExtractor = identifierExtractor;
        this.mergePatchService = mergePatchService;
        this.elementPathService = elementPathService;
        this.clock = clock;
    }

    CreateDppResponse create(String jsonPayload) {
        DigitalProductPassport dpp = parseAndValidate(jsonPayload);
        return persist(dpp, Instant.now(clock));
    }

    CreateDppResponse create(String jsonPayload, DppRepresentation representation) {
        DigitalProductPassport dpp = representation == DppRepresentation.COMPRESSED
                ? expandedCompressed(jsonPayload, null)
                : codec.fromExpandedJson(jsonPayload);
        profiles.validate(dpp);
        return persist(dpp, Instant.now(clock));
    }

    CreateDppResponse createAt(DigitalProductPassport dpp, Instant occurredAt) {
        profiles.validate(dpp);
        return persist(dpp, occurredAt);
    }

    private CreateDppResponse persist(DigitalProductPassport dpp, Instant occurredAt) {
        String dppId = identifierExtractor.extractDppId(dpp);
        backend.create(dpp, occurredAt);
        return new CreateDppResponse(dppId);
    }

    JsonNode readById(String dppId) {
        return readById(dppId, DppRepresentation.FULL);
    }

    JsonNode readById(String dppId, DppRepresentation representation) {
        DigitalProductPassport dpp = backend.findCurrentByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        return represent(dpp, representation);
    }

    boolean hasAnyDpp(String dppId) {
        return backend.existsAnyByDppId(dppId);
    }

    JsonNode readByProductId(String productId) {
        return readByProductId(productId, DppRepresentation.FULL);
    }

    JsonNode readByProductId(String productId, DppRepresentation representation) {
        DigitalProductPassport dpp = backend.findCurrentByProductId(productId)
                .orElseThrow(() -> new RepoApiException(DppStatusCode.ClientErrorResourceNotFound,
                        "PRODUCT_NOT_FOUND", "No active DPP found for product id " + productId));
        return represent(dpp, representation);
    }

    JsonNode readVersionByDppIdAndDate(String dppId, String timestamp) {
        return readVersionByDppIdAndDate(dppId, timestamp, DppRepresentation.FULL);
    }

    JsonNode readVersionByDppIdAndDate(String dppId, String timestamp, DppRepresentation representation) {
        Instant requestedAt;
        try {
            requestedAt = Instant.parse(timestamp);
        } catch (DateTimeParseException exception) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_DATE",
                    "Invalid UTC timestamp " + timestamp);
        }
        DigitalProductPassport version = backend.findByDppIdAt(dppId, requestedAt)
                .orElseThrow(() -> new RepoApiException(DppStatusCode.ClientErrorResourceNotFound,
                        "DPP_VERSION_NOT_FOUND",
                        "No DPP version found for id " + dppId + " at " + timestamp));
        return represent(version, representation);
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

    List<String> readAllActiveDppIds() {
        return backend.findAllActiveDppIds();
    }

    JsonNode updateById(String dppId, String patchJson) {
        return updateById(dppId, patchJson, DppRepresentation.FULL);
    }

    JsonNode updateById(String dppId, String patchJson, DppRepresentation representation) {
        CurrentDppSnapshot snapshot = backend.findCurrentSnapshotByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        DigitalProductPassport current = snapshot.dpp();
        // Build and validate the full merged DPP before touching stored state so failed patches stay atomic.
        JsonNode currentJson = represent(current, representation);
        JsonNode merged = mergePatchService.merge(currentJson.deepCopy(), readTree(patchJson));
        DigitalProductPassport mergedDpp = representation == DppRepresentation.COMPRESSED
                ? expandedCompressed(writeTree(merged), current)
                : codec.fromExpandedJson(writeTree(merged));
        profiles.validate(mergedDpp);
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

        if (sameCanonicalContent(current, mergedDpp)) {
            return represent(mergedDpp, representation);
        }

        Instant now = Instant.now(clock);
        backend.appendVersion(mergedDpp, snapshot.versionNo(), now, "DPP_UPDATED", Map.of("productId", mergedProductId));
        return represent(mergedDpp, representation);
    }

    void deleteById(String dppId) {
        CurrentDppSnapshot snapshot = backend.findCurrentSnapshotByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        backend.softDelete(dppId, snapshot.versionNo(), Instant.now(clock));
    }

    JsonNode readDataElement(String dppId, String elementIdPath) {
        return readDataElement(dppId, elementIdPath, DppRepresentation.FULL);
    }

    JsonNode readDataElement(String dppId, String elementIdPath, DppRepresentation representation) {
        DigitalProductPassport dpp = backend.findCurrentByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        JsonNode element = elementPathService.read(dppToJsonNode(dpp), elementIdPath);
        requireDataElement(element);
        if (representation == DppRepresentation.COMPRESSED) {
            return readTree(codec.toCompressedElementJson(codec.fromExpandedElementJson(writeTree(element))));
        }
        return element;
    }

    JsonNode updateDataElement(String dppId, String elementIdPath, JsonNode dataElement) {
        return updateDataElement(dppId, elementIdPath, dataElement, DppRepresentation.FULL);
    }

    JsonNode updateDataElement(String dppId, String elementIdPath, JsonNode dataElement,
            DppRepresentation representation) {
        if (dataElement == null || (representation == DppRepresentation.FULL && !dataElement.isObject())) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_ELEMENT_UPDATE",
                    "UpdateDataElement payload is invalid for the selected representation");
        }
        if (representation == DppRepresentation.COMPRESSED && containsExpandedDataElementMetadata(dataElement)) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_ELEMENT_UPDATE",
                    "Compressed updates must omit expanded DataElement metadata");
        }
        CurrentDppSnapshot snapshot = backend.findCurrentSnapshotByDppId(dppId)
                .orElseThrow(() -> notFound("No DPP found for id " + dppId));
        DigitalProductPassport current = snapshot.dpp();
        ObjectNode currentTree = (ObjectNode) dppToJsonNode(current);
        ObjectNode workingTree = currentTree.deepCopy();
        // Fine-granular writes still validate the entire resulting DPP before persisting the change.
        JsonNode currentElement = elementPathService.read(workingTree, elementIdPath);
        requireDataElement(currentElement);
        JsonNode originalElementId = currentElement.path("elementId").deepCopy();
        JsonNode originalObjectType = currentElement.path("objectType").deepCopy();
        if (representation == DppRepresentation.COMPRESSED) {
            var oldElement = codec.fromExpandedElementJson(writeTree(currentElement));
            JsonNode incoming = dataElement;
            if (incoming.isObject()) {
                JsonNode currentOperational = readTree(codec.toCompressedElementJson(oldElement));
                if (currentOperational.isObject()) {
                    incoming = mergePatchService.merge(currentOperational.deepCopy(), incoming);
                }
            }
            SemanticResolver resolver = profiles.semanticResolver();
            var semanticPath = elementPathService.semanticPath(workingTree, elementIdPath);
            var expansion = resolver == null
                    ? codec.expandCompressedElementJsonWithIssues(oldElement.getElementId(),
                            writeTree(incoming), oldElement)
                    : codec.expandCompressedElementJsonWithIssues(oldElement.getElementId(), writeTree(incoming),
                            oldElement, current.getDppHeader().getContentSpecificationIds(), resolver, semanticPath);
            logExpansionIssues(expansion.issues());
            dataElement = readTree(codec.toExpandedElementJson(expansion.element()));
        }
        JsonNode updatedElement = elementPathService.amend(workingTree, elementIdPath, dataElement);
        if (!originalElementId.equals(updatedElement.path("elementId"))
                || !originalObjectType.equals(updatedElement.path("objectType"))) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "DATA_ELEMENT_IDENTITY_MISMATCH",
                    "The update DataElement must retain the selected elementId and objectType");
        }
        requireDataElement(updatedElement);
        DigitalProductPassport updatedDpp = parseAndValidate(writeTree(workingTree));
        if (!dppId.equals(identifierExtractor.extractDppId(updatedDpp))) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "DPP_ID_IMMUTABLE",
                    "The DPP id must remain unchanged during element updates");
        }
        String currentProductId = identifierExtractor.extractProductId(current);
        if (!currentProductId.equals(identifierExtractor.extractProductId(updatedDpp))) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "PRODUCT_ID_IMMUTABLE",
                    "The product id must remain unchanged during element updates");
        }

        if (sameCanonicalContent(current, updatedDpp)) {
            return representElement(updatedElement, representation);
        }

        Instant now = Instant.now(clock);
        backend.appendVersion(updatedDpp, snapshot.versionNo(), now, "DATA_ELEMENT_UPDATED",
                Map.of("elementIdPath", elementIdPath));
        return representElement(updatedElement, representation);
    }

    List<LifecycleEventRecord> readEvents(String dppId) {
        if (!eventsEnabled) {
            throw new RepoApiException(DppStatusCode.ServerNotImplemented, "EVENTS_DISABLED",
                    "Repository lifecycle events are disabled by configuration");
        }
        if (!backend.existsAnyByDppId(dppId)) {
            throw notFound("No DPP found for id " + dppId);
        }
        return backend.findEventsByDppId(dppId);
    }

    private DigitalProductPassport parseAndValidate(String jsonPayload) {
        try {
            DigitalProductPassport passport = codec.fromExpandedJson(jsonPayload);
            profiles.validate(passport);
            return passport;
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    private DigitalProductPassport expandedCompressed(String json, DigitalProductPassport previous) {
        JsonNode input = readTree(json);
        if (input != null && input.path("elements").isArray()) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_REPRESENTATION",
                    "Expanded DPP JSON requires representation=full");
        }
        SemanticResolver resolver = profiles.semanticResolver();
        CompressedExpansionResult expansion = resolver == null
                ? codec.expandCompressedJson(json, previous)
                : codec.expandCompressedJson(json, previous, resolver);
        logExpansionIssues(expansion.issues());
        return expansion.passport();
    }

    private boolean containsExpandedDataElementMetadata(JsonNode node) {
        if (node == null || node.isNull() || node.isValueNode()) return false;
        if (node.isObject()) {
            if (node.has("elementId") || node.has("objectType")) return true;
            var fields = node.fields();
            while (fields.hasNext()) {
                if (containsExpandedDataElementMetadata(fields.next().getValue())) return true;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                if (containsExpandedDataElementMetadata(child)) return true;
            }
        }
        return false;
    }

    private void logExpansionIssues(List<CompressedExpansionResult.ExpansionIssue> issues) {
        for (CompressedExpansionResult.ExpansionIssue issue : issues) {
            log.info("Compressed DPP expansion at {}: {}", issue.path(), issue.reason());
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

    private JsonNode dppToJsonNode(DigitalProductPassport dpp) {
        return readTree(codec.toExpandedJson(dpp));
    }

    private boolean sameCanonicalContent(DigitalProductPassport left, DigitalProductPassport right) {
        return sameJsonValue(dppToJsonNode(left), dppToJsonNode(right));
    }

    private void requireDataElement(JsonNode value) {
        codec.fromExpandedElementJson(writeTree(value));
    }

    private boolean sameJsonValue(JsonNode left, JsonNode right) {
        if (left.isNumber() && right.isNumber()) {
            return left.decimalValue().compareTo(right.decimalValue()) == 0;
        }
        if (left.isObject() && right.isObject()) {
            if (left.size() != right.size()) {
                return false;
            }
            var fields = left.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                if (!right.has(field.getKey()) || !sameJsonValue(field.getValue(), right.get(field.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        if (left.isArray() && right.isArray()) {
            if (left.size() != right.size()) {
                return false;
            }
            for (int index = 0; index < left.size(); index++) {
                if (!sameJsonValue(left.get(index), right.get(index))) {
                    return false;
                }
            }
            return true;
        }
        return left.equals(right);
    }

    private JsonNode represent(DigitalProductPassport dpp, DppRepresentation representation) {
        if (representation == DppRepresentation.COMPRESSED) {
            return readTree(codec.toCompressedJson(dpp));
        }
        return dppToJsonNode(dpp);
    }

    private JsonNode representElement(JsonNode expanded, DppRepresentation representation) {
        if (representation == DppRepresentation.FULL) return expanded;
        return readTree(codec.toCompressedElementJson(codec.fromExpandedElementJson(writeTree(expanded))));
    }
}
