package dppsdk.core.validation.format;

import java.util.Set;

/** Classifies the XSD 1.0 built-ins referenced by EN 18223 and its exclusions. */
final class ValueDataTypeValidator {

    enum Classification {
        ALLOWED,
        PROHIBITED,
        UNKNOWN
    }

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "anySimpleType", "string", "boolean", "decimal", "float", "double",
            "duration", "dateTime", "time", "date", "gYearMonth", "gYear", "gMonthDay", "gDay",
            "gMonth", "hexBinary", "base64Binary", "anyURI", "integer", "nonPositiveInteger",
            "negativeInteger", "long", "int", "short", "byte", "nonNegativeInteger", "unsignedLong",
            "unsignedInt", "unsignedShort", "unsignedByte", "positiveInteger");

    private static final Set<String> PROHIBITED_TYPES = Set.of(
            "ENTITIES", "IDREFS", "NMTOKENS", "normalizedString", "token", "language", "NMTOKEN",
            "Name", "NCName", "ID", "IDREF", "ENTITY", "NOTATION", "QName");

    private ValueDataTypeValidator() { }

    static Classification classify(String lexicalValue) {
        if (lexicalValue == null || !lexicalValue.startsWith("xsd:")
                || lexicalValue.length() == 4) {
            return Classification.UNKNOWN;
        }
        String name = lexicalValue.substring(4);
        if (PROHIBITED_TYPES.contains(name)) return Classification.PROHIBITED;
        return ALLOWED_TYPES.contains(name) ? Classification.ALLOWED : Classification.UNKNOWN;
    }

    static String builtInName(String lexicalValue) {
        return classify(lexicalValue) == Classification.ALLOWED
                ? lexicalValue.substring(4) : null;
    }
}
