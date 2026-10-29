package dppsdk.core.transport.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static dppsdk.core.transport.json.DppJsonTestSupport.fixture;
import static org.junit.jupiter.api.Assertions.*;

class DppJsonCodecFailureTest {
    private final DppJsonCodec codec = new DppJsonCodec();

    @Test
    void rejectsUnknownTypesPropertiesAndMalformedValues() throws Exception {
        String single = fixture("expanded/single-valued.json");
        DppJsonException malformed = assertThrows(DppJsonException.class,
                () -> codec.fromExpandedJson("{"));
        assertEquals("Failed to deserialize expanded DPP JSON", malformed.getMessage());
        assertNotNull(malformed.getCause());
        assertThrows(DppJsonException.class, () -> codec.fromExpandedJson("[]"));
        DppJsonException unknownType = assertThrows(DppJsonException.class,
                () -> codec.fromExpandedJson(single.replace("SingleValuedDataElement", "FutureType")));
        assertTrue(unknownType.getMessage().contains("unknown objectType 'FutureType'"));
        assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(single.replace("\"valueDataType\": \"xsd:string\"", "\"valueDataType\": []")));
        assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(single.replace("\"elementId\": \"manufacturerName\"", "\"elementId\": []")));
        assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(single.replace("\"elements\": [", "\"unexpected\": true, \"elements\": [")));
        assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(single.replace("\"item\"", "\"planet\"")));
        assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(single + " {}"));
        String invalidTimestamp = single.replace("2026-09-21T10:00:00Z", "not-an-instant");
        assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(invalidTimestamp));
    }

    @Test
    void rejectsDuplicateKeysAndAmbiguousMultiValuedMembers() throws Exception {
        String header = fixture("expanded/header-only.json");
        String single = fixture("expanded/single-valued.json");
        assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(header.replace("\"granularity\": \"item\"", "\"granularity\": \"item\", \"granularity\": \"batch\"")));
        String alternate = fixture("expanded/multi-valued-value-alternate.json");
        String both = alternate.replace("\"value\": [", "\"elements\": [], \"value\": [");
        DppJsonException bothAliases = assertThrows(DppJsonException.class,
                () -> codec.fromExpandedJson(both));
        assertTrue(bothAliases.getMessage().contains("exactly one of 'elements' or 'value'"));
        ObjectNode root = (ObjectNode) DppJsonTestSupport.JSON.readTree(alternate);
        ((ObjectNode) root.path("elements").get(0)).remove("value");
        String neither = DppJsonTestSupport.JSON.writeValueAsString(root);
        DppJsonException noAlias = assertThrows(DppJsonException.class,
                () -> codec.fromExpandedJson(neither));
        assertTrue(noAlias.getMessage().contains("exactly one of 'elements' or 'value'"));
        String duplicateNested = single.replace("\"value\": \"ExampleCorp\"", "\"value\": {\"a\": 1, \"a\": 2}");
        assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(duplicateNested));
    }

    @Test
    void enforcesRequiredTransportCardinalitiesWithoutCheckingFieldSemantics() throws Exception {
        var incompleteHeader = new dppsdk.core.model.DigitalProductPassport.Builder()
                .dppHeader(new dppsdk.core.model.DppHeader.Builder().build()).build();
        assertThrows(DppJsonException.class, () -> codec.toExpandedJson(incompleteHeader));
        assertThrows(DppJsonException.class, () -> codec.toCompressedJson(incompleteHeader));

        String missingHeaderField = fixture("expanded/header-only.json")
                .replace("  \"dppStatus\": \"active\",\n", "");
        assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(missingHeaderField));

        var emptyCollection = new dppsdk.core.model.DataElementCollection.Builder()
                .elementId("emptyGroup").build();
        var emptyMultiValue = new dppsdk.core.model.MultiValuedDataElement.Builder()
                .elementId("emptyValues").build();
        var emptyMultilingual = new dppsdk.core.model.MultiLanguageDataElement.Builder()
                .elementId("emptyLanguages").build();
        dppsdk.core.model.DataElement[] invalidElements = {
                emptyCollection, emptyMultiValue, emptyMultilingual};
        String[] types = {"DataElementCollection", "MultiValuedDataElement", "MultiLanguageDataElement"};
        String[] listFields = {"elements", "elements", "value"};
        for (int i = 0; i < invalidElements.length; i++) {
            dppsdk.core.model.DataElement invalid = invalidElements[i];
            assertThrows(DppJsonException.class, () -> codec.toExpandedJson(passport(invalid)));
            assertThrows(DppJsonException.class, () -> codec.toCompressedJson(passport(invalid)));
            String invalidJson = fixture("expanded/header-only.json").replace("\"elements\": []",
                    "\"elements\": [{\"elementId\":\"empty\",\"objectType\":\""
                            + types[i] + "\",\"" + listFields[i] + "\":[]}]");
            assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(invalidJson));
        }

        var missingResourceFields = new dppsdk.core.model.RelatedResource.Builder()
                .elementId("manual").build();
        assertThrows(DppJsonException.class, () -> codec.toExpandedJson(passport(missingResourceFields)));
    }

    @Test
    void rejectsNonFiniteValuesAndCompressedKeyCollisions() throws Exception {
        for (Number value : new Number[] {Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new dppsdk.core.model.SingleValuedDataElement.Builder()
                    .elementId("bad").value(value).build());
        }
        assertThrows(IllegalArgumentException.class, () -> new dppsdk.core.model.SingleValuedDataElement.Builder()
                .elementId("bad").value(new Object()).build());
        String validSingle = fixture("expanded/single-valued.json");
        for (String token : new String[] {"NaN", "Infinity", "-Infinity"}) {
            String invalidNumber = validSingle.replace("\"value\": \"ExampleCorp\"", "\"value\": " + token);
            assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(invalidNumber));
        }
        var first = new dppsdk.core.model.SingleValuedDataElement.Builder().elementId("same").value(1).build();
        var second = new dppsdk.core.model.SingleValuedDataElement.Builder().elementId("same").value(2).build();
        assertThrows(DppJsonException.class, () -> codec.toCompressedJson(passport(first, second)));
        for (String reservedId : new String[] {"digitalProductPassportId", "uniqueProductIdentifier",
                "granularity", "dppSchemaVersion", "dppStatus", "lastUpdated",
                "economicOperatorId", "facilityId", "contentSpecificationIds"}) {
            var reserved = new dppsdk.core.model.SingleValuedDataElement.Builder()
                    .elementId(reservedId).value("collision").build();
            assertThrows(DppJsonException.class, () -> codec.toCompressedJson(passport(reserved)), reservedId);
        }
        var nestedDuplicate = new dppsdk.core.model.DataElementCollection.Builder().elementId("group")
                .dataElements(java.util.List.of(first, second)).build();
        assertThrows(DppJsonException.class, () -> codec.toCompressedJson(passport(nestedDuplicate)));
        var nestedGroup = new dppsdk.core.model.DataElementCollection.Builder().elementId("outer")
                .dataElements(java.util.List.of(new dppsdk.core.model.DataElementCollection.Builder()
                        .elementId("inner").dataElements(java.util.List.of(first, second)).build())).build();
        assertThrows(DppJsonException.class, () -> codec.toCompressedJson(passport(nestedGroup)));
        var missingId = new dppsdk.core.model.SingleValuedDataElement.Builder().value(1).build();
        assertThrows(DppJsonException.class, () -> codec.toExpandedJson(passport(missingId)));
    }

    private static dppsdk.core.model.DigitalProductPassport passport(dppsdk.core.model.DataElement... elements) {
        return new dppsdk.core.model.DigitalProductPassport.Builder()
                .dppHeader(DppJsonTestSupport.header()).dataElements(java.util.List.of(elements)).build();
    }

}
