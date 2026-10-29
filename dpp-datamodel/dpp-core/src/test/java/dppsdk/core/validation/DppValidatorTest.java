package dppsdk.core.validation;

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
import dppsdk.core.validation.format.DataFormatValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DppValidatorTest {
    private final DppValidator validator = new DppValidator();

    @Test
    void validMinimalPassportAllowsNoRootElementsAndNoContentSpecifications() {
        assertTrue(validator.validate(passport(validHeader(), List.of())).isValid());
    }

    @Test
    void allGranularityEnumValuesAndOpenStatusValuesAreValid() {
        for (Granularity granularity : Granularity.values()) {
            DppHeader header = validHeader().toBuilder()
                    .granularity(granularity).dppStatus("future legal-act status").build();
            assertTrue(validator.validate(passport(header, List.of())).isValid());
        }
    }

    @Test
    void reportsEachMissingRequiredHeaderFieldAndKeepsOptionalFieldsOptional() {
        List<HeaderCase> cases = List.of(
                new HeaderCase("digitalProductPassportId", b -> b.digitalProductPassportId(null)),
                new HeaderCase("uniqueProductIdentifier", b -> b.uniqueProductIdentifier(null)),
                new HeaderCase("granularity", b -> b.granularity(null)),
                new HeaderCase("dppSchemaVersion", b -> b.dppSchemaVersion(null)),
                new HeaderCase("dppStatus", b -> b.dppStatus(null)),
                new HeaderCase("lastUpdated", b -> b.lastUpdated(null)),
                new HeaderCase("economicOperatorId", b -> b.economicOperatorId(null)));

        for (HeaderCase headerCase : cases) {
            ValidationResult result = validator.validate(passport(
                    headerCase.mutation().apply(validHeader().toBuilder()).build(), List.of()));
            assertTrue(result.issues().stream().anyMatch(issue ->
                    issue.path().equals("header." + headerCase.field())
                            && issue.rule().equals(headerRule(headerCase.field()))));
        }
        assertTrue(validator.validate(passport(validHeader().toBuilder()
                .facilityId(null).contentSpecificationIds(List.of()).build(), List.of())).isValid());
    }

    @Test
    void requiredHeaderValuesArePresenceCheckedWithoutUriOrStatusEnumerations() {
        DppHeader header = validHeader().toBuilder()
                .digitalProductPassportId("not a URI")
                .uniqueProductIdentifier("unformatted product id")
                .economicOperatorId("unformatted operator id")
                .dppStatus("paused-by-future-law")
                .build();
        assertTrue(validator.validate(passport(header, List.of())).isValid());
    }

    @Test
    void usesStableEnRuleIdsAndKeepsRecommendationsAsWarnings() {
        DppHeader missing = validHeader().toBuilder()
                .digitalProductPassportId(null).uniqueProductIdentifier(null).build();
        ValidationResult missingResult = validator.validate(passport(missing, List.of()));
        assertHas(missingResult, "header.digitalProductPassportId", "EN18223_HEADER_DPP_ID_REQUIRED");
        assertHas(missingResult, "header.uniqueProductIdentifier", "EN18223_HEADER_PRODUCT_ID_REQUIRED");

        ValidationResult recommended = validator.validate(passport(validHeader().toBuilder()
                .digitalProductPassportId("passport").build(), List.of()));
        assertTrue(recommended.isValid());
        assertTrue(recommended.issues().stream().anyMatch(issue ->
                issue.rule().equals("EN18223_HEADER_DPP_ID_URI_RECOMMENDED")
                        && issue.severity() == ValidationIssue.Severity.WARNING));
    }

    @Test
    void reportsRequiredElementIdAndSiblingDuplicatesAtEveryScope() {
        DataElement missingId = single(null, "value", null);
        DataElement duplicateRoot = single("duplicate", "one", null);
        DataElement duplicateRootAgain = single("duplicate", "two", null);
        DataElementCollection collection = collection("collection", List.of(
                single("child", "one", null), single("child", "two", null)));
        MultiValuedDataElement multi = multi("multi", List.of(
                single("member", 1, null), single("member", 2, null)));

        ValidationResult result = validator.validate(passport(validHeader(), List.of(
                missingId, duplicateRoot, duplicateRootAgain, collection, multi)));

        assertHas(result, "elements[0].elementId", "EN18223_ELEMENT_ID_REQUIRED");
        assertHas(result, "elements[2].elementId", "EN18223_ELEMENT_ID_UNIQUE_WITHIN_LOCATION");
        assertHas(result, "elements[3].elements[1].elementId", "EN18223_ELEMENT_ID_UNIQUE_WITHIN_LOCATION");
        assertHas(result, "elements[4].elements[1].elementId", "EN18223_ELEMENT_ID_UNIQUE_WITHIN_LOCATION");
    }

    @Test
    void reportsBlankElementIdsAsMissingIdentifiers() {
        ValidationResult result = validator.validate(passport(validHeader(), List.of(
                single("", "value", null), single("   ", "value", null))));

        assertHas(result, "elements[0].elementId", "EN18223_ELEMENT_ID_REQUIRED");
        assertHas(result, "elements[1].elementId", "EN18223_ELEMENT_ID_REQUIRED");
    }

    @Test
    void sameElementIdsInIndependentBranchesAreNotDppWideErrors() {
        DataElementCollection left = collection("left", List.of(single("same", "left", null)));
        DataElementCollection right = collection("right", List.of(single("same", "right", null)));
        assertTrue(validator.validate(passport(validHeader(), List.of(left, right))).isValid());
    }

    @Test
    void collectionMustBeNonEmptyAndValidatesNestedChildrenButAllowsDifferentSubtypes() {
        DataElementCollection empty = collection("empty", List.of());
        MultiLanguageDataElement multilingual = multilingual("translated", List.of(
                new MultiLanguageValue.Builder().value("Chair").language("en").build()));
        DataElementCollection valid = collection("valid", List.of(
                single("text", "chair", null),
                resource("resource", "application/pdf", "not an RFC URL"),
                multilingual));

        ValidationResult result = validator.validate(passport(validHeader(), List.of(empty, valid)));

        assertHas(result, "elements[0].elements", "EN18223_COLLECTION_NON_EMPTY");
        assertFalse(result.issues().stream().anyMatch(issue -> issue.path().startsWith("elements[1]")));
    }

    @Test
    void multilingualCollectionMemberIsAValidCollectionChild() {
        DataElementCollection multilingualOnly = collection("multilingual-only", List.of(
                multilingual("translated", List.of(
                        new MultiLanguageValue.Builder().value("Chair").language("en").build()))));
        DataElementCollection withRequiredType = collection("mixed", List.of(
                multilingual("translated", List.of(
                        new MultiLanguageValue.Builder().value("Chair").language("en").build())),
                single("name", "Chair", null)));
        DataElementCollection withRelatedResource = collection("resource-and-language", List.of(
                multilingual("translated", List.of(
                        new MultiLanguageValue.Builder().value("Chair").language("en").build())),
                resource("manual", "application/pdf", "https://example.org/manual")));

        ValidationResult result = validator.validate(passport(validHeader(), List.of(
                multilingualOnly, withRequiredType, withRelatedResource)));

        assertTrue(result.isValid(), result.issues().toString());
    }

    @Test
    void rejectsDistinctTreesThatFlattenToTheSameAbsoluteElementPath() {
        DataElement flattened = single("a.b", "first", null);
        DataElementCollection nested = collection("a", List.of(single("b", "second", null)));
        ValidationResult result = validator.validate(passport(validHeader(), List.of(flattened, nested)));
        assertHas(result, "elements[1].elements[0].elementId", "EN18223_ABSOLUTE_PATH_UNIQUE");
    }

    @Test
    void singleValueDataTypeIsOptionalAndNullValueCannotSatisfyRequiredValue() {
        DataElement valid = single("single", 9L, null);
        DataElement missingValue = single("missing", null, "xsd:string");
        ValidationResult result = validator.validate(passport(validHeader(), List.of(valid, missingValue)));
        assertHas(result, "elements[1].value", "EN18223_SINGLE_VALUE_REQUIRED");
        assertEquals(1, result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count());
    }

    @Test
    void multivaluedElementsMustBeNonemptyAndHomogeneousByConcreteSubtype() {
        MultiValuedDataElement empty = multi("empty", List.of());
        MultiValuedDataElement homogeneousScalars = multi("scalars", List.of(
                single("one", 1, "xsd:integer"), single("two", 2L, null)));
        MultiValuedDataElement homogeneousCollections = multi("objects", List.of(
                collection("first", List.of(single("name", "A", null))),
                collection("second", List.of(single("name", "B", null)))));
        MultiValuedDataElement mixed = multi("mixed", List.of(
                single("scalar", 1, null), collection("object", List.of(single("child", true, null)))));

        ValidationResult result = validator.validate(passport(validHeader(), List.of(
                empty, homogeneousScalars, homogeneousCollections, mixed)));

        assertHas(result, "elements[0].elements", "EN18223_MULTI_NON_EMPTY");
        assertHas(result, "elements[3].elements[1]", "EN18223_MULTI_HOMOGENEOUS_KIND");
        assertEquals(2, result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count());
    }

    @Test
    void multivaluedSingleValuesMustHaveTheSameJsonTypeWithoutDataTypeMetadata() {
        MultiValuedDataElement numberAndString = multi("number-string", List.of(
                single("first", 1, null), single("second", "two", null)));
        MultiValuedDataElement numberAndBoolean = multi("number-boolean", List.of(
                single("first", 1, null), single("second", true, null)));
        MultiValuedDataElement stringAndBoolean = multi("string-boolean", List.of(
                single("first", "one", null), single("second", false, null)));

        ValidationResult result = validator.validate(passport(validHeader(), List.of(
                numberAndString, numberAndBoolean, stringAndBoolean)));

        assertHas(result, "elements[0].elements[1]", "EN18223_JSON_NATIVE_TYPE_MATCHES_XSD");
        assertHas(result, "elements[1].elements[1]", "EN18223_JSON_NATIVE_TYPE_MATCHES_XSD");
        assertHas(result, "elements[2].elements[1]", "EN18223_JSON_NATIVE_TYPE_MATCHES_XSD");
        assertEquals(3, result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count());
    }

    @Test
    void multiValuedNumbersUseJsonNumberTypeAcrossJavaNumberClasses() {
        MultiValuedDataElement integersAndDecimals = multi("numbers", List.of(
                single("byte", (byte) 1, null),
                single("short", (short) 2, null),
                single("integer", 3, null),
                single("long", 4L, null),
                single("big-integer", new BigInteger("5"), null),
                single("float", 1.0f, null),
                single("double", 2.5d, null),
                single("big-decimal", new BigDecimal("4.25"), null)));

        assertTrue(validator.validate(passport(validHeader(), List.of(integersAndDecimals))).isValid());
    }

    @Test
    void multivaluedHomogeneousStringsBooleansAndJsonContainersAreValid() {
        MultiValuedDataElement strings = multi("strings", List.of(
                single("first", "one", null), single("second", "two", null)));
        MultiValuedDataElement booleans = multi("booleans", List.of(
                single("first", true, null), single("second", false, null)));
        MultiValuedDataElement objectsWithDifferentShapes = multi("objects", List.of(
                single("first", java.util.Map.of("a", 1), null),
                single("second", java.util.Map.of("b", 2, "c", 3), null)));
        MultiValuedDataElement arrays = multi("arrays", List.of(
                single("first", List.of(1, "mixed", true), null),
                single("second", List.of("different", 2), null)));

        assertTrue(validator.validate(passport(validHeader(), List.of(
                strings, booleans, objectsWithDifferentShapes, arrays))).isValid());
    }

    @Test
    void multiValuedObjectSubtypesKeepTheirCompressedJsonTypeWithoutShapeEquality() {
        DataElementCollection first = collection("first", List.of(single("name", "A", null)));
        DataElementCollection second = collection("second", List.of(
                single("different-field", "B", null), single("another-field", 2, null)));
        MultiValuedDataElement collections = multi("objects", List.of(first, second));

        MultiValuedDataElement resources = multi("resources", List.of(
                resource("one", "application/pdf", "https://example.org/one"),
                resource("two", "text/plain", "https://example.org/two")));

        MultiValuedDataElement nestedArrays = multi("nested-arrays", List.of(
                multi("first-array", List.of(single("first-value", 1, null))),
                multi("second-array", List.of(single("second-value", "anything", null)))));

        assertTrue(validator.validate(passport(validHeader(), List.of(
                collections, resources, nestedArrays))).isValid());
    }

    @Test
    void multivaluedJsonTypeMismatchHasTheFullNestedElementPath() {
        MultiValuedDataElement nested = multi("measurements", List.of(
                single("first", 1, null), single("second", "two", null)));
        DataElementCollection container = collection("container", List.of(
                single("label", "measurements", null), nested));

        ValidationResult result = validator.validate(passport(validHeader(), List.of(container)));

        assertHas(result, "elements[0].elements[1].elements[1]", "EN18223_JSON_NATIVE_TYPE_MATCHES_XSD");
        assertEquals(1, result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count());
    }

    @Test
    void declaredValueDataTypeChecksRemainIndependentOfJsonTypeHomogeneity() {
        MultiValuedDataElement values = new MultiValuedDataElement.Builder()
                .elementId("values").valueDataType("xsd:string")
                .dataElements(List.of(single("number", 1, null), single("string", "two", null)))
                .build();
        DigitalProductPassport passport = passport(validHeader(), List.of(values));

        ValidationResult semantic = validator.validate(passport);
        ValidationResult format = new DataFormatValidator().validate(passport);

        assertHas(semantic, "elements[0].elements[1]", "EN18223_JSON_NATIVE_TYPE_MATCHES_XSD");
        assertHas(format, "elements[0].elements[0].value", "EN18223_VALUE_MATCHES_DATATYPE");
        assertEquals(1, semantic.issues().size());
        assertEquals(1, format.issues().size());
    }

    @Test
    void multivaluedMultilanguageChildrenSerializeAsHomogeneousArrays() {
        MultiValuedDataElement multilingual = multi("translated-values", List.of(
                multilingual("first", List.of(new MultiLanguageValue.Builder()
                        .value("Chair").language("en-GB").build())),
                multilingual("second", List.of(new MultiLanguageValue.Builder()
                        .value("Silla").language("es-ES").build()))));

        assertTrue(validator.validate(passport(validHeader(), List.of(multilingual))).isValid());
    }

    @Test
    void recursivelyValidatesMultivaluedElementsNestedInCollections() {
        MultiValuedDataElement nestedMulti = multi("multi", List.of(
                collection("member", List.of(resource("resource", null, "unvalidated URL"))),
                collection("member", List.of(single("value", "valid", null)))));
        DataElementCollection root = collection("root", List.of(
                collection("nested", List.of(nestedMulti))));

        ValidationResult result = validator.validate(passport(validHeader(), List.of(root)));

        assertHas(result, "elements[0].elements[0].elements[0].elements[0].elements[0].contentType",
                "EN18223_RELATED_RESOURCE_CONTENT_TYPE_REQUIRED");
        assertHas(result, "elements[0].elements[0].elements[0].elements[1].elementId",
                "EN18223_ELEMENT_ID_UNIQUE_WITHIN_LOCATION");
        assertEquals(3, result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count());
    }

    @Test
    void relatedResourceRequiresOnlyContentTypeAndUrlPresence() {
        RelatedResource missingContentType = resource("no-type", null, "anything");
        RelatedResource missingUrl = resource("no-url", "anything", null);
        RelatedResource optionalFieldsAbsent = new RelatedResource.Builder()
                .elementId("minimal-resource").contentType("unvalidated type").url("not a URL").build();

        ValidationResult result = validator.validate(passport(validHeader(), List.of(
                missingContentType, missingUrl, optionalFieldsAbsent)));

        assertHas(result, "elements[0].contentType", "EN18223_RELATED_RESOURCE_CONTENT_TYPE_REQUIRED");
        assertHas(result, "elements[1].url", "EN18223_RELATED_RESOURCE_URL_REQUIRED");
        assertEquals(2, result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count());
    }

    @Test
    void multilingualValuesMustBeNonemptyAndHaveValueAndLanguage() {
        MultiLanguageDataElement empty = multilingual("empty", List.of());
        MultiLanguageDataElement invalidMember = multilingual("invalid", List.of(
                new MultiLanguageValue.Builder().language(null).build()));
        MultiLanguageDataElement repeatedLanguages = multilingual("repeated", List.of(
                new MultiLanguageValue.Builder().value("Chair").language("en").build(),
                new MultiLanguageValue.Builder().value("Seat").language("en").build()));

        ValidationResult result = validator.validate(passport(validHeader(), List.of(
                empty, invalidMember, repeatedLanguages)));

        assertHas(result, "elements[0].values", "EN18223_MULTILANGUAGE_NON_EMPTY");
        assertHas(result, "elements[1].values[0].value", "EN18223_MULTILANGUAGE_VALUE_REQUIRED");
        assertHas(result, "elements[1].values[0].language", "EN18223_MULTILANGUAGE_LANGUAGE_REQUIRED");
        assertEquals(3, result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count());
    }

    @Test
    void aggregatesIndependentViolationsInDeterministicTraversalOrder() {
        DppHeader incomplete = validHeader().toBuilder()
                .digitalProductPassportId(null).uniqueProductIdentifier(null).build();
        DataElementCollection nested = collection("outer", List.of(
                collection("inner", List.of()),
                new RelatedResource.Builder().elementId("resource").build()));

        ValidationResult result = validator.validate(passport(incomplete, List.of(nested)));

        assertFalse(result.isValid());
        assertEquals(List.of(
                "header.digitalProductPassportId",
                "header.uniqueProductIdentifier",
                "elements[0].elements[0].elements",
                "elements[0].elements[1].contentType",
                "elements[0].elements[1].url"),
                result.issues().stream().map(ValidationIssue::path).toList());
    }

    @Test
    void issueCollectionIsImmutable() {
        ValidationResult result = validator.validate(passport(
                validHeader().toBuilder().dppStatus(null).build(), List.of()));
        assertThrows(UnsupportedOperationException.class,
                () -> result.issues().add(new ValidationIssue("x", "X", "x")));
    }

    @Test
    void nullPassportProducesAUsefulStructuredIssue() {
        ValidationResult result = validator.validate(null);
        assertFalse(result.isValid());
        assertHas(result, "dpp", "DPP_REQUIRED");
    }

    private static String headerRule(String field) {
        return switch (field) {
            case "digitalProductPassportId" -> "EN18223_HEADER_DPP_ID_REQUIRED";
            case "uniqueProductIdentifier" -> "EN18223_HEADER_PRODUCT_ID_REQUIRED";
            case "granularity" -> "EN18223_HEADER_GRANULARITY_REQUIRED";
            case "dppSchemaVersion" -> "EN18223_HEADER_SCHEMA_VERSION_REQUIRED";
            case "dppStatus" -> "EN18223_HEADER_STATUS_REQUIRED";
            case "lastUpdated" -> "EN18223_HEADER_LAST_UPDATED_REQUIRED";
            case "economicOperatorId" -> "EN18223_HEADER_ECONOMIC_OPERATOR_ID_REQUIRED";
            default -> throw new IllegalArgumentException(field);
        };
    }

    private static void assertHas(ValidationResult result, String path, String rule) {
        assertTrue(result.issues().stream().anyMatch(issue ->
                issue.path().equals(path) && issue.rule().equals(rule)),
                () -> "Expected " + rule + " at " + path + " but got " + result.issues());
    }

    private static DppHeader validHeader() {
        return new DppHeader.Builder()
                .digitalProductPassportId("urn:example:passport")
                .uniqueProductIdentifier("product")
                .granularity(Granularity.ITEM)
                .dppSchemaVersion("2026")
                .dppStatus("active")
                .lastUpdated(Instant.parse("2026-01-01T00:00:00Z"))
                .economicOperatorId("operator")
                .build();
    }

    private static DigitalProductPassport passport(DppHeader header, List<DataElement> elements) {
        return new DigitalProductPassport.Builder().dppHeader(header).dataElements(elements).build();
    }

    private static SingleValuedDataElement single(String id, Object value, String valueDataType) {
        return new SingleValuedDataElement.Builder().elementId(id).value(value)
                .valueDataType(valueDataType).build();
    }

    private static DataElementCollection collection(String id, List<DataElement> elements) {
        return (DataElementCollection) new DataElementCollection.Builder()
                .elementId(id).dataElements(elements).build();
    }

    private static MultiValuedDataElement multi(String id, List<DataElement> elements) {
        return new MultiValuedDataElement.Builder().elementId(id).dataElements(elements).build();
    }

    private static RelatedResource resource(String id, String contentType, String url) {
        return new RelatedResource.Builder().elementId(id)
                .contentType(contentType).url(url).build();
    }

    private static MultiLanguageDataElement multilingual(String id, List<MultiLanguageValue> values) {
        return new MultiLanguageDataElement.Builder().elementId(id).values(values).build();
    }

    private record HeaderCase(String field, UnaryOperator<DppHeader.Builder> mutation) { }
}
