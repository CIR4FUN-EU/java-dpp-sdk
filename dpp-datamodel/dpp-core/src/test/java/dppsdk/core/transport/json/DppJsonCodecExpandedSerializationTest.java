package dppsdk.core.transport.json;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.model.SingleValuedDataElement;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dppsdk.core.transport.json.DppJsonTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DppJsonCodecExpandedSerializationTest {
    private final DppJsonCodec codec = new DppJsonCodec();

    @Test
    void writesFlattenedHeaderAndSingleElementInCanonicalOrder() throws Exception {
        DppHeader header = new DppHeader.Builder()
                .digitalProductPassportId("https://example.org/dpp/1234545")
                .uniqueProductIdentifier("https://example.org/1234545")
                .granularity(Granularity.ITEM).dppSchemaVersion("EN18223:2026")
                .dppStatus("active").lastUpdated(Instant.parse("2026-09-21T10:00:00Z"))
                .economicOperatorId("gxx:ppp456789").contentSpecificationIds(List.of())
                .build();
        DigitalProductPassport passport = new DigitalProductPassport.Builder().dppHeader(header)
                .dataElements(List.of(single("manufacturerName", "xsd:string", "ExampleCorp"))).build();

        String json = codec.toExpandedJson(passport);
        var actual = JSON.readTree(json);
        assertEquals(tree("expanded/single-valued.json"), actual);
        assertFalse(actual.has("dppHeader"));
        assertFalse(actual.has("facilityId"));
        List<String> properties = new ArrayList<>();
        actual.fieldNames().forEachRemaining(properties::add);
        assertEquals(List.of("digitalProductPassportId", "uniqueProductIdentifier", "granularity",
                "dppSchemaVersion", "dppStatus", "lastUpdated", "economicOperatorId",
                "contentSpecificationIds", "elements"), properties);
        assertEquals(json, codec.toExpandedJson(passport));
    }

    @Test
    void writesCompleteNestedPassport() throws Exception {
        assertEquals(tree("expanded/complete-nested-passport.json"),
                JSON.readTree(codec.toExpandedJson(completePassport())));
    }

    @Test
    void allowsPassportWithNoElements() throws Exception {
        DigitalProductPassport passport = new DigitalProductPassport.Builder()
                .dppHeader(header()).build();
        assertEquals(0, JSON.readTree(codec.toExpandedJson(passport)).get("elements").size());
    }

    @Test
    void omitsAbsentOptionalResourceFieldsAndKeepsEmptySpecifications() throws Exception {
        var resource = new dppsdk.core.model.RelatedResource.Builder()
                .elementId("manual").contentType("application/pdf")
                .url("https://example.org/manual.pdf").build();
        var header = header().toBuilder().facilityId(null).contentSpecificationIds(List.of()).build();
        var passport = new DigitalProductPassport.Builder().dppHeader(header)
                .dataElements(List.of(resource)).build();
        var json = JSON.readTree(codec.toExpandedJson(passport));
        assertFalse(json.has("facilityId"));
        assertEquals(0, json.get("contentSpecificationIds").size());
        assertEquals("application/pdf", json.get("elements").get(0).get("contentType").textValue());
        assertEquals("https://example.org/manual.pdf", json.get("elements").get(0).get("url").textValue());
        assertFalse(json.get("elements").get(0).has("language"));
        assertFalse(json.get("elements").get(0).has("resourceTitle"));
    }

    @Test
    void writesEquivalentObjectValuesWithDeterministicPropertyOrder() throws Exception {
        Map<String, Object> firstValue = new LinkedHashMap<>();
        firstValue.put("zeta", 2);
        firstValue.put("alpha", 1);
        Map<String, Object> secondValue = new LinkedHashMap<>();
        secondValue.put("alpha", 1);
        secondValue.put("zeta", 2);

        DigitalProductPassport first = passportWithObjectValue(firstValue);
        DigitalProductPassport second = passportWithObjectValue(secondValue);

        assertEquals(first, second);
        assertEquals(codec.toExpandedJson(first), codec.toExpandedJson(second));
        assertEquals(codec.toCompressedJson(first), codec.toCompressedJson(second));
        assertEquals("{\"alpha\":1,\"zeta\":2}",
                JSON.writeValueAsString(JSON.readTree(codec.toExpandedJson(first))
                        .at("/elements/0/value")));
    }

    private DigitalProductPassport passportWithObjectValue(Map<String, Object> value) {
        return new DigitalProductPassport.Builder().dppHeader(header())
                .dataElements(List.of(new SingleValuedDataElement.Builder()
                        .elementId("objectValue").value(value).build()))
                .build();
    }
}
