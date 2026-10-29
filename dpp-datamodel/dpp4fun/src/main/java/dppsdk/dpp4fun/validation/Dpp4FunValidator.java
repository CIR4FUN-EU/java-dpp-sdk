package dppsdk.dpp4fun.validation;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.RelatedResource;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.semantic.DataElementKind;
import dppsdk.core.semantic.SemanticPath;
import dppsdk.core.validation.ValidationIssue;
import dppsdk.core.validation.ValidationResult;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile;
import dppsdk.dpp4fun.semantic.Dpp4FunElementDefinition;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Validates only D4.1 catalog bindings and the SDK binding invariants. */
public final class Dpp4FunValidator {
    private final Dpp4FunSemanticProfile catalog = Dpp4FunSemanticProfile.loadDefault();
    private final Dpp4FunSemanticResolver resolver = new Dpp4FunSemanticResolver(catalog);

    public ValidationResult validate(Dpp4Fun value) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (value == null) {
            issue(issues, "", "DPP4FUN_REQUIRED", "Dpp4Fun is required");
            return ValidationResult.of(issues);
        }
        DigitalProductPassport passport = value.asDigitalProductPassport();
        List<String> contentIds = passport.getDppHeader().getContentSpecificationIds();
        List<String> knownContentIds = new ArrayList<>();
        knownContentIds.add(catalog.contentSpecificationId());
        knownContentIds.addAll(catalog.variantContentSpecificationIds().values());
        for (String id : contentIds) {
            if (!knownContentIds.contains(id)) issue(issues, "contentSpecificationIds", "DPP4FUN_CONTENT_SPEC_SUPPORTED",
                    "content specification is not supported by this Dpp4Fun profile: " + id);
        }
        List<String> applicability = catalog.applicabilityFor(contentIds);
        long variants = catalog.variantContentSpecificationIds().values().stream().filter(contentIds::contains).count();
        if (variants > 1) issue(issues, "contentSpecificationIds", "CHARACTERISTICS_VARIANT_AMBIGUOUS",
                "at most one frozen Characteristics variant may be selected");

        Map<String, Integer> rootOccurrences = new HashMap<>();
        for (DataElement element : passport.getDataElements()) {
            String id = element.getElementId();
            rootOccurrences.merge(id, 1, Integer::sum);
            SemanticPath path = SemanticPath.of(id);
            boolean knownRoot = catalog.definitions().stream().anyMatch(entry -> entry.path().equals(id)
                    && entry.appliesTo().stream().anyMatch(applicability::contains));
            if (knownRoot) validateElement(element, path, contentIds, issues, "elements." + id);
        }
        rootOccurrences.forEach((id, count) -> {
            if (count > 1 && catalog.definitionFor(contentIds, SemanticPath.of(id)).isPresent()) {
                issue(issues, "elements." + id, "BINDING_OCCURRENCE_EXCEEDED",
                        "catalog binding permits at most one occurrence");
            }
        });
        if (!applicability.contains("general") && passport.getDataElements().stream().anyMatch(element ->
                catalog.definitions().stream().anyMatch(entry -> entry.path().equals(element.getElementId())
                        && entry.appliesTo().contains("general")))) {
            issue(issues, "contentSpecificationIds", "DPP4FUN_BINDING_REQUIRED",
                    "general Dpp4Fun aspects require the SDK binding content specification");
        }
        validateSemanticMinimums(passport.getDataElements(), "", issues);
        return ValidationResult.of(issues);
    }

    /** Applies only the explicit D4.1 prose minima; optional fields remain optional. */
    private void validateSemanticMinimums(List<DataElement> elements, String parentPath,
            List<ValidationIssue> issues) {
        for (DataElement element : elements) {
            String path = parentPath.isEmpty() ? element.getElementId() : parentPath + "." + element.getElementId();
            if (element instanceof MultiValuedDataElement values) {
                List<dppsdk.dpp4fun.semantic.Dpp4FunSemanticRule> matchingRules = catalog.semanticRules().stream()
                        .filter(rule -> path.toLowerCase(java.util.Locale.ROOT)
                                .endsWith(rule.pathSuffix().replace("[*]", "").toLowerCase(java.util.Locale.ROOT)))
                        .filter(rule -> rule.excludedPaths().stream().noneMatch(excluded ->
                                (path + "[*]").equalsIgnoreCase(excluded)))
                        .toList();
                for (int index = 0; index < values.getDataElements().size(); index++) {
                    DataElement item = values.getDataElements().get(index);
                    String itemPath = path + "[" + index + "]";
                    if (item instanceof DataElementCollection collection) {
                        for (var rule : matchingRules) {
                            if (rule.requiredField() != null) {
                                requireField(collection, itemPath, rule.requiredField(), rule.id(), issues);
                            } else if (rule.requiredAnyFields().stream().noneMatch(field -> hasField(collection, field))) {
                                issue(issues, itemPath + "." + rule.requiredAnyFields().get(0), rule.id(),
                                        "one of " + rule.requiredAnyFields() + " is required by D4.1");
                            }
                        }
                    }
                    if (item instanceof DataElementCollection collection) {
                        validateSemanticMinimums(collection.getDataElements(), itemPath, issues);
                    }
                }
            } else if (element instanceof DataElementCollection collection) {
                validateSemanticMinimums(collection.getDataElements(), path, issues);
            }
        }
    }

    private static void requireField(DataElementCollection value, String path, String field, String rule,
            List<ValidationIssue> issues) {
        if (!hasField(value, field)) issue(issues, path + "." + field, rule,
                "field " + field + " is required by D4.1");
    }

    private static boolean hasField(DataElementCollection value, String id) {
        return value.getDataElements().stream().anyMatch(child -> id.equals(child.getElementId()));
    }

    private void validateElement(DataElement element, SemanticPath path, List<String> contentIds,
            List<ValidationIssue> issues, String issuePath) {
        Dpp4FunElementDefinition binding = catalog.definitionFor(contentIds, path).orElse(null);
        if (binding == null) {
            if (isOtherVariantPath(path)) issue(issues, issuePath, "PRODUCT_VARIANT_NOT_APPLICABLE",
                    "element is defined for a different Characteristics variant");
            return; // core-representable extensions are retained and are not assigned guessed semantics
        }
        DataElementKind actualKind = kindOf(element);
        if (actualKind != binding.kind()) {
            issue(issues, issuePath, "DATA_ELEMENT_KIND_MISMATCH",
                    "catalog expects " + binding.kind() + " but found " + actualKind);
            return;
        }
        var definition = resolver.resolve(contentIds, path).orElseThrow();
        if (definition.elementId().isPresent() && !definition.elementId().get().equals(element.getElementId())) {
            issue(issues, issuePath, "ELEMENT_ID_MISMATCH", "elementId does not match the catalog binding");
        }
        if (definition.dictionaryReferenceResolved()
                && !Objects.equals(definition.dictionaryReference().orElse(null), element.getDictionaryReference())) {
            issue(issues, issuePath, "DICTIONARY_REFERENCE_MISMATCH", "dictionaryReference does not match the catalog binding");
        }
        if (element instanceof SingleValuedDataElement scalar) {
            if (binding.unitDimension() != null && binding.unitDimension().endsWith("-unit")
                    && scalar.getValue() instanceof String symbol
                    && knownUnitHasDifferentDimension(symbol, binding.unitDimension())) {
                issue(issues, issuePath, "DPP4FUN_UNIT_DIMENSION_MISMATCH",
                        "known unit symbol does not match the catalogued physical dimension");
            }
            if (path.segments().equals(List.of("characteristics", "surface"))
                    && !definition.dictionaryReferenceResolved()) {
                issues.add(new ValidationIssue(issuePath, "DPP4FUN_SURFACE_SEMANTIC_BINDING_UNRESOLVED",
                        "Surface has no approved semantic dictionary reference",
                        ValidationIssue.Severity.WARNING));
            }
            if (definition.valueDataTypeResolved()
                    && !Objects.equals(definition.valueDataType().orElse(null), scalar.getValueDataType())) {
                issue(issues, issuePath, "VALUE_DATA_TYPE_MISMATCH", "valueDataType does not match the catalog binding");
            }
            if ("xsd:anyURI".equals(binding.valueDataType()) && scalar.getValue() instanceof String text
                    && !isUriReference(text)) {
                issue(issues, issuePath + ".value", "DPP4FUN_URI_FIELD_RFC3986",
                        "URI-valued D4.1 field must use RFC 3986 syntax");
            }
            if (!binding.matchesValue(scalar.getValue())) {
                issue(issues, issuePath, "VALUE_KIND_MISMATCH", "value does not match the catalog datatype");
            }
        } else if (element instanceof MultiValuedDataElement values) {
            if (definition.valueDataTypeResolved()
                    && !Objects.equals(definition.valueDataType().orElse(null), values.getValueDataType())) {
                issue(issues, issuePath, "VALUE_DATA_TYPE_MISMATCH", "valueDataType does not match the catalog binding");
            }
            if (values.getDataElements().isEmpty()) issue(issues, issuePath, "EMPTY_PROFILE_CONTAINER",
                    "empty D4.1 lists must be omitted");
            Dpp4FunElementDefinition itemBinding = catalog.definitionFor(contentIds, path.child("0")).orElse(null);
            Dpp4FunElementDefinition.Occurrence sourceItems = itemBinding == null
                    ? binding.sourceCardinality() : itemBinding.sourceCardinality();
            checkOccurrence(sourceItems, values.getDataElements().size(), issuePath, issues);
            for (int index = 0; index < values.getDataElements().size(); index++) {
                DataElement child = values.getDataElements().get(index);
                if (itemBinding != null && itemBinding.repeatedItemPolicy() != null
                        && itemBinding.repeatedItemPolicy().elementIdPattern() != null) {
                    String expectedId = String.format(java.util.Locale.ROOT,
                            itemBinding.repeatedItemPolicy().elementIdPattern(), index);
                    if (!expectedId.equals(child.getElementId())) {
                        issue(issues, issuePath + "[" + index + "].elementId", "REPEATED_ITEM_ID_MISMATCH",
                                "repeated item elementId does not match the catalog position policy");
                    }
                }
                if (itemBinding != null && kindOf(child) != itemBinding.kind()) {
                    issue(issues, issuePath + "[" + index + "]", "DPP4FUN_LIST_ITEM_KIND",
                            "repeated item must use the catalogued homogeneous child kind");
                    continue;
                }
                validateElement(child, path.child(Integer.toString(index)), contentIds, issues,
                        issuePath + "[" + index + "]");
            }
        } else if (element instanceof DataElementCollection collection) {
            if (collection.getDataElements().isEmpty()) issue(issues, issuePath, "EMPTY_PROFILE_CONTAINER",
                    "empty D4.1 collections must be omitted");
            Map<String, Integer> siblingCounts = new HashMap<>();
            for (DataElement child : collection.getDataElements()) siblingCounts.merge(child.getElementId(), 1, Integer::sum);
            for (DataElement child : collection.getDataElements()) {
                SemanticPath childPath = path.child(child.getElementId());
                Dpp4FunElementDefinition childBinding = catalog.definitionFor(contentIds, childPath).orElse(null);
                if (siblingCounts.get(child.getElementId()) > 1 && childBinding != null
                        && childBinding.bindingOccurrence().max() != null
                        && siblingCounts.get(child.getElementId()) > childBinding.bindingOccurrence().max()) {
                    issue(issues, issuePath + "." + child.getElementId(), "BINDING_OCCURRENCE_EXCEEDED",
                            "catalog binding occurrence is exceeded");
                }
                validateElement(child, childPath, contentIds, issues,
                        issuePath + "." + child.getElementId());
            }
        } else if (element instanceof RelatedResource) {
            // No RelatedResource row exists in the frozen catalog without source resource metadata.
        }
    }

    private static boolean knownUnitHasDifferentDimension(String symbol, String expectedDimension) {
        for (dppsdk.dpp4fun.model.unit.UnitDimension dimension : dppsdk.dpp4fun.model.unit.UnitDimension.values()) {
            if (!dimension.name().equalsIgnoreCase(expectedDimension.replace("-unit", ""))
                    && dppsdk.dpp4fun.model.unit.SemanticUnit.known(symbol, dimension).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private boolean isOtherVariantPath(SemanticPath path) {
        for (Dpp4FunProductVariant variant : Dpp4FunProductVariant.values()) {
            List<String> ids = catalog.contentSpecificationIds(variant);
            if (catalog.definitionFor(ids, path).isPresent()) return true;
        }
        return false;
    }

    private static DataElementKind kindOf(DataElement element) {
        if (element instanceof DataElementCollection) return DataElementKind.COLLECTION;
        if (element instanceof SingleValuedDataElement) return DataElementKind.SINGLE;
        if (element instanceof MultiValuedDataElement) return DataElementKind.MULTI_VALUED;
        if (element instanceof MultiLanguageDataElement) return DataElementKind.MULTI_LANGUAGE;
        return DataElementKind.RELATED_RESOURCE;
    }

    private static boolean isUriReference(String value) {
        try { java.net.URI.create(value); return true; }
        catch (IllegalArgumentException exception) { return false; }
    }

    private static void checkOccurrence(Dpp4FunElementDefinition.Occurrence occurrence, int count, String path,
            List<ValidationIssue> issues) {
        if (occurrence == null) return;
        if (count < occurrence.min() || occurrence.max() != null && count > occurrence.max()) {
            issue(issues, path, "SOURCE_CARDINALITY_MISMATCH", "item count conflicts with frozen source cardinality");
        }
    }

    private static void issue(List<ValidationIssue> issues, String path, String rule, String message) {
        issues.add(new ValidationIssue(path, canonicalRule(rule), message));
    }

    private static String canonicalRule(String rule) {
        return switch (rule) {
            case "CHARACTERISTICS_VARIANT_AMBIGUOUS" -> "DPP4FUN_CHARACTERISTICS_VARIANT_SINGLE";
            case "PRODUCT_VARIANT_NOT_APPLICABLE" -> "DPP4FUN_CHARACTERISTICS_VARIANT_MATCHES_CONTENT_SPEC";
            case "DPP4FUN_BINDING_REQUIRED" -> "DPP4FUN_CONTENT_SPEC_SUPPORTED";
            case "DATA_ELEMENT_KIND_MISMATCH" -> "DPP4FUN_RESERVED_PATH_KIND";
            case "ELEMENT_ID_MISMATCH" -> "DPP4FUN_RESERVED_PATH_ELEMENT_ID";
            case "DICTIONARY_REFERENCE_MISMATCH" -> "DPP4FUN_RESERVED_PATH_DICTIONARY_REFERENCE";
            case "VALUE_DATA_TYPE_MISMATCH" -> "DPP4FUN_RESERVED_PATH_VALUE_DATATYPE";
            case "VALUE_KIND_MISMATCH" -> "DPP4FUN_BINDING_DATATYPE_MISMATCH";
            case "EMPTY_PROFILE_CONTAINER", "SOURCE_CARDINALITY_MISMATCH", "BINDING_OCCURRENCE_EXCEEDED" ->
                    "DPP4FUN_EXPLICIT_CARDINALITY";
            case "REPEATED_ITEM_ID_MISMATCH" -> "DPP4FUN_LIST_ITEM_ID_CANONICAL";
            case "DPP4FUN_INSTRUCTION_MINIMUM" -> "DPP4FUN_INSTRUCTION_NAME_REQUIRED";
            case "DPP4FUN_ASSESSMENT_MINIMUM" -> "DPP4FUN_ASSESSMENT_NAME_REQUIRED";
            case "DPP4FUN_RECORD_MINIMUM" -> "DPP4FUN_RECORD_NAME_REQUIRED";
            default -> rule;
        };
    }
}
