package dppsdk.dpp4fun.semantic;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dppsdk.core.semantic.DataElementKind;
import dppsdk.core.semantic.SemanticPath;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** The sole runtime owner of D4.1 element, conflict, applicability, and validation metadata. */
public final class Dpp4FunSemanticProfile {
    private static final String RESOURCE = "/dppsdk/dpp4fun/semantic/dpp4fun-semantic-profile-v1.json";

    private final String profileVersion;
    private final String contentSpecificationId;
    private final Map<String, String> variantContentSpecificationIds;
    private final Map<String, String> sources;
    private final List<Dpp4FunElementDefinition> definitions;
    private final List<Dpp4FunSemanticRule> semanticRules;
    private final List<ConflictDecision> conflictDecisions;

    private Dpp4FunSemanticProfile(String profileVersion, String contentSpecificationId,
            Map<String, String> variantContentSpecificationIds, Map<String, String> sources,
            List<Dpp4FunElementDefinition> definitions, List<Dpp4FunSemanticRule> semanticRules,
            List<ConflictDecision> conflictDecisions) {
        this.profileVersion = Objects.requireNonNull(profileVersion, "profileVersion");
        this.contentSpecificationId = Objects.requireNonNull(contentSpecificationId, "contentSpecificationId");
        this.variantContentSpecificationIds = Map.copyOf(variantContentSpecificationIds);
        this.sources = Map.copyOf(sources);
        this.definitions = List.copyOf(definitions);
        this.semanticRules = List.copyOf(semanticRules);
        this.conflictDecisions = List.copyOf(conflictDecisions);
        validate();
    }

    public static Dpp4FunSemanticProfile loadDefault() {
        try (InputStream stream = Dpp4FunSemanticProfile.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing bundled D4.1 semantic profile: " + RESOURCE);
            return parse(new ObjectMapper().readTree(stream));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load bundled D4.1 semantic profile", exception);
        }
    }

    public String profileVersion() { return profileVersion; }
    public String contentSpecificationId() { return contentSpecificationId; }
    public Map<String, String> variantContentSpecificationIds() { return variantContentSpecificationIds; }
    public Map<String, String> sources() { return sources; }
    public List<Dpp4FunElementDefinition> definitions() { return definitions; }
    public List<Dpp4FunSemanticRule> semanticRules() { return semanticRules; }
    public List<ConflictDecision> conflictDecisions() { return conflictDecisions; }

    public List<String> contentSpecificationIds() { return List.of(contentSpecificationId); }

    /** Returns the ordered canonical root element IDs declared by this profile. */
    public List<String> rootElementIds() { return rootElementIds(definitions); }

    static List<String> rootElementIds(List<Dpp4FunElementDefinition> sourceDefinitions) {
        LinkedHashSet<String> rootIds = new LinkedHashSet<>();
        for (Dpp4FunElementDefinition definition : sourceDefinitions) {
            if (!definition.path().contains(".") && definition.path().equals(definition.elementId())) {
                rootIds.add(definition.elementId());
            }
        }
        return List.copyOf(rootIds);
    }

    public List<String> contentSpecificationIds(Dpp4FunProductVariant variant) {
        if (variant == null) return contentSpecificationIds();
        String variantId = variantContentSpecificationIds.get(variant.id());
        if (variantId == null) throw new IllegalStateException("Profile has no Characteristics variant " + variant.id());
        return List.of(contentSpecificationId, variantId);
    }

    public List<String> applicabilityFor(List<String> contentSpecificationIds) {
        Objects.requireNonNull(contentSpecificationIds, "contentSpecificationIds");
        LinkedHashSet<String> applicability = new LinkedHashSet<>();
        if (contentSpecificationIds.contains(contentSpecificationId)) applicability.add("general");
        for (Dpp4FunProductVariant variant : Dpp4FunProductVariant.values()) {
            if (contentSpecificationIds.contains(variantContentSpecificationIds.get(variant.id()))) {
                applicability.add("characteristics-" + variant.id());
            }
        }
        return List.copyOf(applicability);
    }

    public java.util.Optional<Dpp4FunElementDefinition> definitionFor(
            List<String> contentSpecificationIds, SemanticPath path) {
        Objects.requireNonNull(path, "path");
        List<String> applicability = applicabilityFor(contentSpecificationIds);
        List<Dpp4FunElementDefinition> matches = definitions.stream()
                .filter(definition -> definition.appliesTo().stream().anyMatch(applicability::contains))
                .filter(definition -> pathMatches(definition.path(), path))
                .toList();
        return matches.size() == 1 ? java.util.Optional.of(matches.get(0)) : java.util.Optional.empty();
    }

    public boolean isPathForAnotherVariant(SemanticPath path) {
        for (Dpp4FunProductVariant variant : Dpp4FunProductVariant.values()) {
            if (definitionFor(contentSpecificationIds(variant), path).isPresent()) return true;
        }
        return false;
    }

    private void validate() {
        if (!profileVersion.matches("\\d+\\.\\d+\\.\\d+")) {
            throw new IllegalStateException("Profile version must be semantic: " + profileVersion);
        }
        Set<String> expectedVariants = java.util.Arrays.stream(Dpp4FunProductVariant.values())
                .map(Dpp4FunProductVariant::id).collect(java.util.stream.Collectors.toSet());
        if (!variantContentSpecificationIds.keySet().equals(expectedVariants)
                || variantContentSpecificationIds.values().stream().anyMatch(String::isBlank)
                || variantContentSpecificationIds.values().stream().distinct().count() != expectedVariants.size()
                || variantContentSpecificationIds.containsValue(contentSpecificationId)) {
            throw new IllegalStateException("Profile must define a distinct content specification for each variant");
        }
        Set<String> keys = new HashSet<>();
        Map<String, Dpp4FunElementDefinition> byApplicabilityPath = new LinkedHashMap<>();
        for (Dpp4FunElementDefinition definition : definitions) {
            if (definition.repeatedItemPolicy() != null
                    && (definition.repeatedItemPolicy().elementIdPattern() != null) != definition.path().endsWith("[*]")) {
                throw new IllegalStateException("Repeated-item policy/path mismatch at " + definition.path());
            }
            for (String applicability : definition.appliesTo()) {
                if (!keys.add(applicability + "|" + definition.path())) {
                    throw new IllegalStateException("Duplicate profile path for applicability: " + definition.path());
                }
                byApplicabilityPath.put(applicability + "|" + definition.path(), definition);
            }
        }
        if (definitions.isEmpty()) throw new IllegalStateException("D4.1 semantic profile must not be empty");
        for (Dpp4FunElementDefinition definition : definitions) {
            if (definition.unitSlotPath() == null) continue;
            String[] path = definition.path().split("\\.", 2);
            if (path.length != 2 || definition.unitDimension() == null) {
                throw new IllegalStateException("Unit mapping must identify a nested measurement and dimension: "
                        + definition.path());
            }
            for (String applicability : definition.appliesTo()) {
                String slotKey = applicability + "|" + path[0] + "." + definition.unitSlotPath();
                Dpp4FunElementDefinition slot = byApplicabilityPath.get(slotKey);
                if (slot == null || slot.kind() != DataElementKind.SINGLE || !"xsd:string".equals(slot.valueDataType())) {
                    throw new IllegalStateException("Unit slot lacks a source-backed string binding: " + slotKey);
                }
            }
        }
    }

    private static boolean pathMatches(String pattern, SemanticPath path) {
        String[] expected = pattern.split("\\.");
        int actualIndex = 0;
        for (String segment : expected) {
            if (actualIndex >= path.segments().size()) return false;
            if (segment.endsWith("[*]")) {
                if (!segment.substring(0, segment.length() - 3).equals(path.segments().get(actualIndex))) return false;
                if (actualIndex + 1 >= path.segments().size() || !isIndex(path.segments().get(actualIndex + 1))) return false;
                actualIndex += 2;
            } else if (!segment.equals(path.segments().get(actualIndex++))) return false;
        }
        return actualIndex == path.segments().size();
    }

    private static boolean isIndex(String value) {
        if (value.isEmpty()) return false;
        for (int index = 0; index < value.length(); index++) if (!Character.isDigit(value.charAt(index))) return false;
        return true;
    }

    private static Dpp4FunSemanticProfile parse(JsonNode root) {
        List<Dpp4FunElementDefinition> definitions = new ArrayList<>();
        JsonNode definitionNodes = root.path("definitions");
        if (!definitionNodes.isArray()) throw new IllegalStateException("Profile definitions must be an array");
        for (JsonNode node : definitionNodes) definitions.add(parseDefinition(node));
        List<Dpp4FunSemanticRule> rules = new ArrayList<>();
        JsonNode ruleNodes = root.path("semanticRules");
        if (ruleNodes.isArray()) for (JsonNode node : ruleNodes) rules.add(parseRule(node));
        List<ConflictDecision> conflicts = new ArrayList<>();
        JsonNode conflictNodes = root.path("conflictDecisions");
        if (conflictNodes.isArray()) for (JsonNode node : conflictNodes) conflicts.add(parseConflict(node));
        return new Dpp4FunSemanticProfile(requiredText(root, "profileVersion"),
                requiredText(root, "contentSpecificationId"), stringMap(root.path("variantContentSpecificationIds")),
                stringMap(root.path("sources")), definitions, rules, conflicts);
    }

    private static Dpp4FunElementDefinition parseDefinition(JsonNode node) {
        String path = requiredText(node, "path");
        DataElementKind kind = switch (requiredText(node, "objectType")) {
            case "DataElementCollection" -> DataElementKind.COLLECTION;
            case "SingleValuedDataElement" -> DataElementKind.SINGLE;
            case "MultiValuedDataElement" -> DataElementKind.MULTI_VALUED;
            case "MultiLanguageDataElement" -> DataElementKind.MULTI_LANGUAGE;
            case "RelatedResource" -> DataElementKind.RELATED_RESOURCE;
            default -> throw new IllegalStateException("Unknown EN DataElement kind at " + path);
        };
        Set<String> applicability = stringSet(node.path("appliesTo"));
        List<String> aliases = stringList(node.path("aliases"));
        List<Dpp4FunElementDefinition.SourceReference> sourceReferences = new ArrayList<>();
        JsonNode references = node.path("sourceReferences");
        if (references.isArray()) for (JsonNode reference : references) {
            sourceReferences.add(new Dpp4FunElementDefinition.SourceReference(requiredText(reference, "source"),
                    requiredText(reference, "section"), reference.path("figureNumber").intValue(),
                    requiredText(reference, "aasTemplate")));
        }
        JsonNode policy = node.path("repeatedItemPolicy");
        Dpp4FunElementDefinition.RepeatedItemPolicy repeatedItemPolicy = policy.isObject()
                ? new Dpp4FunElementDefinition.RepeatedItemPolicy(policy.path("orderPreserved").asBoolean(),
                        nullableText(policy, "elementIdPattern")) : null;
        return new Dpp4FunElementDefinition(path, requiredText(node, "javaConcept"), requiredText(node, "elementId"),
                kind, nullableText(node, "dictionaryReference"), nullableText(node, "valueDataType"),
                occurrence(node.path("sourceCardinality")), occurrence(node.path("bindingOccurrence")),
                nullableText(node, "unitDimension"), nullableText(node, "unitSlotPath"), repeatedItemPolicy,
                applicability, aliases, sourceReferences, requiredText(node, "resolutionStatus"),
                nullableText(node.path("conflictResolution"), "note"), stringSet(node.path("validationRules")),
                stringMap(node.path("source")));
    }

    private static Dpp4FunSemanticRule parseRule(JsonNode node) {
        return new Dpp4FunSemanticRule(requiredText(node, "id"), requiredText(node, "pathSuffix"),
                nullableText(node, "requiredField"), stringList(node.path("requiredAnyFields")),
                stringList(node.path("excludedPaths")));
    }

    private static ConflictDecision parseConflict(JsonNode node) {
        return new ConflictDecision(requiredText(node, "id"), requiredText(node, "path"),
                requiredText(node, "status"), requiredText(node, "note"), stringList(node.path("evidence")));
    }

    private static Dpp4FunElementDefinition.Occurrence occurrence(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        JsonNode maxNode = node.path("max");
        Integer max = maxNode.isNull() || maxNode.isMissingNode() ? null : maxNode.intValue();
        return new Dpp4FunElementDefinition.Occurrence(node.path("min").intValue(), max);
    }

    private static Set<String> stringSet(JsonNode node) { return new LinkedHashSet<>(stringList(node)); }

    private static List<String> stringList(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(value -> { if (value.isTextual()) values.add(value.asText()); });
        return List.copyOf(values);
    }

    private static Map<String, String> stringMap(JsonNode node) {
        if (!node.isObject()) return Map.of();
        Map<String, String> values = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            if (entry.getValue().isTextual()) values.put(entry.getKey(), entry.getValue().asText());
        });
        return Map.copyOf(values);
    }

    private static String requiredText(JsonNode node, String field) {
        String value = nullableText(node, field);
        if (value == null || value.isBlank()) throw new IllegalStateException("Profile field is missing: " + field);
        return value;
    }

    private static String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    public record ConflictDecision(String id, String path, String status, String note, List<String> evidence) {
        public ConflictDecision {
            if (id == null || id.isBlank() || path == null || path.isBlank() || status == null || status.isBlank()
                    || note == null || evidence == null) throw new IllegalArgumentException("conflict decision incomplete");
            evidence = List.copyOf(evidence);
        }
    }
}
