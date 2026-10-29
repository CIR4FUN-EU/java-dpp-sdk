package dppsdk.core.validation;

import java.util.Objects;

/** One mandatory semantic validation finding at a deterministic model path. */
public record ValidationIssue(String path, String rule, String message, Severity severity) {
    public enum Severity { ERROR, WARNING }

    public ValidationIssue(String path, String rule, String message) {
        this(path, rule, message, Severity.ERROR);
    }

    public ValidationIssue {
        Objects.requireNonNull(path, "path is required");
        Objects.requireNonNull(rule, "rule is required");
        Objects.requireNonNull(message, "message is required");
        Objects.requireNonNull(severity, "severity is required");
        rule = canonicalRule(rule);
    }

    private static String canonicalRule(String rule) {
        return switch (rule) {
            case "ELEMENT_ID_REQUIRED" -> "EN18223_ELEMENT_ID_REQUIRED";
            case "ELEMENT_ID_DUPLICATE_IN_SCOPE" -> "EN18223_ELEMENT_ID_UNIQUE_WITHIN_LOCATION";
            case "COLLECTION_EMPTY" -> "EN18223_COLLECTION_NON_EMPTY";
            case "SINGLE_VALUE_REQUIRED" -> "EN18223_SINGLE_VALUE_REQUIRED";
            case "MULTI_VALUE_EMPTY" -> "EN18223_MULTI_NON_EMPTY";
            case "MULTI_VALUE_MIXED_ELEMENT_TYPES" -> "EN18223_MULTI_HOMOGENEOUS_KIND";
            case "MULTI_VALUE_JSON_TYPE_MISMATCH" -> "EN18223_JSON_NATIVE_TYPE_MATCHES_XSD";
            case "MULTI_LANGUAGE_EMPTY" -> "EN18223_MULTILANGUAGE_NON_EMPTY";
            case "MULTI_LANGUAGE_VALUE_REQUIRED" -> "EN18223_MULTILANGUAGE_VALUE_REQUIRED";
            case "MULTI_LANGUAGE_LANGUAGE_REQUIRED" -> "EN18223_MULTILANGUAGE_LANGUAGE_REQUIRED";
            case "RELATED_RESOURCE_CONTENT_TYPE_REQUIRED" -> "EN18223_RELATED_RESOURCE_CONTENT_TYPE_REQUIRED";
            case "RELATED_RESOURCE_URL_REQUIRED" -> "EN18223_RELATED_RESOURCE_URL_REQUIRED";
            case "RESOURCE_URL_INVALID" -> "EN18223_RELATED_RESOURCE_URL_RFC3986";
            case "MEDIA_TYPE_INVALID" -> "EN18223_RELATED_RESOURCE_CONTENT_TYPE_SYNTAX";
            case "LOCALIZED_LANGUAGE_FORMAT_INVALID", "LANGUAGE_CODE_UNKNOWN", "COUNTRY_CODE_UNKNOWN" ->
                    "EN18223_MULTILANGUAGE_LANGUAGE_TAG";
            case "VALUE_DATA_TYPE_PROHIBITED", "VALUE_DATA_TYPE_UNKNOWN" -> "EN18223_VALUE_DATATYPE_SUPPORTED";
            case "VALUE_TYPE_INCOMPATIBLE", "VALUE_FORMAT_INVALID" -> "EN18223_VALUE_MATCHES_DATATYPE";
            case "MULTI_VALUE_DATA_TYPE_CONFLICT", "MULTI_VALUE_DATA_TYPE_MISMATCH" ->
                    "EN18223_MULTI_HOMOGENEOUS_DATATYPE";
            default -> rule;
        };
    }
}
