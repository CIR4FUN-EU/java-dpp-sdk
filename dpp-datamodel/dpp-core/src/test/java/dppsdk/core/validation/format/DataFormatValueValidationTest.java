package dppsdk.core.validation.format;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.RelatedResource;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.validation.DppValidator;
import dppsdk.core.validation.ValidationIssue;
import dppsdk.core.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataFormatValueValidationTest {

    @Test
    void recognizesEveryApplicableXmlSchemaBuiltInAndKeepsTableSevenSeparate() {
        String[] allowed = {
                "anySimpleType", "string", "boolean", "decimal", "float", "double",
                "duration", "dateTime", "time", "date", "gYearMonth", "gYear", "gMonthDay", "gDay",
                "gMonth", "hexBinary", "base64Binary", "anyURI", "integer", "nonPositiveInteger",
                "negativeInteger", "long", "int", "short", "byte", "nonNegativeInteger", "unsignedLong",
                "unsignedInt", "unsignedShort", "unsignedByte", "positiveInteger"
        };
        DataElement[] elements = new DataElement[allowed.length];
        for (int i = 0; i < allowed.length; i++) {
            String type = "xsd:" + allowed[i];
            elements[i] = single("allowed" + i, valueFor(allowed[i]), type);
        }

        ValidationResult result = validate(elements);

        assertTrue(result.isValid(), result.issues().toString());
    }

    @Test
    void rejectsAllNormativelyProhibitedAndUnknownDatatypeSpellings() {
        String[] prohibited = {
                "normalizedString", "token", "language", "NMTOKEN", "NMTOKENS", "Name",
                "NCName", "ID", "IDREF", "IDREFS", "ENTITY", "ENTITIES", "NOTATION", "QName"
        };
        DataElement[] elements = new DataElement[prohibited.length + 3];
        for (int i = 0; i < prohibited.length; i++) {
            elements[i] = single("prohibited" + i, "opaque", "xsd:" + prohibited[i]);
        }
        elements[prohibited.length] = single("unknown", "opaque", "xsd:notAType");
        elements[prohibited.length + 1] = single("wrongCase", "opaque", "xsd:QName ");
        elements[prohibited.length + 2] = single("alternatePrefix", "opaque", "xs:integer");

        ValidationResult result = validate(elements);

        assertEquals(Set.of("elements[14].valueDataType", "elements[15].valueDataType",
                        "elements[16].valueDataType", "elements[0].valueDataType", "elements[1].valueDataType",
                        "elements[2].valueDataType", "elements[3].valueDataType", "elements[4].valueDataType",
                        "elements[5].valueDataType", "elements[6].valueDataType", "elements[7].valueDataType",
                        "elements[8].valueDataType", "elements[9].valueDataType", "elements[10].valueDataType",
                        "elements[11].valueDataType", "elements[12].valueDataType", "elements[13].valueDataType"),
                paths(result, "EN18223_VALUE_DATATYPE_SUPPORTED"));
    }

    @Test
    void acceptsOnlyBuiltInsInTheW3cXmlSchemaPartTwoEditionCitedByEn18223() {
        DataElement[] elements = {
                single("anyAtomicType", "value", "xsd:anyAtomicType"),
                single("yearMonthDuration", "P1Y", "xsd:yearMonthDuration"),
                single("dayTimeDuration", "P1D", "xsd:dayTimeDuration"),
                single("dateTimeStamp", "2024-01-01T00:00:00Z", "xsd:dateTimeStamp")
        };

        ValidationResult result = validate(elements);

        assertEquals(Set.of("elements[0].valueDataType", "elements[1].valueDataType",
                        "elements[2].valueDataType", "elements[3].valueDataType"),
                paths(result, "EN18223_VALUE_DATATYPE_SUPPORTED"));
    }

    @Test
    void checksEachExplicitTableSevenValueMappingAndItsLexicalForm() {
        DataElement[] valid = {
                single("string", "text", "xsd:string"),
                single("boolean", true, "xsd:boolean"),
                single("decimalNumber", new BigDecimal("1.25"), "xsd:decimal"),
                single("decimalString", "9007199254740993.125", "xsd:decimal"),
                single("decimalWhitespace", "  3.14\t", "xsd:decimal"),
                single("integer", new BigInteger("9007199254740993"), "xsd:integer"),
                single("float", 1.25f, "xsd:float"),
                single("double", 1.25d, "xsd:double"),
                single("date", "2024-02-29", "xsd:date"),
                single("dateTime", "2024-02-29T23:59:59Z", "xsd:dateTime"),
                single("leapSecond", "2016-12-31T23:59:60Z", "xsd:dateTime"),
                single("endOfDay", "2024-02-29T24:00:00Z", "xsd:dateTime"),
                single("uri", "urn:example:item%20one", "xsd:anyURI"),
                single("base64", "SGVsbG8=", "xsd:base64Binary"),
                single("base64Whitespace", " S G V s b G 8 = ", "xsd:base64Binary")
        };
        assertTrue(validate(valid).isValid());

        DataElement[] invalid = {
                single("string", 5, "xsd:string"),
                single("boolean", "true", "xsd:boolean"),
                single("decimal", "1e2", "xsd:decimal"),
                single("integer", new BigDecimal("1.25"), "xsd:integer"),
                single("float", "1.25", "xsd:float"),
                single("double", true, "xsd:double"),
                single("date", "2023-02-29", "xsd:date"),
                single("dateTime", "2024-02-29T23:59:59+00:00", "xsd:dateTime"),
                single("invalidLeapSecond", "2024-02-29T23:59:60Z", "xsd:dateTime"),
                single("invalidEndOfDay", "2024-02-29T24:00:01Z", "xsd:dateTime"),
                single("uri", "relative/path", "xsd:anyURI"),
                single("base64", "SGVsbG8!", "xsd:base64Binary"),
                single("base64Padding", "SGVsbG8", "xsd:base64Binary"),
                single("base64PadBits", "AB==", "xsd:base64Binary")
        };
        ValidationResult result = validate(invalid);
        assertEquals(Set.of("elements[0].value", "elements[1].value", "elements[2].value", "elements[3].value",
                        "elements[4].value", "elements[5].value", "elements[6].value", "elements[7].value",
                        "elements[8].value", "elements[9].value", "elements[10].value",
                        "elements[11].value", "elements[12].value", "elements[13].value"),
                paths(result, "EN18223_VALUE_MATCHES_DATATYPE"));
    }

    @Test
    void validatesDerivedIntegerSignsAndExactRangesWithoutOverflow() {
        DataElement[] elements = {
                single("positiveMin", BigInteger.ONE, "xsd:positiveInteger"),
                single("nonNegativeMin", BigInteger.ZERO, "xsd:nonNegativeInteger"),
                single("negativeMax", BigInteger.ONE.negate(), "xsd:negativeInteger"),
                single("byteMin", BigInteger.valueOf(-128), "xsd:byte"),
                single("byteMax", BigInteger.valueOf(127), "xsd:byte"),
                single("unsignedByteMax", BigInteger.valueOf(255), "xsd:unsignedByte"),
                single("shortMax", BigInteger.valueOf(32767), "xsd:short"),
                single("unsignedShortMax", BigInteger.valueOf(65535), "xsd:unsignedShort"),
                single("intMax", BigInteger.valueOf(Integer.MAX_VALUE), "xsd:int"),
                single("unsignedIntMax", new BigInteger("4294967295"), "xsd:unsignedInt"),
                single("longMin", BigInteger.valueOf(Long.MIN_VALUE), "xsd:long"),
                single("unsignedLongMax", new BigInteger("18446744073709551615"), "xsd:unsignedLong"),
                single("positiveZero", BigInteger.ZERO, "xsd:positiveInteger"),
                single("negativeZero", BigInteger.ZERO, "xsd:negativeInteger"),
                single("byteTooSmall", BigInteger.valueOf(-129), "xsd:byte"),
                single("unsignedByteTooLarge", BigInteger.valueOf(256), "xsd:unsignedByte"),
                single("unsignedLongTooLarge", new BigInteger("18446744073709551616"), "xsd:unsignedLong"),
                single("nonInteger", new BigDecimal("1.5"), "xsd:unsignedInt")
        };

        ValidationResult result = validate(elements);

        assertEquals(Set.of("elements[12].value", "elements[13].value", "elements[14].value",
                        "elements[15].value", "elements[16].value", "elements[17].value"),
                paths(result, "EN18223_VALUE_MATCHES_DATATYPE"));
    }

    @Test
    void validatesContainerTypeForScalarChildrenAndReportsChildTypeConflicts() {
        MultiValuedDataElement inherited = multi("inherited", "xsd:float",
                single("number", 1.5d, null), single("notNumber", "text", null));
        MultiValuedDataElement conflict = multi("conflict", "xsd:float",
                single("child", 2, "xsd:integer"));
        MultiValuedDataElement matching = multi("matching", "xsd:integer",
                single("child", 2, "xsd:integer"));

        ValidationResult result = validate(inherited, conflict, matching);

        assertEquals(Set.of("elements[0].elements[1].value"), paths(result, "EN18223_VALUE_MATCHES_DATATYPE"));
        assertEquals(Set.of("elements[1].elements[0].valueDataType"),
                paths(result, "EN18223_MULTI_HOMOGENEOUS_DATATYPE"));
    }

    @Test
    void rejectsDifferentDeclaredChildTypesWhenContainerTypeIsUnspecified() {
        MultiValuedDataElement heterogeneous = multi("numbers", null,
                single("integer", 1, "xsd:integer"), single("decimal", 1.5d, "xsd:double"));

        ValidationResult result = validate(heterogeneous);

        assertEquals(Set.of("elements[0].elements[1].valueDataType"),
                paths(result, "EN18223_MULTI_HOMOGENEOUS_DATATYPE"));
    }

    @Test
    void doesNotApplyContainerScalarCompatibilityToComplexChildren() {
        MultiValuedDataElement multi = multi("complex", "xsd:anyURI",
                collection("object", single("text", "relative", null)));

        ValidationResult result = validate(multi);

        assertTrue(result.isValid(), result.issues().toString());
    }

    @Test
    void formatValidationDoesNotRepeatSemanticMissingValueOrResourceErrors() {
        DigitalProductPassport passport = passport(
                single("missing", null, "xsd:string"),
                new RelatedResource.Builder().elementId("resource").build());

        ValidationResult format = new DataFormatValidator().validate(passport);
        ValidationResult semantic = new DppValidator().validate(passport);

        assertTrue(format.isValid(), format.issues().toString());
        assertFalse(semantic.isValid());
        assertTrue(semantic.issues().stream().anyMatch(issue -> issue.rule().equals("EN18223_SINGLE_VALUE_REQUIRED")));
        assertTrue(semantic.issues().stream().anyMatch(issue -> issue.rule().equals("EN18223_RELATED_RESOURCE_URL_REQUIRED")));
        assertTrue(new DataFormatValidator().validate(passport(single("untyped", "text", null))).isValid());
        assertThrows(IllegalArgumentException.class,
                () -> single("nan", Double.NaN, "xsd:double"));
        assertThrows(IllegalArgumentException.class,
                () -> single("positiveInfinity", Double.POSITIVE_INFINITY, "xsd:float"));
    }

    @Test
    void accumulatesIndependentNestedErrorsInDeterministicModelOrder() {
        DataElementCollection nested = collection("nested",
                new RelatedResource.Builder().elementId("resource")
                        .url("relative").contentType("application/").language("en").build(),
                collection("inner", single("value", "2023-02-29", "xsd:date")));

        ValidationResult result = validate(nested);

        assertEquals(List.of("elements[0].elements[0].url", "elements[0].elements[0].contentType",
                        "elements[0].elements[0].language", "elements[0].elements[1].elements[0].value"),
                result.issues().stream().map(ValidationIssue::path).toList());
    }

    private static Object valueFor(String type) {
        return switch (type) {
            case "string" -> "text";
            case "boolean" -> true;
            case "decimal", "float", "double" -> BigDecimal.ONE;
            case "nonPositiveInteger" -> BigInteger.ZERO;
            case "negativeInteger" -> BigInteger.ONE.negate();
            case "integer", "long", "int", "short", "byte",
                    "nonNegativeInteger", "unsignedLong", "unsignedInt", "unsignedShort", "unsignedByte",
                    "positiveInteger" -> BigInteger.ONE;
            case "date" -> "2024-02-29";
            case "dateTime" -> "2024-02-29T23:59:59Z";
            case "anyURI" -> "urn:example:value";
            case "base64Binary" -> "AA==";
            default -> Map.of("opaque", true);
        };
    }

    private static SingleValuedDataElement single(String id, Object value, String type) {
        return new SingleValuedDataElement.Builder().elementId(id).value(value).valueDataType(type).build();
    }

    private static MultiValuedDataElement multi(String id, String type, DataElement... values) {
        return new MultiValuedDataElement.Builder().elementId(id).valueDataType(type)
                .dataElements(List.of(values)).build();
    }

    private static DataElementCollection collection(String id, DataElement... values) {
        return (DataElementCollection) new DataElementCollection.Builder().elementId(id)
                .dataElements(List.of(values)).build();
    }

    private static ValidationResult validate(DataElement... elements) {
        return new DataFormatValidator().validate(passport(elements));
    }

    private static DigitalProductPassport passport(DataElement... elements) {
        return new DigitalProductPassport.Builder().dppHeader(new DppHeader.Builder().build())
                .dataElements(List.of(elements)).build();
    }

    private static Set<String> paths(ValidationResult result, String rule) {
        return result.issues().stream().filter(issue -> issue.rule().equals(rule))
                .map(ValidationIssue::path).collect(Collectors.toSet());
    }
}
