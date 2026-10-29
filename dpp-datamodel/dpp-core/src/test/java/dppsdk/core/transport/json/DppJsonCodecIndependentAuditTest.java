package dppsdk.core.transport.json;

import com.fasterxml.jackson.databind.JsonNode;
import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.MultiLanguageValue;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.RelatedResource;
import dppsdk.core.model.SingleValuedDataElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dppsdk.core.transport.json.DppJsonTestSupport.JSON;
import static dppsdk.core.transport.json.DppJsonTestSupport.header;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DppJsonCodecIndependentAuditTest {
    private final DppJsonCodec codec = new DppJsonCodec();

    @Test
    void expandedHeaderOnlyPassportAcceptsOmittedOptionalElements() throws Exception {
        DigitalProductPassport headerOnly = new DigitalProductPassport.Builder()
                .dppHeader(header()).dataElements(List.of()).build();
        JsonNode expanded = JSON.readTree(codec.toExpandedJson(headerOnly));
        ((com.fasterxml.jackson.databind.node.ObjectNode) expanded).remove("elements");

        assertEquals(headerOnly, codec.fromExpandedJson(expanded.toString()));
    }

    @Test
    void manuallyBuiltDeepPassportHasExpectedExpandedAndCompressedShapes() throws Exception {
        DataElementCollection deepest = collection("deepGroup", List.of(
                single("deepValue", null, "leaf")));
        DataElementCollection nested = collection("nestedGroup", List.of(
                single("nestedValue", "xsd:integer", 7),
                new MultiValuedDataElement.Builder().elementId("nestedAlternatives")
                        .dataElements(List.of(
                                collection("alternativeOne", List.of(single("partName", null, "one"))),
                                collection("alternativeTwo", List.of(single("partName", null, "two")))))
                        .build(),
                deepest));
        DigitalProductPassport passport = new DigitalProductPassport.Builder().dppHeader(header())
                .dataElements(List.of(
                        single("rootText", "xsd:string", "text"),
                        single("rootFlag", "xsd:boolean", true),
                        new RelatedResource.Builder().elementId("manual")
                                .contentType("application/pdf").url("https://example.org/manual.pdf")
                                .language("en-GB").resourceTitle("Manual").build(),
                        new MultiLanguageDataElement.Builder().elementId("description").values(List.of(
                                new MultiLanguageValue.Builder().value("One").language("en-IE").build(),
                                new MultiLanguageValue.Builder().value("Deux").language("fr-FR").build()))
                                .build(),
                        nested,
                        new MultiValuedDataElement.Builder().elementId("ratings").valueDataType("xsd:integer")
                                .dataElements(List.of(single("ratingOne", "xsd:integer", 1),
                                        single("ratingTwo", "xsd:integer", 2))).build()))
                .build();

        JsonNode expanded = JSON.readTree(codec.toExpandedJson(passport));
        assertFalse(expanded.has("dppHeader"));
        assertEquals("item", expanded.get("granularity").textValue());
        assertEquals("rootText", expanded.get("elements").get(0).get("elementId").textValue());
        assertEquals("SingleValuedDataElement", expanded.get("elements").get(0).get("objectType").textValue());
        assertEquals("xsd:string", expanded.get("elements").get(0).get("valueDataType").textValue());
        assertTrue(expanded.get("elements").get(1).get("value").booleanValue());
        assertEquals("application/pdf", expanded.at("/elements/2/contentType").textValue());
        assertEquals("Manual", expanded.at("/elements/2/resourceTitle").textValue());
        assertEquals("Deux", expanded.at("/elements/3/value/1/value").textValue());
        assertEquals("deepGroup", expanded.at("/elements/4/elements/2/elementId").textValue());
        assertTrue(expanded.at("/elements/4/elements/1/value/1/elements").isArray());
        assertEquals("ratings", expanded.at("/elements/5/elementId").textValue());
        assertTrue(expanded.at("/elements/5/value").isArray());
        assertFalse(expanded.at("/elements/5").has("elements"));
        assertEquals(passport, codec.fromExpandedJson(codec.toExpandedJson(passport)));

        JsonNode compressed = JSON.readTree(codec.toCompressedJson(passport));
        assertEquals("text", compressed.get("rootText").textValue());
        assertTrue(compressed.get("rootFlag").booleanValue());
        assertEquals("Manual", compressed.at("/manual/resourceTitle").textValue());
        assertEquals("https://example.org/manual.pdf", compressed.at("/manual/url").textValue());
        assertEquals("Deux", compressed.at("/description/1/value").textValue());
        assertEquals(7, compressed.at("/nestedGroup/nestedValue").intValue());
        assertEquals("leaf", compressed.at("/nestedGroup/deepGroup/deepValue").textValue());
        assertTrue(compressed.at("/nestedGroup/nestedAlternatives").isArray());
        assertEquals("two", compressed.at("/nestedGroup/nestedAlternatives/1/partName").textValue());
        assertEquals(2, compressed.at("/ratings/1").intValue());
        assertFalse(compressed.at("/nestedGroup").toString().contains("dictionaryReference"));
        assertFalse(compressed.at("/nestedGroup").toString().contains("valueDataType"));
    }

    private static DataElementCollection collection(String id, List<DataElement> children) {
        return new DataElementCollection.Builder().elementId(id).dataElements(children).build();
    }

    private static SingleValuedDataElement single(String id, String dataType, Object value) {
        return new SingleValuedDataElement.Builder().elementId(id).valueDataType(dataType).value(value).build();
    }
}
