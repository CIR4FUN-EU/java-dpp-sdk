package dppsdk.core.transport.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.MultiLanguageValue;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.RelatedResource;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.semantic.DataElementKind;
import dppsdk.core.semantic.SemanticDefinition;
import dppsdk.core.semantic.SemanticPath;
import dppsdk.core.semantic.SemanticResolver;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** JSON transport for the canonical EN-oriented passport domain model. */
public final class DppJsonCodec {
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;
    private static final Set<String> HEADER_FIELDS = Set.of(
            "digitalProductPassportId", "uniqueProductIdentifier", "granularity",
            "dppSchemaVersion", "dppStatus", "lastUpdated", "economicOperatorId",
            "facilityId", "contentSpecificationIds");
    private static final Set<String> EXPANDED_ROOT_FIELDS = Set.of("elements");
    private static final Set<String> COMMON_ELEMENT_FIELDS = Set.of(
            "elementId", "objectType", "dictionaryReference");

    private final ObjectMapper mapper;

    public DppJsonCodec() {
        JsonFactory factory = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
        this.mapper = new ObjectMapper(factory);
        this.mapper.setNodeFactory(new JsonNodeFactory(true));
        this.mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    /** Writes the complete domain representation with header attributes flattened at the root. */
    public String toExpandedJson(DigitalProductPassport dpp) {
        try {
            ObjectNode root = headerNode(requirePassport(dpp).getDppHeader());
            ArrayNode elements = root.putArray("elements");
            for (DataElement element : dpp.getDataElements()) {
                elements.add(expandedElement(element));
            }
            return mapper.writeValueAsString(root);
        } catch (DppJsonException e) {
            throw e;
        } catch (JsonProcessingException | RuntimeException e) {
            throw new DppJsonException("Failed to serialize expanded DPP JSON", e);
        }
    }

    /** Reads the expanded full representation into the immutable domain model. */
    public DigitalProductPassport fromExpandedJson(String json) {
        try {
            JsonNode parsed = mapper.readTree(json);
            if (!(parsed instanceof ObjectNode root)) {
                throw new DppJsonException("Expanded DPP JSON root must be an object");
            }
            checkKnownFields(root, HEADER_FIELDS, EXPANDED_ROOT_FIELDS, "$");
            DppHeader header = readHeader(root);
            JsonNode elementsNode = root.get("elements");
            if (elementsNode != null && !elementsNode.isArray()) {
                throw invalid("$.elements", "must be an array");
            }
            List<DataElement> elements = new ArrayList<>(elementsNode == null ? 0 : elementsNode.size());
            for (int i = 0; elementsNode != null && i < elementsNode.size(); i++) {
                elements.add(readElement(elementsNode.get(i), "$.elements[" + i + "]"));
            }
            return new DigitalProductPassport.Builder().dppHeader(header)
                    .dataElements(elements).build();
        } catch (DppJsonException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new DppJsonException("Failed to deserialize expanded DPP JSON", e);
        }
    }

    /**
     * Reads the flattened EN 18223 header from either expanded or compressed DPP JSON.
     * Element content is deliberately not parsed or reconstructed.
     *
     * @param dppJson full or compressed DPP JSON object
     * @return parsed header
     * @throws DppJsonException if the root or a required header field is invalid
     */
    public DppHeader readHeader(JsonNode dppJson) {
        if (!(dppJson instanceof ObjectNode root)) {
            throw invalid("$", "DPP JSON root must be an object");
        }
        return readHeader(root);
    }

    /** Writes the operational compressed representation. */
    public String toCompressedJson(DigitalProductPassport dpp) {
        try {
            DigitalProductPassport passport = requirePassport(dpp);
            ObjectNode root = headerNode(passport.getDppHeader());
            for (DataElement element : passport.getDataElements()) {
                String id = requireElementId(element, "$");
                if (HEADER_FIELDS.contains(id) || root.has(id)) {
                    throw invalid("$." + id, "compressed element key collides with an existing root property");
                }
                root.set(id, compressedValue(element, "$." + id));
            }
            return mapper.writeValueAsString(root);
        } catch (DppJsonException e) {
            throw e;
        } catch (JsonProcessingException | RuntimeException e) {
            throw new DppJsonException("Failed to serialize compressed DPP JSON", e);
        }
    }

    /**
     * Reconstructs the canonical structure that can be determined from EN 18223 operational JSON.
     * Dictionary references, value data types, and original array-item identifiers are not recoverable.
     */
    public CompressedExpansionResult expandCompressedJson(String json) {
        return expandCompressedJson(json, (DigitalProductPassport) null);
    }

    /** Expands operational JSON, using a stored passport when available to resolve subtype ambiguity. */
    public CompressedExpansionResult expandCompressedJson(String json, DigitalProductPassport previous) {
        return expandCompressedJsonInternal(json, previous, null);
    }

    /** Expands operational JSON using semantic definitions for information omitted by compression. */
    public CompressedExpansionResult expandCompressedJsonWithResolver(String json, SemanticResolver resolver) {
        return expandCompressedJson(json, null, resolver);
    }

    /** Expands operational JSON, combining a resolver with compatible stored semantic information. */
    public CompressedExpansionResult expandCompressedJson(
            String json, DigitalProductPassport previous, SemanticResolver resolver) {
        return expandCompressedJsonInternal(json, previous, Objects.requireNonNull(resolver, "resolver"));
    }

    private CompressedExpansionResult expandCompressedJsonInternal(
            String json, DigitalProductPassport previous, SemanticResolver resolver) {
        try {
            JsonNode parsed = mapper.readTree(json);
            if (!(parsed instanceof ObjectNode root)) {
                throw invalid("$", "compressed DPP JSON root must be an object");
            }
            DppHeader header = readHeader(root);
            List<DataElement> elements = new ArrayList<>();
            List<CompressedExpansionResult.ExpansionIssue> issues = new ArrayList<>();
            Map<String, DataElement> previousById = elementsById(
                    previous == null ? List.of() : previous.getDataElements());
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (!HEADER_FIELDS.contains(field.getKey())) {
                    elements.add(expandElement(field.getKey(), field.getValue(), "$.'" + field.getKey() + "'",
                            issues, previousById.get(field.getKey()), resolver, header.getContentSpecificationIds(),
                            SemanticPath.of(field.getKey())));
                }
            }
            DigitalProductPassport passport = new DigitalProductPassport.Builder()
                    .dppHeader(header).dataElements(elements).build();
            if (previous != null) {
                passport = passport.toBuilder()
                        .dataElements(restoreElements(passport.getDataElements(), previous.getDataElements(), resolver,
                                header.getContentSpecificationIds())).build();
            }
            return new CompressedExpansionResult(passport, issues);
        } catch (DppJsonException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new DppJsonException("Failed to expand compressed DPP JSON", e);
        }
    }

    /** Writes one operational DataElement value without its enclosing elementId key. */
    public String toCompressedElementJson(DataElement element) {
        try {
            return mapper.writeValueAsString(compressedValue(element, "$.'" + requireElementId(element, "$" ) + "'"));
        } catch (JsonProcessingException e) {
            throw new DppJsonException("Failed to serialize compressed DataElement JSON", e);
        }
    }

    /** Writes one full DataElement using the same serializer as a full passport. */
    public String toExpandedElementJson(DataElement element) {
        try {
            return mapper.writeValueAsString(expandedElement(element));
        } catch (JsonProcessingException e) {
            throw new DppJsonException("Failed to serialize expanded DataElement JSON", e);
        }
    }

    /** Reads one expanded DataElement from its full JSON form. */
    public DataElement fromExpandedElementJson(String json) {
        try {
            return readElement(mapper.readTree(json), "$element");
        } catch (IOException e) {
            throw new DppJsonException("Failed to parse expanded DataElement JSON", e);
        }
    }

    /** Reconstructs one operational value, retaining matching stored semantic metadata if present. */
    public DataElement expandCompressedElementJson(String elementId, String json, DataElement previous) {
        return expandCompressedElementJsonWithIssues(elementId, json, previous).element();
    }

    /** Reconstructs one operational value and reports information absent or ambiguous in that form. */
    public CompressedElementExpansionResult expandCompressedElementJsonWithIssues(
            String elementId, String json, DataElement previous) {
        if (elementId == null || elementId.isBlank()) throw invalid("$element", "elementId must not be blank");
        return expandCompressedElementJsonWithIssues(elementId, json, previous, List.of(), null);
    }

    /** Expands one operational value with semantic context and definitions. */
    public CompressedElementExpansionResult expandCompressedElementJsonWithIssues(
            String elementId, String json, DataElement previous,
            List<String> contentSpecificationIds, SemanticResolver resolver) {
        return expandCompressedElementJsonWithIssues(elementId, json, previous, contentSpecificationIds,
                resolver, SemanticPath.of(elementId));
    }

    /** Expands one operational value using its complete reserved semantic path. */
    public CompressedElementExpansionResult expandCompressedElementJsonWithIssues(
            String elementId, String json, DataElement previous,
            List<String> contentSpecificationIds, SemanticResolver resolver, SemanticPath semanticPath) {
        if (elementId == null || elementId.isBlank()) throw invalid("$element", "elementId must not be blank");
        Objects.requireNonNull(contentSpecificationIds, "contentSpecificationIds");
        Objects.requireNonNull(semanticPath, "semanticPath");
        try {
            List<CompressedExpansionResult.ExpansionIssue> issues = new ArrayList<>();
            DataElement fresh = expandElement(elementId, mapper.readTree(json), "$.'" + elementId + "'",
                    issues, previous, resolver, List.copyOf(contentSpecificationIds), semanticPath);
            return new CompressedElementExpansionResult(restoreElement(fresh, previous), issues);
        } catch (IOException e) {
            throw new DppJsonException("Failed to parse compressed DataElement JSON", e);
        }
    }

    private List<DataElement> restoreElements(List<DataElement> fresh, List<DataElement> previous) {
        Map<String, DataElement> byId = elementsById(previous);
        List<DataElement> restored = new ArrayList<>(fresh.size());
        for (DataElement element : fresh) restored.add(restoreElement(element, byId.get(element.getElementId())));
        return restored;
    }

    private List<DataElement> restoreElements(List<DataElement> fresh, List<DataElement> previous,
            SemanticResolver resolver, List<String> contentSpecificationIds) {
        Map<String, DataElement> byId = elementsById(previous);
        List<DataElement> restored = new ArrayList<>(fresh.size());
        for (DataElement element : fresh) {
            restored.add(restoreElement(element, byId.get(element.getElementId()), resolver,
                    contentSpecificationIds, SemanticPath.of(element.getElementId())));
        }
        return restored;
    }

    private static Map<String, DataElement> elementsById(List<DataElement> elements) {
        Map<String, DataElement> byId = new LinkedHashMap<>();
        for (DataElement element : elements) byId.put(element.getElementId(), element);
        return byId;
    }

    private DataElement restoreElement(DataElement fresh, DataElement old) {
        if (old == null) return fresh;
        // A single value may itself contain JSON objects or arrays. Its known stored subtype
        // is stronger evidence than the structural inference available from compressed JSON.
        if (old instanceof SingleValuedDataElement prior && fresh.getClass() != old.getClass()) {
            JsonNode operational = compressedValue(fresh, "$.'" + fresh.getElementId() + "'");
            return prior.toBuilder().value(javaValue(operational, "$.'" + fresh.getElementId() + "'")).build();
        }
        if (fresh.getClass() != old.getClass()) {
            throw invalid("$.'" + fresh.getElementId() + "'",
                    "compressed value cannot change the stored DataElement subtype");
        }
        if (fresh instanceof SingleValuedDataElement value) {
            SingleValuedDataElement prior = (SingleValuedDataElement) old;
            return value.toBuilder().dictionaryReference(prior.getDictionaryReference())
                    .valueDataType(prior.getValueDataType()).build();
        }
        if (fresh instanceof DataElementCollection collection) {
            return collection.toBuilder().dictionaryReference(old.getDictionaryReference())
                    .dataElements(restoreElements(collection.getDataElements(),
                            ((DataElementCollection) old).getDataElements())).build();
        }
        if (fresh instanceof MultiValuedDataElement multi) {
            MultiValuedDataElement prior = (MultiValuedDataElement) old;
            List<DataElement> values = new ArrayList<>();
            for (int i = 0; i < multi.getDataElements().size(); i++) {
                DataElement item = multi.getDataElements().get(i);
                DataElement priorItem = i < prior.getDataElements().size() ? prior.getDataElements().get(i) : null;
                if (priorItem != null) {
                    item = withElementId(restoreElement(item, priorItem), priorItem.getElementId());
                }
                values.add(item);
            }
            return multi.toBuilder().dictionaryReference(prior.getDictionaryReference())
                    .valueDataType(prior.getValueDataType()).dataElements(values).build();
        }
        if (fresh instanceof RelatedResource resource) {
            return resource.toBuilder().dictionaryReference(old.getDictionaryReference()).build();
        }
        MultiLanguageDataElement multilingual = (MultiLanguageDataElement) fresh;
        return multilingual.toBuilder().dictionaryReference(old.getDictionaryReference()).build();
    }

    private DataElement restoreElement(DataElement fresh, DataElement old, SemanticResolver resolver,
            List<String> contentSpecificationIds, SemanticPath semanticPath) {
        if (old == null) return fresh;
        SemanticDefinition definition = resolver == null ? null
                : resolver.resolve(contentSpecificationIds, semanticPath).orElse(null);
        if (old instanceof SingleValuedDataElement prior && fresh.getClass() != old.getClass()) {
            if (definition != null && definition.kind().isPresent()) {
                throw invalid("$." + semanticPath, "resolved kind conflicts with the stored DataElement kind");
            }
            JsonNode operational = compressedValue(fresh, "$element");
            return prior.toBuilder().value(javaValue(operational, "$element")).build();
        }
        if (fresh.getClass() != old.getClass()) {
            throw invalid("$." + semanticPath, "compressed value cannot change the stored DataElement subtype");
        }
        String dictionaryReference = definition != null && definition.dictionaryReferenceResolved()
                ? definition.dictionaryReference().orElse(null) : old.getDictionaryReference();
        if (fresh instanceof SingleValuedDataElement value) {
            SingleValuedDataElement prior = (SingleValuedDataElement) old;
            String valueDataType = definition != null && definition.valueDataTypeResolved()
                    ? definition.valueDataType().orElse(null) : prior.getValueDataType();
            return value.toBuilder().dictionaryReference(dictionaryReference).valueDataType(valueDataType).build();
        }
        if (fresh instanceof DataElementCollection collection) {
            DataElementCollection prior = (DataElementCollection) old;
            List<DataElement> children = new ArrayList<>(collection.getDataElements().size());
            Map<String, DataElement> previousChildren = elementsById(prior.getDataElements());
            for (DataElement child : collection.getDataElements()) {
                children.add(restoreElement(child, previousChildren.get(child.getElementId()), resolver,
                        contentSpecificationIds, semanticPath.child(child.getElementId())));
            }
            return collection.toBuilder().dictionaryReference(dictionaryReference).dataElements(children).build();
        }
        if (fresh instanceof MultiValuedDataElement multi) {
            MultiValuedDataElement prior = (MultiValuedDataElement) old;
            String valueDataType = definition != null && definition.valueDataTypeResolved()
                    ? definition.valueDataType().orElse(null) : prior.getValueDataType();
            List<DataElement> items = new ArrayList<>(multi.getDataElements().size());
            for (int index = 0; index < multi.getDataElements().size(); index++) {
                DataElement item = multi.getDataElements().get(index);
                DataElement previousItem = index < prior.getDataElements().size() ? prior.getDataElements().get(index) : null;
                SemanticPath itemPath = semanticPath.child(Integer.toString(index));
                DataElement restoredItem = restoreElement(item, previousItem, resolver, contentSpecificationIds, itemPath);
                boolean hasResolvedId = resolver != null && resolver.resolve(contentSpecificationIds, itemPath)
                        .flatMap(SemanticDefinition::elementId).isPresent();
                if (previousItem != null && !hasResolvedId) restoredItem = withElementId(restoredItem, previousItem.getElementId());
                items.add(restoredItem);
            }
            return multi.toBuilder().dictionaryReference(dictionaryReference).valueDataType(valueDataType)
                    .dataElements(items).build();
        }
        if (fresh instanceof RelatedResource resource) {
            return resource.toBuilder().dictionaryReference(dictionaryReference).build();
        }
        MultiLanguageDataElement languages = (MultiLanguageDataElement) fresh;
        return languages.toBuilder().dictionaryReference(dictionaryReference).build();
    }

    private static DataElement withElementId(DataElement value, String id) {
        if (value instanceof SingleValuedDataElement single) return single.toBuilder().elementId(id).build();
        if (value instanceof DataElementCollection collection) return collection.toBuilder().elementId(id).build();
        if (value instanceof MultiValuedDataElement multi) return multi.toBuilder().elementId(id).build();
        if (value instanceof RelatedResource resource) return resource.toBuilder().elementId(id).build();
        return ((MultiLanguageDataElement) value).toBuilder().elementId(id).build();
    }

    private DataElement expandElement(String id, JsonNode value, String path,
            List<CompressedExpansionResult.ExpansionIssue> issues, DataElement previous) {
        if (id == null || id.isBlank()) throw invalid(path, "elementId must not be blank");
        return expandElement(id, value, path, issues, previous, null, List.of(), SemanticPath.of(id));
    }

    private DataElement expandElement(String id, JsonNode value, String path,
            List<CompressedExpansionResult.ExpansionIssue> issues, DataElement previous,
            SemanticResolver resolver, List<String> contentSpecificationIds, SemanticPath semanticPath) {
        if (id == null || id.isBlank()) throw invalid(path, "elementId must not be blank");
        if (value == null || value.isNull()) throw invalid(path, "null cannot form a required DataElement value");
        Optional<SemanticDefinition> definition = resolver == null ? Optional.empty()
                : Objects.requireNonNull(resolver.resolve(contentSpecificationIds, semanticPath),
                        "semantic resolver result");
        SemanticDefinition resolved = definition.orElse(null);
        if (resolved != null) {
            validateResolvedDefinition(path, id, resolved, previous);
        }
        int issueStart = issues.size();
        DataElement expanded = resolved != null && resolved.kind().isPresent()
                ? expandResolvedElement(id, value, path, issues, resolver, contentSpecificationIds,
                        semanticPath, previous, resolved)
                : expandElementBestEffort(id, value, path, issues, previous, resolver,
                        contentSpecificationIds, semanticPath);
        if (resolved != null) {
            expanded = applyDefinition(expanded, resolved);
            if (resolved.dictionaryReferenceResolved()) {
                removeIssues(issues, issueStart, path, "dictionaryReference is absent from operational JSON");
            }
            if (resolved.valueDataTypeResolved()) {
                removeIssues(issues, issueStart, path, "valueDataType is absent from operational JSON");
            }
        }
        return expanded;
    }

    private DataElement expandElementBestEffort(String id, JsonNode value, String path,
            List<CompressedExpansionResult.ExpansionIssue> issues, DataElement previous,
            SemanticResolver resolver, List<String> contentSpecificationIds, SemanticPath semanticPath) {
        if (previous instanceof SingleValuedDataElement single && value.isContainerNode()) {
            issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                    "object or array subtype was retained from the stored single-valued element"));
            return single.toBuilder().value(javaValue(value, path)).build();
        }
        issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                "dictionaryReference is absent from operational JSON"));
        if (value instanceof ObjectNode object) {
            if (!(previous instanceof DataElementCollection) && object.has("contentType") && object.has("url")) {
                issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                        "RelatedResource was inferred from contentType/url; an object-valued single element is also possible"));
                Set<String> resourceFields = Set.of("contentType", "url", "language", "resourceTitle");
                Iterator<String> names = object.fieldNames();
                while (names.hasNext()) {
                    if (!resourceFields.contains(names.next())) {
                        issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                                "resource-like object has additional fields; retained as a single JSON value"));
                        return new SingleValuedDataElement.Builder().elementId(id)
                                .value(javaValue(object, path)).build();
                    }
                }
                return new RelatedResource.Builder().elementId(id)
                        .contentType(requiredText(object, "contentType", path))
                        .url(requiredText(object, "url", path))
                        .language(optionalText(object, "language", path))
                        .resourceTitle(optionalText(object, "resourceTitle", path)).build();
            }
            if (object.isEmpty()) {
                issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                        "empty object inferred as a single value because a collection requires an element"));
                return new SingleValuedDataElement.Builder().elementId(id).value(Map.of()).build();
            }
            issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                    "DataElementCollection was inferred from an object; an object-valued single element is also possible"));
            List<DataElement> children = new ArrayList<>();
            Map<String, DataElement> previousChildren = previous instanceof DataElementCollection collection
                    ? elementsById(collection.getDataElements()) : Map.of();
            object.fields().forEachRemaining(field -> children.add(expandElement(
                    field.getKey(), field.getValue(), path + ".'" + field.getKey() + "'", issues,
                    previousChildren.get(field.getKey()), resolver, contentSpecificationIds,
                    semanticPath.child(field.getKey()))));
            return new DataElementCollection.Builder().elementId(id).dataElements(children).build();
        }
        if (value instanceof ArrayNode array) {
            if (array.isEmpty()) {
                issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                        "empty array inferred as a single value because a multi-valued element requires an item"));
                return new SingleValuedDataElement.Builder().elementId(id).value(List.of()).build();
            }
            if (!(previous instanceof MultiValuedDataElement) && isMultilingualArray(array)) {
                issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                        "MultiLanguageDataElement was inferred from language/value items; another array subtype is possible"));
                List<MultiLanguageValue> values = new ArrayList<>();
                for (int i = 0; i < array.size(); i++) {
                    ObjectNode item = (ObjectNode) array.get(i);
                    values.add(new MultiLanguageValue.Builder()
                            .language(requiredText(item, "language", path + "[" + i + "]"))
                            .value(requiredText(item, "value", path + "[" + i + "]")).build());
                }
                return new MultiLanguageDataElement.Builder().elementId(id).values(values).build();
            }
            JsonNodeType expectedType = array.get(0).getNodeType();
            issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                    "MultiValuedDataElement was inferred from an array; an array-valued single element is also possible"));
            List<DataElement> children = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                if (array.get(i).getNodeType() != expectedType) {
                    issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                            "mixed array items retained as a single JSON value because a multi-valued element requires like-typed items"));
                    return new SingleValuedDataElement.Builder().elementId(id)
                            .value(javaValue(array, path)).build();
                }
                DataElement previousItem = previous instanceof MultiValuedDataElement multi
                        && i < multi.getDataElements().size() ? multi.getDataElements().get(i) : null;
                children.add(expandElement(Integer.toString(i), array.get(i), path + "[" + i + "]",
                        issues, previousItem, resolver, contentSpecificationIds,
                        semanticPath.child(Integer.toString(i))));
            }
            issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                    "original array-item elementIds are absent; positional identifiers were used"));
            issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                    "valueDataType is absent from operational JSON"));
            return new MultiValuedDataElement.Builder().elementId(id).dataElements(children).build();
        }
        issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                "valueDataType is absent from operational JSON"));
        return new SingleValuedDataElement.Builder().elementId(id).value(javaValue(value, path)).build();
    }

    private DataElement expandResolvedElement(String id, JsonNode value, String path,
            List<CompressedExpansionResult.ExpansionIssue> issues, SemanticResolver resolver,
            List<String> contentSpecificationIds, SemanticPath semanticPath, DataElement previous,
            SemanticDefinition definition) {
        DataElementKind kind = definition.kind().orElseThrow();
        String resolvedId = definition.elementId().orElse(id);
        String dictionaryReference = definition.dictionaryReference().orElse(null);
        String valueDataType = definition.valueDataType().orElse(null);
        if (dictionaryReference == null && !definition.dictionaryReferenceResolved()) {
            issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                    "dictionaryReference is absent from operational JSON"));
        }
        return switch (kind) {
            case SINGLE -> {
                if (valueDataType == null && !definition.valueDataTypeResolved()) addMissingValueDataTypeIssue(issues, path);
                yield new SingleValuedDataElement.Builder().elementId(resolvedId)
                        .dictionaryReference(dictionaryReference).valueDataType(valueDataType)
                        .value(javaValue(value, path)).build();
            }
            case COLLECTION -> {
                if (!(value instanceof ObjectNode object)) {
                    throw invalid(path, "semantic definition expects a DataElementCollection object");
                }
                List<DataElement> children = new ArrayList<>();
                Map<String, DataElement> previousChildren = previous instanceof DataElementCollection collection
                        ? elementsById(collection.getDataElements()) : Map.of();
                object.fields().forEachRemaining(field -> children.add(expandElement(
                        field.getKey(), field.getValue(), path + ".'" + field.getKey() + "'", issues,
                        previousChildren.get(field.getKey()), resolver, contentSpecificationIds,
                        semanticPath.child(field.getKey()))));
                yield new DataElementCollection.Builder().elementId(resolvedId)
                        .dictionaryReference(dictionaryReference).dataElements(children).build();
            }
            case MULTI_VALUED -> {
                if (!(value instanceof ArrayNode array) || array.isEmpty()) {
                    throw invalid(path, "semantic definition expects a non-empty MultiValuedDataElement array");
                }
                JsonNodeType itemType = array.get(0).getNodeType();
                for (JsonNode item : array) {
                    if (item.getNodeType() != itemType) {
                        throw invalid(path, "resolved MultiValuedDataElement requires like-typed array items");
                    }
                }
                if (valueDataType == null && !definition.valueDataTypeResolved()) addMissingValueDataTypeIssue(issues, path);
                List<DataElement> children = new ArrayList<>();
                for (int index = 0; index < array.size(); index++) {
                    DataElement previousItem = previous instanceof MultiValuedDataElement multi
                            && index < multi.getDataElements().size() ? multi.getDataElements().get(index) : null;
                    DataElement child = expandElement(Integer.toString(index), array.get(index),
                            path + "[" + index + "]", issues, previousItem, resolver,
                            contentSpecificationIds, semanticPath.child(Integer.toString(index)));
                    children.add(child);
                }
                if (children.stream().anyMatch(child -> child.getElementId().matches("\\d+"))) {
                    issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                            "original array-item elementIds are absent; positional identifiers were used"));
                }
                yield new MultiValuedDataElement.Builder().elementId(resolvedId)
                        .dictionaryReference(dictionaryReference).valueDataType(valueDataType)
                        .dataElements(children).build();
            }
            case MULTI_LANGUAGE -> {
                if (!(value instanceof ArrayNode array) || !isMultilingualArray(array)) {
                    throw invalid(path, "semantic definition expects language/value array items");
                }
                List<MultiLanguageValue> values = new ArrayList<>();
                for (int index = 0; index < array.size(); index++) {
                    ObjectNode item = (ObjectNode) array.get(index);
                    values.add(new MultiLanguageValue.Builder()
                            .language(requiredText(item, "language", path + "[" + index + "]"))
                            .value(requiredText(item, "value", path + "[" + index + "]")).build());
                }
                yield new MultiLanguageDataElement.Builder().elementId(resolvedId)
                        .dictionaryReference(dictionaryReference).values(values).build();
            }
            case RELATED_RESOURCE -> {
                if (!(value instanceof ObjectNode object) || !object.has("contentType") || !object.has("url")) {
                    throw invalid(path, "semantic definition expects a RelatedResource object with contentType and url");
                }
                Set<String> resourceFields = Set.of("contentType", "url", "language", "resourceTitle");
                Iterator<String> names = object.fieldNames();
                while (names.hasNext()) {
                    if (!resourceFields.contains(names.next())) {
                        throw invalid(path, "resolved RelatedResource contains an unsupported field");
                    }
                }
                yield new RelatedResource.Builder().elementId(resolvedId)
                        .dictionaryReference(dictionaryReference)
                        .contentType(requiredText(object, "contentType", path))
                        .url(requiredText(object, "url", path))
                        .language(optionalText(object, "language", path))
                        .resourceTitle(optionalText(object, "resourceTitle", path)).build();
            }
        };
    }

    private static void validateResolvedDefinition(
            String path, String compressedId, SemanticDefinition definition, DataElement previous) {
        String resolvedId = definition.elementId().orElse(null);
        if (resolvedId != null && !compressedId.equals(resolvedId) && !compressedId.matches("\\d+")) {
            throw invalid(path, "semantic definition elementId conflicts with the compressed elementId");
        }
        if (previous == null) return;
        if (definition.kind().isPresent() && kindOf(previous) != definition.kind().get()) {
            throw invalid(path, "semantic definition conflicts with the stored DataElement kind");
        }
        if (resolvedId != null && !resolvedId.equals(previous.getElementId())) {
            throw invalid(path, "semantic definition conflicts with the stored elementId");
        }
        if (definition.dictionaryReference().isPresent() && previous.getDictionaryReference() != null
                && !definition.dictionaryReference().get().equals(previous.getDictionaryReference())) {
            throw invalid(path, "semantic definition conflicts with the stored dictionaryReference");
        }
        if (definition.dictionaryReferenceResolved() && definition.dictionaryReference().isEmpty()
                && previous.getDictionaryReference() != null) {
            throw invalid(path, "semantic definition conflicts with the stored dictionaryReference");
        }
        String previousDataType = valueDataType(previous);
        if (definition.valueDataType().isPresent() && previousDataType != null
                && !definition.valueDataType().get().equals(previousDataType)) {
            throw invalid(path, "semantic definition conflicts with the stored valueDataType");
        }
        if (definition.valueDataTypeResolved() && definition.valueDataType().isEmpty() && previousDataType != null) {
            throw invalid(path, "semantic definition conflicts with the stored valueDataType");
        }
    }

    private static DataElementKind kindOf(DataElement element) {
        if (element instanceof DataElementCollection) return DataElementKind.COLLECTION;
        if (element instanceof SingleValuedDataElement) return DataElementKind.SINGLE;
        if (element instanceof MultiValuedDataElement) return DataElementKind.MULTI_VALUED;
        if (element instanceof MultiLanguageDataElement) return DataElementKind.MULTI_LANGUAGE;
        return DataElementKind.RELATED_RESOURCE;
    }

    private static String valueDataType(DataElement element) {
        if (element instanceof SingleValuedDataElement single) return single.getValueDataType();
        if (element instanceof MultiValuedDataElement multi) return multi.getValueDataType();
        return null;
    }

    private static DataElement applyDefinition(DataElement element, SemanticDefinition definition) {
        String elementId = definition.elementId().orElse(element.getElementId());
        String dictionaryReference = definition.dictionaryReferenceResolved()
                ? definition.dictionaryReference().orElse(null) : element.getDictionaryReference();
        String valueDataType = definition.valueDataType().orElse(null);
        if (definition.valueDataType().isPresent()
                && !(element instanceof SingleValuedDataElement || element instanceof MultiValuedDataElement)) {
            throw invalid("$.'" + element.getElementId() + "'", "valueDataType is not valid for the resolved DataElement kind");
        }
        if (element instanceof SingleValuedDataElement single) {
            return single.toBuilder().elementId(elementId).dictionaryReference(dictionaryReference)
                    .valueDataType(definition.valueDataTypeResolved() ? valueDataType : single.getValueDataType()).build();
        }
        if (element instanceof DataElementCollection collection) {
            return collection.toBuilder().elementId(elementId).dictionaryReference(dictionaryReference).build();
        }
        if (element instanceof MultiValuedDataElement multi) {
            return multi.toBuilder().elementId(elementId).dictionaryReference(dictionaryReference)
                    .valueDataType(definition.valueDataTypeResolved() ? valueDataType : multi.getValueDataType()).build();
        }
        if (element instanceof MultiLanguageDataElement multilingual) {
            return multilingual.toBuilder().elementId(elementId).dictionaryReference(dictionaryReference).build();
        }
        RelatedResource resource = (RelatedResource) element;
        return resource.toBuilder().elementId(elementId).dictionaryReference(dictionaryReference).build();
    }

    private static void removeIssues(List<CompressedExpansionResult.ExpansionIssue> issues,
            int start, String path, String reason) {
        for (int index = issues.size() - 1; index >= start; index--) {
            CompressedExpansionResult.ExpansionIssue issue = issues.get(index);
            if (issue.path().equals(path) && issue.reason().equals(reason)) issues.remove(index);
        }
    }

    private static void addMissingValueDataTypeIssue(
            List<CompressedExpansionResult.ExpansionIssue> issues, String path) {
        issues.add(new CompressedExpansionResult.ExpansionIssue(path,
                "valueDataType is absent from operational JSON"));
    }

    private static boolean isMultilingualArray(ArrayNode array) {
        for (JsonNode item : array) {
            if (!(item instanceof ObjectNode object) || object.size() != 2
                    || !object.path("language").isTextual() || !object.path("value").isTextual()) {
                return false;
            }
        }
        return true;
    }

    private ObjectNode headerNode(DppHeader header) {
        if (header == null) {
            throw new DppJsonException("DPP header is required");
        }
        ObjectNode root = NODES.objectNode();
        putRequiredText(root, "digitalProductPassportId", header.getDigitalProductPassportId());
        putRequiredText(root, "uniqueProductIdentifier", header.getUniqueProductIdentifier());
        if (header.getGranularity() == null) {
            throw invalid("$.granularity", "is required");
        }
        root.put("granularity", switch (header.getGranularity()) {
                case MODEL -> "model";
                case BATCH -> "batch";
                case ITEM -> "item";
        });
        putRequiredText(root, "dppSchemaVersion", header.getDppSchemaVersion());
        putRequiredText(root, "dppStatus", header.getDppStatus());
        if (header.getLastUpdated() == null) throw invalid("$.lastUpdated", "is required");
        root.put("lastUpdated", header.getLastUpdated().toString());
        putRequiredText(root, "economicOperatorId", header.getEconomicOperatorId());
        putText(root, "facilityId", header.getFacilityId());
        ArrayNode specifications = root.putArray("contentSpecificationIds");
        for (String id : header.getContentSpecificationIds()) {
            if (id == null) {
                throw new DppJsonException("contentSpecificationIds must not contain null");
            }
            specifications.add(id);
        }
        return root;
    }

    private ObjectNode expandedElement(DataElement element) {
        if (element == null) {
            throw new DppJsonException("DataElement must not be null");
        }
        ObjectNode node = NODES.objectNode();
        node.put("elementId", requireElementId(element, "expanded element"));
        node.put("objectType", objectType(element));
        putText(node, "dictionaryReference", element.getDictionaryReference());
        if (element instanceof DataElementCollection collection) {
            requireNonEmpty(collection.getDataElements(), "DataElementCollection.elements");
            ArrayNode children = node.putArray("elements");
            for (DataElement child : collection.getDataElements()) {
                children.add(expandedElement(child));
            }
        } else if (element instanceof SingleValuedDataElement single) {
            putText(node, "valueDataType", single.getValueDataType());
            if (single.getValue() == null) {
                node.putNull("value");
            } else {
                node.set("value", jsonValue(single.getValue(), "$." + element.getElementId() + ".value"));
            }
        } else if (element instanceof MultiValuedDataElement multi) {
            requireNonEmpty(multi.getDataElements(), "MultiValuedDataElement.value");
            putText(node, "valueDataType", multi.getValueDataType());
            ArrayNode children = node.putArray("value");
            for (DataElement child : multi.getDataElements()) {
                children.add(expandedElement(child));
            }
        } else if (element instanceof RelatedResource resource) {
            putRequiredText(node, "contentType", resource.getContentType());
            putRequiredText(node, "url", resource.getUrl());
            putText(node, "language", resource.getLanguage());
            putText(node, "resourceTitle", resource.getResourceTitle());
        } else if (element instanceof MultiLanguageDataElement multilingual) {
            requireNonEmpty(multilingual.getValues(), "MultiLanguageDataElement.value");
            ArrayNode values = node.putArray("value");
            for (MultiLanguageValue value : multilingual.getValues()) {
                if (value == null) {
                    throw new DppJsonException("MultiLanguageDataElement values must not contain null");
                }
                ObjectNode entry = values.addObject();
                putRequiredText(entry, "value", value.getValue());
                putRequiredText(entry, "language", value.getLanguage());
            }
        } else {
            throw new DppJsonException("Unsupported DataElement subtype: " + element.getClass().getName());
        }
        return node;
    }

    private JsonNode compressedValue(DataElement element, String path) {
        if (element instanceof SingleValuedDataElement single) {
            return jsonValue(single.getValue(), path);
        }
        if (element instanceof DataElementCollection collection) {
            requireNonEmpty(collection.getDataElements(), path);
            ObjectNode object = NODES.objectNode();
            for (DataElement child : collection.getDataElements()) {
                String childId = requireElementId(child, path);
                if (object.has(childId)) {
                    throw invalid(path + "." + childId, "duplicate compressed element key");
                }
                object.set(childId, compressedValue(child, path + "." + childId));
            }
            return object;
        }
        if (element instanceof MultiValuedDataElement multi) {
            requireNonEmpty(multi.getDataElements(), path);
            ArrayNode array = NODES.arrayNode();
            JsonNodeType expectedType = null;
            for (int i = 0; i < multi.getDataElements().size(); i++) {
                DataElement child = multi.getDataElements().get(i);
                String childPath = path + "[" + i + "]";
                JsonNode value = compressedValue(child, childPath);
                JsonNodeType currentType = value.getNodeType();
                if (expectedType == null) {
                    expectedType = currentType;
                } else if (currentType != expectedType) {
                    throw invalid(childPath,
                            "MultiValuedDataElement items must have the same JSON datatype");
                }
                array.add(value);
            }
            return array;
        }
        if (element instanceof RelatedResource resource) {
            ObjectNode object = NODES.objectNode();
            putText(object, "resourceTitle", resource.getResourceTitle());
            putRequiredText(object, "contentType", resource.getContentType());
            putRequiredText(object, "url", resource.getUrl());
            putText(object, "language", resource.getLanguage());
            return object;
        }
        if (element instanceof MultiLanguageDataElement multilingual) {
            requireNonEmpty(multilingual.getValues(), path);
            ArrayNode values = NODES.arrayNode();
            for (MultiLanguageValue value : multilingual.getValues()) {
                if (value == null) {
                    throw new DppJsonException(path + " must not contain null values");
                }
                ObjectNode entry = values.addObject();
                putRequiredText(entry, "value", value.getValue());
                putRequiredText(entry, "language", value.getLanguage());
            }
            return values;
        }
        throw new DppJsonException(path + " has an unsupported DataElement subtype");
    }

    private DataElement readElement(JsonNode node, String path) {
        if (!(node instanceof ObjectNode object)) {
            throw invalid(path, "must be an object");
        }
        String type = requiredText(object, "objectType", path);
        String id = requiredText(object, "elementId", path);
        String dictionaryReference = optionalText(object, "dictionaryReference", path);
        return switch (type) {
            case "DataElementCollection" -> {
                checkKnownFields(object, COMMON_ELEMENT_FIELDS, Set.of("elements"), path);
                ArrayNode children = requiredArray(object, "elements", path);
                requireNonEmpty(children, path + ".elements");
                yield new DataElementCollection.Builder().elementId(id)
                        .dictionaryReference(dictionaryReference)
                        .dataElements(readElements(children, path + ".elements"))
                        .build();
            }
            case "SingleValuedDataElement" -> {
                checkKnownFields(object, COMMON_ELEMENT_FIELDS,
                        Set.of("valueDataType", "value"), path);
                if (!object.has("value")) throw invalid(path + ".value", "is required");
                yield new SingleValuedDataElement.Builder().elementId(id)
                        .dictionaryReference(dictionaryReference)
                        .valueDataType(optionalText(object, "valueDataType", path))
                        .value(javaValue(object.get("value"), path + ".value")).build();
            }
            case "MultiValuedDataElement" -> readMultiValued(object, id, dictionaryReference, path);
            case "RelatedResource" -> {
                Set<String> fields = Set.of("contentType", "url", "language", "resourceTitle");
                checkKnownFields(object, COMMON_ELEMENT_FIELDS, fields, path);
                yield new RelatedResource.Builder().elementId(id)
                        .dictionaryReference(dictionaryReference)
                        .contentType(requiredText(object, "contentType", path))
                        .url(requiredText(object, "url", path))
                        .language(optionalText(object, "language", path))
                        .resourceTitle(optionalText(object, "resourceTitle", path)).build();
            }
            case "MultiLanguageDataElement" -> {
                checkKnownFields(object, COMMON_ELEMENT_FIELDS, Set.of("value"), path);
                ArrayNode values = requiredArray(object, "value", path);
                requireNonEmpty(values, path + ".value");
                List<MultiLanguageValue> parsedValues = new ArrayList<>(values.size());
                for (int i = 0; i < values.size(); i++) {
                    String entryPath = path + ".value[" + i + "]";
                    if (!(values.get(i) instanceof ObjectNode entry)) {
                        throw invalid(entryPath, "must be an object");
                    }
                    checkKnownFields(entry, Set.of(), Set.of("value", "language"), entryPath);
                    parsedValues.add(new MultiLanguageValue.Builder()
                            .value(requiredText(entry, "value", entryPath))
                            .language(requiredText(entry, "language", entryPath)).build());
                }
                yield new MultiLanguageDataElement.Builder().elementId(id)
                        .dictionaryReference(dictionaryReference).values(parsedValues).build();
            }
            default -> throw invalid(path + ".objectType", "unknown objectType '" + type + "'");
        };
    }

    private DataElement readMultiValued(
            ObjectNode object, String id, String dictionaryReference, String path) {
        checkKnownFields(object, COMMON_ELEMENT_FIELDS,
                Set.of("valueDataType", "elements", "value"), path);
        boolean hasElements = object.has("elements");
        boolean hasValue = object.has("value");
        if (hasElements == hasValue) {
            throw invalid(path, "MultiValuedDataElement must contain exactly one of 'elements' or 'value'");
        }
        JsonNode arrayNode = object.get(hasElements ? "elements" : "value");
        if (!arrayNode.isArray()) {
            throw invalid(path + "." + (hasElements ? "elements" : "value"), "must be an array");
        }
        requireNonEmpty((ArrayNode) arrayNode, path + "." + (hasElements ? "elements" : "value"));
        return new MultiValuedDataElement.Builder().elementId(id)
                .dictionaryReference(dictionaryReference)
                .valueDataType(optionalText(object, "valueDataType", path))
                .dataElements(readElements((ArrayNode) arrayNode,
                        path + "." + (hasElements ? "elements" : "value")))
                .build();
    }

    private List<DataElement> readElements(ArrayNode array, String path) {
        List<DataElement> result = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            result.add(readElement(array.get(i), path + "[" + i + "]"));
        }
        return result;
    }

    private DppHeader readHeader(ObjectNode root) {
        Granularity granularity;
        String granularityText = requiredText(root, "granularity", "$");
        try {
            granularity = Granularity.valueOf(granularityText.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DppJsonException("$.granularity has an unknown value '" + granularityText + "'", e);
        }
        Instant updated;
        String updatedText = requiredText(root, "lastUpdated", "$");
        try {
            updated = Instant.parse(updatedText);
        } catch (DateTimeParseException e) {
            throw new DppJsonException("$.lastUpdated must be an ISO-8601 instant", e);
        }
        if (!updated.toString().equals(updatedText)) {
            throw invalid("$.lastUpdated", "must use canonical UTC ISO-8601 Instant representation");
        }
        List<String> specifications = new ArrayList<>();
        JsonNode specsNode = root.get("contentSpecificationIds");
        if (specsNode != null) {
            if (!specsNode.isArray()) throw invalid("$.contentSpecificationIds", "must be an array");
            for (int i = 0; i < specsNode.size(); i++) {
                JsonNode item = specsNode.get(i);
                if (!item.isTextual()) throw invalid("$.contentSpecificationIds[" + i + "]", "must be a string");
                specifications.add(item.textValue());
            }
        }
        return new DppHeader.Builder()
                .digitalProductPassportId(requiredText(root, "digitalProductPassportId", "$"))
                .uniqueProductIdentifier(requiredText(root, "uniqueProductIdentifier", "$"))
                .granularity(granularity)
                .dppSchemaVersion(requiredText(root, "dppSchemaVersion", "$"))
                .dppStatus(requiredText(root, "dppStatus", "$"))
                .lastUpdated(updated)
                .economicOperatorId(requiredText(root, "economicOperatorId", "$"))
                .facilityId(optionalText(root, "facilityId", "$"))
                .contentSpecificationIds(specifications).build();
    }

    private JsonNode jsonValue(Object value, String path) {
        if (value == null) return NODES.nullNode();
        if (value instanceof String text) return NODES.textNode(text);
        if (value instanceof Boolean bool) return NODES.booleanNode(bool);
        if (value instanceof Byte number) return NODES.numberNode(number.intValue());
        if (value instanceof Short number) return NODES.numberNode(number.intValue());
        if (value instanceof Integer number) return NODES.numberNode(number);
        if (value instanceof Long number) return NODES.numberNode(number);
        if (value instanceof BigInteger number) return NODES.numberNode(number);
        if (value instanceof BigDecimal number) return NODES.numberNode(number);
        if (value instanceof Float number) {
            if (!Float.isFinite(number)) throw invalid(path, "number must be finite");
            return NODES.numberNode(number);
        }
        if (value instanceof Double number) {
            if (!Double.isFinite(number)) throw invalid(path, "number must be finite");
            return NODES.numberNode(number);
        }
        if (value instanceof List<?> list) {
            ArrayNode array = NODES.arrayNode();
            for (int i = 0; i < list.size(); i++) array.add(jsonValue(list.get(i), path + "[" + i + "]"));
            return array;
        }
        if (value instanceof Map<?, ?> map) {
            ObjectNode object = NODES.objectNode();
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) throw invalid(path, "object keys must be strings");
                sorted.put(key, entry.getValue());
            }
            for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                String key = entry.getKey();
                object.set(key, jsonValue(entry.getValue(), path + "." + key));
            }
            return object;
        }
        throw invalid(path, "unsupported JSON value type " + value.getClass().getName());
    }

    private Object javaValue(JsonNode node, String path) {
        if (node == null || node.isNull()) return null;
        if (node.isTextual()) return node.textValue();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isIntegralNumber()) {
            if (node.canConvertToInt()) return node.intValue();
            if (node.canConvertToLong()) return node.longValue();
            return node.bigIntegerValue();
        }
        if (node.isFloatingPointNumber()) return node.decimalValue();
        if (node.isArray()) {
            List<Object> values = new ArrayList<>(node.size());
            for (int i = 0; i < node.size(); i++) values.add(javaValue(node.get(i), path + "[" + i + "]"));
            return values;
        }
        if (node instanceof ObjectNode object) {
            Map<String, Object> values = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                values.put(field.getKey(), javaValue(field.getValue(), path + "." + field.getKey()));
            }
            return values;
        }
        throw invalid(path, "unsupported JSON value");
    }

    private static String objectType(DataElement element) {
        if (element instanceof DataElementCollection) return "DataElementCollection";
        if (element instanceof SingleValuedDataElement) return "SingleValuedDataElement";
        if (element instanceof MultiValuedDataElement) return "MultiValuedDataElement";
        if (element instanceof RelatedResource) return "RelatedResource";
        if (element instanceof MultiLanguageDataElement) return "MultiLanguageDataElement";
        throw new DppJsonException("Unsupported DataElement subtype: " + element.getClass().getName());
    }

    private static String requireElementId(DataElement element, String path) {
        if (element == null || element.getElementId() == null) {
            throw invalid(path, "elementId is required for compressed serialization");
        }
        return element.getElementId();
    }

    private static DigitalProductPassport requirePassport(DigitalProductPassport dpp) {
        if (dpp == null) throw new DppJsonException("DPP is required");
        return dpp;
    }

    private static void putText(ObjectNode node, String field, String value) {
        if (value != null) node.put(field, value);
    }

    private static void putRequiredText(ObjectNode node, String field, String value) {
        if (value == null) throw invalid("$." + field, "is required");
        node.put(field, value);
    }

    private static void requireNonEmpty(List<?> values, String path) {
        if (values.isEmpty()) throw invalid(path, "must contain at least one item");
    }

    private static void requireNonEmpty(ArrayNode values, String path) {
        if (values.isEmpty()) throw invalid(path, "must contain at least one item");
    }

    private static String requiredText(ObjectNode object, String field, String path) {
        JsonNode value = object.get(field);
        if (value == null || !value.isTextual()) throw invalid(path + "." + field, "must be a string");
        return value.textValue();
    }

    private static String optionalText(ObjectNode object, String field, String path) {
        JsonNode value = object.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw invalid(path + "." + field, "must be a string when present");
        return value.textValue();
    }

    private static ArrayNode requiredArray(ObjectNode object, String field, String path) {
        JsonNode value = object.get(field);
        if (!(value instanceof ArrayNode array)) throw invalid(path + "." + field, "must be an array");
        return array;
    }

    private static void checkKnownFields(
            ObjectNode object, Set<String> common, Set<String> subtype, String path) {
        Iterator<String> fields = object.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!common.contains(field) && !subtype.contains(field)) {
                throw invalid(path + "." + field, "unknown property");
            }
        }
    }

    private static DppJsonException invalid(String path, String detail) {
        return new DppJsonException(path + " " + detail);
    }
}
