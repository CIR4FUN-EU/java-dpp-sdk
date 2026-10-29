package dppsdk.dpp4fun.semantic;
import java.math.BigInteger;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.semantic.DataElementKind;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Source-resolved D4.1 element metadata owned by the semantic profile. */
public record Dpp4FunElementDefinition(
        String path,
        String javaConcept,
        String elementId,
        DataElementKind kind,
        String dictionaryReference,
        String valueDataType,
        Occurrence sourceCardinality,
        Occurrence bindingOccurrence,
        String unitDimension,
        String unitSlotPath,
        RepeatedItemPolicy repeatedItemPolicy,
        Set<String> appliesTo,
        List<String> aliases,
        List<SourceReference> sourceReferences,
        String resolutionStatus,
        String conflictResolutionNote,
        Set<String> validationRules,
        Map<String, String> source) {

    public Dpp4FunElementDefinition {
        if (path == null || path.isBlank() || javaConcept == null || javaConcept.isBlank()
                || elementId == null || elementId.isBlank() || kind == null || bindingOccurrence == null
                || appliesTo == null || appliesTo.isEmpty() || aliases == null || sourceReferences == null
                || resolutionStatus == null || validationRules == null || source == null) {
            throw new IllegalArgumentException("semantic profile element definition is incomplete");
        }
        appliesTo = Set.copyOf(appliesTo);
        aliases = List.copyOf(aliases);
        sourceReferences = List.copyOf(sourceReferences);
        validationRules = Set.copyOf(validationRules);
        source = Map.copyOf(source);
    }

    public boolean dictionaryReferenceResolved() {
        return !"UNRESOLVED_DICTIONARY_REFERENCE".equals(resolutionStatus)
                && !"RESOLVED_WITHOUT_DICTIONARY_REFERENCE".equals(resolutionStatus);
    }

    public boolean valueDataTypeResolved() {
        return !"UNRESOLVED_VALUE_DATA_TYPE".equals(resolutionStatus);
    }

    public boolean matchesValue(Object value) {
        return switch (valueDataType == null ? "" : valueDataType) {
            case "xsd:string", "xsd:anyURI", "xsd:dateTime" -> value instanceof String;
            case "xsd:boolean" -> value instanceof Boolean;
            case "xsd:double", "xsd:decimal" -> value instanceof Number number
                    && Double.isFinite(number.doubleValue());
            case "xsd:integer" -> value instanceof Byte || value instanceof Short || value instanceof Integer
                    || value instanceof Long || value instanceof BigInteger;
            default -> false;
        };
    }

    public record Occurrence(int min, Integer max) {
        public Occurrence {
            if (min < 0 || (max != null && max < min)) {
                throw new IllegalArgumentException("invalid occurrence bounds");
            }
        }
    }

    public record RepeatedItemPolicy(boolean orderPreserved, String elementIdPattern) {
        public RepeatedItemPolicy {
            if (elementIdPattern != null && !elementIdPattern.contains("%02d")) {
                throw new IllegalArgumentException("unsupported repeated-item elementId pattern");
            }
        }
    }

    public record SourceReference(String source, String section, int figureNumber, String aasTemplate) {
        public SourceReference {
            if (source == null || source.isBlank() || section == null || section.isBlank()
                    || figureNumber < 3 || aasTemplate == null || aasTemplate.isBlank()) {
                throw new IllegalArgumentException("source reference is incomplete");
            }
        }
    }
}
