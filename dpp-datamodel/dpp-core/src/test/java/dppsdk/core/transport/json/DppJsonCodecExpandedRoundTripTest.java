package dppsdk.core.transport.json;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.validation.DppValidator;
import dppsdk.core.validation.format.DataFormatValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.math.BigInteger;

import static dppsdk.core.transport.json.DppJsonTestSupport.fixture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DppJsonCodecExpandedRoundTripTest {
    private final DppJsonCodec codec = new DppJsonCodec();

    @Test
    void roundTripsAllStandardExpandedFixtures() throws Exception {
        List<String> fixtures = List.of("header-only", "single-valued", "collection",
                "nested-collection", "multi-valued-elements", "multi-valued-value-alternate",
                "related-resource", "multi-language", "complete-nested-passport");
        for (String name : fixtures) {
            DigitalProductPassport parsed = codec.fromExpandedJson(fixture("expanded/" + name + ".json"));
            assertTrue(new DppValidator().validate(parsed).isValid(), name + " semantic validation");
            assertTrue(new DataFormatValidator().validate(parsed).isValid(), name + " format validation");
            assertEquals(parsed, codec.fromExpandedJson(codec.toExpandedJson(parsed)), name);
        }
    }

    @Test
    void roundTripsEverySupportedJavaNumberRangeAndNestedJsonValues() throws Exception {
        BigInteger largeInteger = new BigInteger("1234567890123456789012345678901234567890");
        BigDecimal preciseDecimal = new BigDecimal("12345678901234567890.123456789012345678901234567890");
        List<dppsdk.core.model.DataElement> values = List.of(
                new dppsdk.core.model.SingleValuedDataElement.Builder().elementId("byteValue")
                        .valueDataType("xsd:integer").value((byte) 12).build(),
                new dppsdk.core.model.SingleValuedDataElement.Builder().elementId("shortValue")
                        .valueDataType("xsd:integer").value((short) 32000).build(),
                new dppsdk.core.model.SingleValuedDataElement.Builder().elementId("integerValue")
                        .valueDataType("xsd:integer").value(42).build(),
                new dppsdk.core.model.SingleValuedDataElement.Builder().elementId("longValue")
                        .valueDataType("xsd:integer").value(42L).build(),
                new dppsdk.core.model.SingleValuedDataElement.Builder().elementId("bigIntegerValue")
                        .valueDataType("xsd:integer").value(largeInteger).build(),
                new dppsdk.core.model.SingleValuedDataElement.Builder().elementId("bigDecimalValue")
                        .valueDataType("xsd:decimal").value(preciseDecimal).build(),
                new dppsdk.core.model.SingleValuedDataElement.Builder().elementId("floatValue")
                        .valueDataType("xsd:float").value(1.25f).build(),
                new dppsdk.core.model.SingleValuedDataElement.Builder().elementId("decimalValue")
                        .valueDataType("xsd:double").value(25.5d).build(),
                new dppsdk.core.model.SingleValuedDataElement.Builder().elementId("jsonObject")
                        .value(Map.of("mixed", List.of("text", true, 42L, preciseDecimal,
                                Map.of("nested", "object"))))
                        .build());
        var original = new dppsdk.core.model.DigitalProductPassport.Builder()
                .dppHeader(DppJsonTestSupport.header()).dataElements(values).build();

        var expanded = DppJsonTestSupport.JSON.readTree(codec.toExpandedJson(original));
        assertTrue(expanded.get("elements").get(0).get("value").isIntegralNumber());
        assertEquals(largeInteger, expanded.get("elements").get(4).get("value").bigIntegerValue());
        assertEquals(preciseDecimal, expanded.get("elements").get(5).get("value").decimalValue());
        assertTrue(expanded.get("elements").get(6).get("value").isFloatingPointNumber());
        assertTrue(expanded.get("elements").get(7).get("value").isFloatingPointNumber());
        assertEquals(original, codec.fromExpandedJson(codec.toExpandedJson(original)));

        var compressed = DppJsonTestSupport.JSON.readTree(codec.toCompressedJson(original));
        assertEquals(largeInteger, compressed.get("bigIntegerValue").bigIntegerValue());
        assertEquals(preciseDecimal, compressed.get("bigDecimalValue").decimalValue());
        assertTrue(compressed.get("floatValue").isFloatingPointNumber());
    }

    @Test
    void roundTripsNullSingleValueAndReadsNullOptionalFields() throws Exception {
        var passport = new dppsdk.core.model.DigitalProductPassport.Builder()
                .dppHeader(DppJsonTestSupport.header().toBuilder().facilityId(null).build())
                .dataElements(List.of(new dppsdk.core.model.SingleValuedDataElement.Builder()
                        .elementId("emptyValue").value(null).build()))
                .build();
        var output = codec.toExpandedJson(passport);
        assertEquals(passport, codec.fromExpandedJson(output));
        String nullFacility = DppJsonTestSupport.fixture("expanded/header-only.json")
                .replace("\"facilityId\": \"gxx:xxx987654\"", "\"facilityId\": null");
        assertNull(codec.fromExpandedJson(nullFacility).getDppHeader().getFacilityId());
    }
}
