package dppsdk.dpp4fun.model.internal;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.MultiLanguageValue;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.semantic.DataElementKind;
import dppsdk.core.semantic.SemanticPath;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile;
import dppsdk.dpp4fun.semantic.Dpp4FunElementDefinition;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Internal catalog-backed state for named typed aspect builders. Normal application code should use a named typed aspect
 * such as {@link Durability} or {@link ChairCharacteristics}.
 */
public final class ProfiledAspectState {
    private final String elementId;
    private final DataElementCollection element;
    private final Dpp4FunProductVariant variant;

    public ProfiledAspectState(String elementId, DataElement value) {
        this(elementId, value, null);
    }

    public ProfiledAspectState(String elementId, DataElement value, Dpp4FunProductVariant variant) {
        this.elementId = Objects.requireNonNull(elementId, "elementId");
        if (!(value instanceof DataElementCollection collection)) {
            throw new IllegalArgumentException(elementId + " aspect must be a DataElementCollection");
        }
        if (!elementId.equals(collection.getElementId())) {
            throw new IllegalArgumentException("Expected aspect elementId " + elementId);
        }
        element = collection;
        this.variant = variant;
    }

    public String elementId() { return elementId; }
    public Dpp4FunProductVariant variant() { return variant; }

    /** Returns the immutable canonical EN node represented by this aspect view. */
    public DataElementCollection asDataElement() { return element; }

    /** Returns a fixed semantic value by its catalog elementId. */
    public Object value(String elementId) {
        DataElement child = child(elementId);
        if (!(child instanceof SingleValuedDataElement scalar)) {
            throw new IllegalArgumentException(elementId + " is not a single-valued field in " + this.elementId);
        }
        return scalar.getValue();
    }

    public String text(String elementId) { return typedValue(elementId, String.class); }
    public Boolean bool(String elementId) { return typedValue(elementId, Boolean.class); }
    public Number number(String elementId) {
        Object value = value(elementId);
        if (value == null) return null;
        if (!(value instanceof Number number)) throw new IllegalArgumentException(elementId + " is not numeric");
        return number;
    }

    public List<MultiLanguageDataElement> languages() {
        return element.getDataElements().stream().filter(MultiLanguageDataElement.class::isInstance)
                .map(MultiLanguageDataElement.class::cast).toList();
    }

    public List<MultiValuedDataElement> lists() {
        return element.getDataElements().stream().filter(MultiValuedDataElement.class::isInstance)
                .map(MultiValuedDataElement.class::cast).toList();
    }

    public List<ProfiledAspectState> composites() {
        return element.getDataElements().stream().filter(DataElementCollection.class::isInstance)
                .map(DataElementCollection.class::cast)
                .map(collection -> new ProfiledAspectState(collection.getElementId(), collection)).toList();
    }

    /** Starts an immutable edit that retains the existing semantic subtree and extensions. */
    public Builder toBuilder() { return new Builder(elementId, variant, element); }

    public static Builder builder(KnownProfileAspect type) { return new Builder(type.elementId(), null); }
    public static Builder builder(Dpp4FunProductVariant variant) { return new Builder("characteristics", variant); }

    public static DataElementCollection mergeForEdit(DataElementCollection existing, DataElementCollection replacement,
            Dpp4FunProductVariant variant) {
        Builder builder = new Builder(replacement.getElementId(), variant, existing);
        return builder.merge(existing, replacement, List.of(replacement.getElementId()));
    }

    DataElementCollection element() { return element; }

    private DataElement child(String childId) {
        Objects.requireNonNull(childId, "elementId");
        return element.getDataElements().stream().filter(value -> childId.equals(value.getElementId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown aspect elementId: " + childId));
    }

    private <T> T typedValue(String id, Class<T> type) {
        Object value = value(id);
        if (value == null) return null;
        if (!type.isInstance(value)) throw new IllegalArgumentException(id + " has value type " + value.getClass().getSimpleName());
        return type.cast(value);
    }

    /** Catalog-driven construction for any frozen general aspect or Characteristics variant. */
    public static final class Builder {
        private final String rootId;
        private final Dpp4FunProductVariant variant;
        private final Dpp4FunSemanticProfile catalog = Dpp4FunSemanticProfile.loadDefault();
        private final List<String> contentSpecificationIds;
        private final Node root = new Node();
        private DataElementCollection base;

        private Builder(String rootId, Dpp4FunProductVariant variant) {
            this(rootId, variant, null);
        }

        private Builder(String rootId, Dpp4FunProductVariant variant, DataElementCollection base) {
            this.rootId = rootId;
            this.variant = variant;
            this.base = base;
            contentSpecificationIds = catalog.contentSpecificationIds(variant);
            if (catalog.definitionFor(contentSpecificationIds, SemanticPath.of(rootId)).isEmpty()) {
                throw new IllegalArgumentException("No Dpp4Fun binding for aspect " + rootId);
            }
        }

        /** Sets a source-defined scalar using its canonical relative semantic path. */
        public Builder field(String relativePath, Object value) {
            if (value == null) return this;
            List<String> segments = parsePath(relativePath);
            List<String> full = new ArrayList<>();
            full.add(rootId);
            full.addAll(segments);
            Dpp4FunElementDefinition binding = catalog.definitionFor(contentSpecificationIds,
                    semanticPath(full))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown Dpp4Fun binding path: " + String.join(".", full)));
            if (binding.kind() != DataElementKind.SINGLE) {
                throw new IllegalArgumentException("Path is not a single-valued field: " + String.join(".", full));
            }
            validateValue(binding, value);
            Node node = root;
            for (String segment : segments) node = node.children.computeIfAbsent(segment, ignored -> new Node());
            if (!node.children.isEmpty() || node.value != null || node.languages != null) {
                throw new IllegalArgumentException("Conflicting or duplicate field path: " + relativePath);
            }
            node.value = value;
            return this;
        }

        /** Sets a source-defined multi-language field; language tags and values are preserved as supplied. */
        public Builder languages(String relativePath, Map<String, String> values) {
            Objects.requireNonNull(values, "values");
            if (values.isEmpty()) return this;
            List<String> segments = parsePath(relativePath);
            List<String> full = new ArrayList<>();
            full.add(rootId);
            full.addAll(segments);
            Dpp4FunElementDefinition binding = catalog.definitionFor(contentSpecificationIds, semanticPath(full))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown Dpp4Fun binding path: " + String.join(".", full)));
            if (binding.kind() != DataElementKind.MULTI_LANGUAGE) {
                throw new IllegalArgumentException("Path is not a multi-language field: " + String.join(".", full));
            }
            Node node = root;
            for (String segment : segments) node = node.children.computeIfAbsent(segment, ignored -> new Node());
            if (!node.children.isEmpty() || node.value != null || node.languages != null) {
                throw new IllegalArgumentException("Conflicting or duplicate field path: " + relativePath);
            }
            node.languages = values.entrySet().stream()
                    .map(entry -> new MultiLanguageValue.Builder().language(entry.getKey()).value(entry.getValue()).build())
                    .toList();
            return this;
        }

        /** Imports a composite's values through the catalog so fixed EN metadata is rebuilt canonically. */
        public Builder importElement(String relativePath, DataElement value) {
            Objects.requireNonNull(value, "value");
            if (value instanceof SingleValuedDataElement scalar) {
                return field(relativePath, scalar.getValue());
            }
            if (value instanceof MultiLanguageDataElement multilingual) {
                Map<String, String> entries = new LinkedHashMap<>();
                multilingual.getValues().forEach(entry -> entries.put(entry.getLanguage(), entry.getValue()));
                return languages(relativePath, entries);
            }
            if (value instanceof DataElementCollection collection) {
                for (DataElement child : collection.getDataElements()) {
                    importElement(relativePath + "." + child.getElementId(), child);
                }
                return this;
            }
            if (value instanceof MultiValuedDataElement values) {
                for (int index = 0; index < values.getDataElements().size(); index++) {
                    importElement(relativePath + "[" + index + "]", values.getDataElements().get(index));
                }
                return this;
            }
            throw new IllegalArgumentException("Unsupported composite element kind at " + relativePath);
        }

        /** Replaces a direct repeated property while retaining unrelated children on the aspect. */
        public Builder replaceList(String relativePath) {
            List<String> segments = parsePath(relativePath);
            if (segments.size() != 1 || segments.get(0).contains("[")) {
                throw new IllegalArgumentException("Only a direct aspect list can be replaced: " + relativePath);
            }
            List<String> full = new ArrayList<>();
            full.add(rootId);
            full.addAll(segments);
            Dpp4FunElementDefinition binding = catalog.definitionFor(contentSpecificationIds, semanticPath(full))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown Dpp4Fun binding path: " + relativePath));
            if (binding.kind() != DataElementKind.MULTI_VALUED) {
                throw new IllegalArgumentException("Path is not a repeated D4.1 property: " + relativePath);
            }
            if (base != null) {
                List<DataElement> retained = base.getDataElements().stream()
                        .filter(element -> !segments.get(0).equals(element.getElementId())).toList();
                base = base.toBuilder().dataElements(retained).build();
            }
            return this;
        }

        /**
         * Binds a mapped base measurement to its catalog-defined unit property. Built-in unit identity can
         * be restored from the canonical symbol; a custom unit's URI is not represented in D4.1 string slots.
         */
        public Builder quantity(String relativePath, Dpp4FunQuantity<? extends SemanticUnit> quantity) {
            Objects.requireNonNull(quantity, "quantity");
            List<String> segments = parsePath(relativePath);
            List<String> full = new ArrayList<>();
            full.add(rootId);
            full.addAll(segments);
            Dpp4FunElementDefinition binding = catalog.definitionFor(contentSpecificationIds, semanticPath(full))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown Dpp4Fun quantity path: " + relativePath));
            if (binding.unitDimension() == null || binding.unitSlotPath() == null) {
                throw new IllegalArgumentException("No source-backed unit slot for quantity: " + relativePath);
            }
            String expectedDimension = binding.unitDimension().toUpperCase(java.util.Locale.ROOT);
            if (!quantity.unit().dimension().name().equals(expectedDimension)) {
                throw new IllegalArgumentException("DPP4FUN_UNIT_DIMENSION_MISMATCH: Quantity unit dimension at " + relativePath
                        + " must be " + binding.unitDimension());
            }
            if (SemanticUnit.known(quantity.unit().symbol(), quantity.unit().dimension())
                    .filter(quantity.unit()::equals).isEmpty()) {
                throw new IllegalArgumentException("DPP4FUN_UNIT_NOT_CANONICAL: Quantity unit at " + relativePath
                        + " must use a built-in unit whose identity is preserved by D4.1");
            }
            field(relativePath, quantity.value());
            String requestedSymbol = quantity.unit().symbol();
            Object currentSymbol = valueAt(binding.unitSlotPath());
            if (currentSymbol != null && !requestedSymbol.equals(currentSymbol)) {
                throw new IllegalArgumentException("DPP4FUN_UNIT_SLOT_CONFLICT: " + binding.unitSlotPath()
                        + " already uses " + currentSymbol + "; normalize all values before building");
            }
            if (currentSymbol == null) field(binding.unitSlotPath(), requestedSymbol);
            return this;
        }

        private Object valueAt(String relativePath) {
            Node node = root;
            for (String segment : parsePath(relativePath)) {
                node = node.children.get(segment);
                if (node == null) break;
            }
            if (node != null && node.value != null) return node.value;
            if (base != null) {
                DataElement existing = base.getDataElements().stream()
                        .filter(element -> relativePath.equals(element.getElementId())).findFirst().orElse(null);
                if (existing instanceof SingleValuedDataElement scalar) return scalar.getValue();
            }
            return null;
        }

        public ProfiledAspectState build() {
            DataElementCollection result = (DataElementCollection) new Dpp4FunBinder(catalog,
                    contentSpecificationIds).bind(rootId, root, base != null);
            if (base != null) result = merge(base, result, List.of(rootId));
            return new ProfiledAspectState(rootId, result, variant);
        }

        private static List<String> append(List<String> path, String value) {
            List<String> result = new ArrayList<>(path);
            result.add(value);
            return result;
        }

        private DataElementCollection merge(DataElementCollection original, DataElementCollection replacement,
                List<String> semanticSegments) {
            List<DataElement> merged = new ArrayList<>(original.getDataElements());
            for (DataElement next : replacement.getDataElements()) {
                List<String> childPath = append(semanticSegments, next.getElementId());
                int existingIndex = -1;
                for (int index = 0; index < merged.size(); index++) {
                    if (Objects.equals(merged.get(index).getElementId(), next.getElementId())) {
                        if (existingIndex >= 0) throw new IllegalArgumentException("Duplicate fixed profile elementId: " + next.getElementId());
                        existingIndex = index;
                    }
                }
                if (existingIndex < 0) merged.add(next);
                else {
                    DataElement previous = merged.get(existingIndex);
                    DataElement combined;
                    if (previous instanceof DataElementCollection oldCollection
                            && next instanceof DataElementCollection newCollection) {
                        combined = merge(oldCollection, newCollection, childPath);
                    } else if (previous instanceof MultiValuedDataElement oldValues
                            && next instanceof MultiValuedDataElement newValues) {
                        combined = merge(oldValues, newValues, childPath);
                    } else {
                        combined = next;
                    }
                    merged.set(existingIndex, combined);
                }
            }
            return replacement.toBuilder().dataElements(merged).build();
        }

        private MultiValuedDataElement merge(MultiValuedDataElement original, MultiValuedDataElement replacement,
                List<String> semanticSegments) {
            List<DataElement> merged = new ArrayList<>(original.getDataElements());
            for (DataElement next : replacement.getDataElements()) {
                int itemIndex = repeatedPosition(next.getElementId());
                List<String> itemPath = append(semanticSegments, Integer.toString(itemIndex));
                int existingIndex = -1;
                for (int index = 0; index < merged.size(); index++) {
                    if (Objects.equals(merged.get(index).getElementId(), next.getElementId())) {
                        if (existingIndex >= 0) throw new IllegalArgumentException("Duplicate repeated-item ID: " + next.getElementId());
                        existingIndex = index;
                    }
                }
                if (existingIndex < 0) merged.add(next);
                else {
                    DataElement previous = merged.get(existingIndex);
                    DataElement combined = previous instanceof DataElementCollection oldCollection
                            && next instanceof DataElementCollection newCollection
                            ? merge(oldCollection, newCollection, itemPath) : next;
                    merged.set(existingIndex, combined);
                }
            }
            merged.sort(java.util.Comparator.comparingInt(value -> repeatedPosition(value.getElementId())));
            for (int index = 0; index < merged.size(); index++) {
                if (repeatedPosition(merged.get(index).getElementId()) != index) {
                    throw new IllegalArgumentException("Repeated item indexes must remain contiguous from zero");
                }
            }
            return replacement.toBuilder().dataElements(merged).build();
        }

        private static int repeatedPosition(String elementId) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(".*?(\\d+)$").matcher(elementId);
            if (!matcher.matches()) throw new IllegalArgumentException("Repeated item id must end in its canonical index: " + elementId);
            return Integer.parseInt(matcher.group(1));
        }

        private static SemanticPath semanticPath(List<String> segments) {
            return SemanticPath.of(segments.get(0), segments.subList(1, segments.size()).toArray(String[]::new));
        }

        private static List<String> parsePath(String path) {
            if (path == null || path.isBlank()) throw new IllegalArgumentException("field path is required");
            List<String> segments = new ArrayList<>();
            for (String part : path.split("\\.")) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([A-Za-z][A-Za-z0-9]*)(?:\\[(\\d+)\\])?").matcher(part);
                if (!matcher.matches()) throw new IllegalArgumentException("Invalid Dpp4Fun field path: " + path);
                segments.add(matcher.group(1));
                if (matcher.group(2) != null) segments.add(matcher.group(2));
            }
            return List.copyOf(segments);
        }

        private static void validateValue(Dpp4FunElementDefinition binding, Object value) {
            if (!binding.matchesValue(value)) {
                throw new IllegalArgumentException("Value at " + binding.path() + " must match " + binding.valueDataType());
            }
        }
    }

    static final class Node {
        final Map<String, Node> children = new LinkedHashMap<>();
        Object value;
        List<MultiLanguageValue> languages;
        boolean isEmpty() { return value == null && languages == null && children.isEmpty(); }
    }
}
