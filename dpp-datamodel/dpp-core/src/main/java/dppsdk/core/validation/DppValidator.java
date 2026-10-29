package dppsdk.core.validation;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.MultiLanguageValue;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.RelatedResource;
import dppsdk.core.model.SingleValuedDataElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validates mandatory EN 18223 Clause 4 semantics for a canonical passport model. */
public final class DppValidator {

    /** Validate the full passport and return every mandatory semantic issue found. */
    public ValidationResult validate(DigitalProductPassport passport) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (passport == null) {
            add(issues, "dpp", "DPP_REQUIRED", "DigitalProductPassport is required");
            return new ValidationResult(issues);
        }

        validateHeader(passport.getDppHeader(), issues);
        validateAbsolutePaths(passport.getDataElements(), "elements", "elements", new HashSet<>(), issues);
        validateElements(passport.getDataElements(), new ValidationPath("elements"), issues);
        return new ValidationResult(issues);
    }

    private static void validateHeader(DppHeader header, List<ValidationIssue> issues) {
        requiredHeader(header.getDigitalProductPassportId(), "digitalProductPassportId", "EN18223_HEADER_DPP_ID_REQUIRED", issues);
        requiredHeader(header.getUniqueProductIdentifier(), "uniqueProductIdentifier", "EN18223_HEADER_PRODUCT_ID_REQUIRED", issues);
        requiredHeader(header.getGranularity(), "granularity", "EN18223_HEADER_GRANULARITY_REQUIRED", issues);
        requiredHeader(header.getDppSchemaVersion(), "dppSchemaVersion", "EN18223_HEADER_SCHEMA_VERSION_REQUIRED", issues);
        requiredHeader(header.getDppStatus(), "dppStatus", "EN18223_HEADER_STATUS_REQUIRED", issues);
        requiredHeader(header.getLastUpdated(), "lastUpdated", "EN18223_HEADER_LAST_UPDATED_REQUIRED", issues);
        requiredHeader(header.getEconomicOperatorId(), "economicOperatorId", "EN18223_HEADER_ECONOMIC_OPERATOR_ID_REQUIRED", issues);
        if (header.getDigitalProductPassportId() != null && !isAbsoluteUri(header.getDigitalProductPassportId())) {
            issues.add(new ValidationIssue("header.digitalProductPassportId",
                    "EN18223_HEADER_DPP_ID_URI_RECOMMENDED", "digitalProductPassportId should be an absolute URI",
                    ValidationIssue.Severity.WARNING));
        }
        for (String id : header.getContentSpecificationIds()) {
            if (id == null || id.isBlank()) add(issues, "header.contentSpecificationIds",
                    "EN18223_HEADER_CONTENT_SPEC_ID_NONBLANK", "contentSpecificationId must not be blank");
        }
    }

    private static void validateElements(List<DataElement> elements, ValidationPath path,
                                         List<ValidationIssue> issues) {
        Set<String> idsInScope = new HashSet<>();
        for (int i = 0; i < elements.size(); i++) {
            DataElement element = elements.get(i);
            int elementMark = path.length();
            path.appendIndex(i);
            try {
                String id = element.getElementId();
                if (id == null || id.isBlank()) {
                    addAt(issues, path, ".elementId", "EN18223_ELEMENT_ID_REQUIRED",
                            "elementId is required");
                } else if (!idsInScope.add(id)) {
                    addAt(issues, path, ".elementId", "EN18223_ELEMENT_ID_UNIQUE_WITHIN_LOCATION",
                            "elementId must be unique among elements in the same location");
                }

                validateElement(element, path, issues);
            } finally {
                path.truncate(elementMark);
            }
        }
    }

    private static void validateElement(DataElement element, ValidationPath path,
                                        List<ValidationIssue> issues) {
        if (element instanceof DataElementCollection collection) {
            int mark = path.length();
            path.append(".elements");
            try {
                if (collection.getDataElements().isEmpty()) {
                    add(issues, path.toString(), "EN18223_COLLECTION_NON_EMPTY",
                            "DataElementCollection must contain at least one DataElement");
                } else {
                    validateElements(collection.getDataElements(), path, issues);
                }
            } finally {
                path.truncate(mark);
            }
        } else if (element instanceof SingleValuedDataElement single) {
            if (single.getValue() == null) {
                addAt(issues, path, ".value", "EN18223_SINGLE_VALUE_REQUIRED",
                        "SingleValuedDataElement value is required");
            }
        } else if (element instanceof MultiValuedDataElement multi) {
            List<DataElement> values = multi.getDataElements();
            int mark = path.length();
            path.append(".elements");
            try {
                if (values.isEmpty()) {
                    add(issues, path.toString(), "EN18223_MULTI_NON_EMPTY",
                            "MultiValuedDataElement must contain at least one DataElement");
                } else {
                    Class<?> expectedType = values.get(0).getClass();
                    boolean mixedElementTypes = false;
                    for (int i = 1; i < values.size(); i++) {
                        if (values.get(i).getClass() != expectedType) {
                            mixedElementTypes = true;
                            int childMark = path.length();
                            path.appendIndex(i);
                            add(issues, path.toString(), "EN18223_MULTI_HOMOGENEOUS_KIND",
                                    "All DataElements in a MultiValuedDataElement must have the same concrete type");
                            path.truncate(childMark);
                        }
                    }
                    if (!mixedElementTypes) {
                        validateMultiValueJsonTypes(values, path, issues);
                    }
                    validateElements(values, path, issues);
                }
            } finally {
                path.truncate(mark);
            }
        } else if (element instanceof RelatedResource resource) {
            required(resource.getContentType(), path, ".contentType", issues,
                    "RELATED_RESOURCE_CONTENT_TYPE_REQUIRED", "contentType is required");
            required(resource.getUrl(), path, ".url", issues,
                    "RELATED_RESOURCE_URL_REQUIRED", "url is required");
        } else if (element instanceof MultiLanguageDataElement multilingual) {
            List<MultiLanguageValue> values = multilingual.getValues();
            int mark = path.length();
            path.append(".values");
            try {
                if (values.isEmpty()) {
                    add(issues, path.toString(), "MULTI_LANGUAGE_EMPTY",
                            "MultiLanguageDataElement must contain at least one MultiLanguageValue");
                }
                for (int i = 0; i < values.size(); i++) {
                    int valueMark = path.length();
                    path.appendIndex(i);
                    try {
                        MultiLanguageValue value = values.get(i);
                        required(value.getValue(), path, ".value", issues,
                                "MULTI_LANGUAGE_VALUE_REQUIRED", "value is required");
                        required(value.getLanguage(), path, ".language", issues,
                                "MULTI_LANGUAGE_LANGUAGE_REQUIRED", "language is required");
                    } finally {
                        path.truncate(valueMark);
                    }
                }
            } finally {
                path.truncate(mark);
            }
        } else {
            add(issues, path.toString(), "EN18223_OBJECT_TYPE_MATCHES_RUNTIME_KIND",
                    "Unsupported DataElement implementation: " + element.getClass().getName());
        }
    }

    private static void validateMultiValueJsonTypes(List<DataElement> values, ValidationPath path,
                                                    List<ValidationIssue> issues) {
        JsonValueType expectedType = null;
        for (int i = 0; i < values.size(); i++) {
            JsonValueType currentType = jsonValueType(values.get(i));
            if (currentType == null) continue;
            if (expectedType == null) {
                expectedType = currentType;
            } else if (currentType != expectedType) {
                int mark = path.length();
                path.appendIndex(i);
                add(issues, path.toString(), "MULTI_VALUE_JSON_TYPE_MISMATCH",
                        "All values in a MultiValuedDataElement must serialize to the same JSON type");
                path.truncate(mark);
            }
        }
    }

    /** Top-level JSON type produced by the compressed representation for a DataElement. */
    private static JsonValueType jsonValueType(DataElement element) {
        if (element instanceof SingleValuedDataElement single) {
            return jsonValueType(single.getValue());
        }
        if (element instanceof DataElementCollection || element instanceof RelatedResource) {
            return JsonValueType.OBJECT;
        }
        if (element instanceof MultiValuedDataElement || element instanceof MultiLanguageDataElement) {
            return JsonValueType.ARRAY;
        }
        return null;
    }

    private static JsonValueType jsonValueType(Object value) {
        if (value instanceof String) return JsonValueType.STRING;
        if (value instanceof Number) return JsonValueType.NUMBER;
        if (value instanceof Boolean) return JsonValueType.BOOLEAN;
        if (value instanceof Map<?, ?>) return JsonValueType.OBJECT;
        if (value instanceof List<?>) return JsonValueType.ARRAY;
        // Null is reported by SINGLE_VALUE_REQUIRED; do not duplicate it as a type mismatch.
        return null;
    }

    private enum JsonValueType {
        STRING,
        NUMBER,
        BOOLEAN,
        OBJECT,
        ARRAY
    }

    private static void validateAbsolutePaths(List<DataElement> elements, String semanticParent,
            String displayParent, Set<String> seen, List<ValidationIssue> issues) {
        for (int i = 0; i < elements.size(); i++) {
            DataElement element = elements.get(i);
            String id = element.getElementId();
            if (id == null || id.isBlank()) continue;
            String absolute = semanticParent + "." + id;
            if (!seen.add(absolute)) {
                add(issues, displayParent + "[" + i + "].elementId", "EN18223_ABSOLUTE_PATH_UNIQUE",
                        "absolute DataElement paths must be unique");
            }
            List<DataElement> children = element instanceof DataElementCollection collection
                    ? collection.getDataElements()
                    : element instanceof MultiValuedDataElement multi ? multi.getDataElements() : List.of();
            if (!children.isEmpty()) validateAbsolutePaths(children, absolute, displayParent + "[" + i + "].elements",
                    seen, issues);
        }
    }

    private static void requiredHeader(Object value, String property, String rule, List<ValidationIssue> issues) {
        if (value == null) {
            add(issues, "header." + property, rule, property + " is required");
        }
    }

    private static boolean isAbsoluteUri(String value) {
        try { return java.net.URI.create(value).isAbsolute(); }
        catch (IllegalArgumentException exception) { return false; }
    }

    private static void required(Object value, ValidationPath path, String suffix,
                                 List<ValidationIssue> issues,
                                 String rule, String message) {
        if (value == null) {
            addAt(issues, path, suffix, rule, message);
        }
    }

    private static void addAt(List<ValidationIssue> issues, ValidationPath path, String suffix,
                              String rule, String message) {
        add(issues, path.toString() + suffix, rule, message);
    }

    private static void add(List<ValidationIssue> issues, String path, String rule, String message) {
        issues.add(new ValidationIssue(path, rule, message));
    }

    private static final class ValidationPath {
        private final StringBuilder value;

        private ValidationPath(String root) {
            this.value = new StringBuilder(root);
        }

        private int length() {
            return value.length();
        }

        private void append(String suffix) {
            value.append(suffix);
        }

        private void appendIndex(int index) {
            value.append('[').append(index).append(']');
        }

        private void truncate(int length) {
            value.setLength(length);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }
}
