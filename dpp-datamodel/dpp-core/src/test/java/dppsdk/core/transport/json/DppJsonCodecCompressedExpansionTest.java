package dppsdk.core.transport.json;

import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.RelatedResource;
import dppsdk.core.model.SingleValuedDataElement;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static dppsdk.core.transport.json.DppJsonTestSupport.JSON;
import static dppsdk.core.transport.json.DppJsonTestSupport.completePassport;
import static org.junit.jupiter.api.Assertions.*;

class DppJsonCodecCompressedExpansionTest {
    private final DppJsonCodec codec = new DppJsonCodec();

    @Test
    void expandsEveryStandardOperationalShapeWithoutInventingSemanticMetadata() throws Exception {
        String compressed = codec.toCompressedJson(completePassport());
        var result = codec.expandCompressedJson(compressed);
        var elements = result.passport().getDataElements();

        assertInstanceOf(SingleValuedDataElement.class, elements.get(0));
        assertInstanceOf(DataElementCollection.class, elements.get(2));
        assertInstanceOf(MultiValuedDataElement.class, elements.get(3));
        assertInstanceOf(MultiValuedDataElement.class, elements.get(4));
        assertInstanceOf(RelatedResource.class, elements.get(5));
        assertInstanceOf(MultiLanguageDataElement.class, elements.get(6));
        assertNull(elements.get(0).getDictionaryReference());
        assertNull(((SingleValuedDataElement) elements.get(0)).getValueDataType());
        assertNull(((MultiValuedDataElement) elements.get(3)).getValueDataType());
        assertEquals(JSON.readTree(compressed), JSON.readTree(codec.toCompressedJson(result.passport())));
        assertFalse(result.issues().isEmpty());
    }

    @Test
    void rejectsOperationalStructuresThatCannotFormRequiredCanonicalElements() throws Exception {
        var root = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(
                codec.toCompressedJson(completePassport()));
        root.putNull("nullValue");
        assertThrows(DppJsonException.class, () -> codec.expandCompressedJson(root.toString()));
    }

    @Test
    void retainsStoredSemanticMetadataWhenOperationalValuesChange() throws Exception {
        var previous = completePassport();
        var compressed = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(
                codec.toCompressedJson(previous));
        compressed.put("manufacturerName", "Changed");

        var result = codec.expandCompressedJson(compressed.toString(), previous);

        assertEquals(previous.getDataElements().get(0).getDictionaryReference(),
                result.passport().getDataElements().get(0).getDictionaryReference());
        assertEquals(((SingleValuedDataElement) previous.getDataElements().get(0)).getValueDataType(),
                ((SingleValuedDataElement) result.passport().getDataElements().get(0)).getValueDataType());
    }

    @Test
    void convertsAnIndividualElementThroughCanonicalSdkTransport() throws Exception {
        var original = completePassport().getDataElements().get(0);
        String operational = codec.toCompressedElementJson(original);
        var restored = codec.expandCompressedElementJsonWithIssues(original.getElementId(), operational, original);
        assertEquals(original, restored.element());
        assertFalse(restored.issues().isEmpty());
        assertTrue(restored.issues().stream().allMatch(issue -> issue.path().contains(original.getElementId())));
    }

    @Test
    void compressedPatchKeepsKnownSingleValueSubtypeForObjectAndArrayValues() throws Exception {
        var objectValue = new SingleValuedDataElement.Builder().elementId("objectValue")
                .dictionaryReference("urn:dictionary:object").valueDataType("xsd:string")
                .value(Map.of("first", "old")).build();
        var arrayValue = new SingleValuedDataElement.Builder().elementId("arrayValue")
                .dictionaryReference("urn:dictionary:array").valueDataType("xsd:string")
                .value(List.of("old")).build();
        var previous = completePassport().toBuilder().dataElements(List.of(objectValue, arrayValue)).build();
        var compressed = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(codec.toCompressedJson(previous));
        compressed.withObject("objectValue").put("first", "new");
        compressed.withArray("arrayValue").set(0, JSON.getNodeFactory().textNode("new"));

        var restored = codec.expandCompressedJson(compressed.toString(), previous).passport();
        assertEquals(Map.of("first", "new"), ((SingleValuedDataElement) restored.getDataElements().get(0)).getValue());
        assertEquals(List.of("new"), ((SingleValuedDataElement) restored.getDataElements().get(1)).getValue());
        assertEquals("urn:dictionary:object", restored.getDataElements().get(0).getDictionaryReference());
        assertEquals("urn:dictionary:array", restored.getDataElements().get(1).getDictionaryReference());
    }

    @Test
    void emptyJsonContainersExpandAsValidSingleValues() throws Exception {
        var operational = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(
                codec.toCompressedJson(completePassport()));
        operational.putObject("emptyObject");
        operational.putArray("emptyArray");

        var expanded = codec.expandCompressedJson(operational.toString());
        var elements = expanded.passport().getDataElements();
        assertEquals(Map.of(), ((SingleValuedDataElement) elements.get(elements.size() - 2)).getValue());
        assertEquals(List.of(), ((SingleValuedDataElement) elements.get(elements.size() - 1)).getValue());
        assertTrue(expanded.issues().stream().anyMatch(issue -> issue.path().contains("emptyObject")));
        assertTrue(expanded.issues().stream().anyMatch(issue -> issue.path().contains("emptyArray")));
    }

    @Test
    void mixedJsonArrayRemainsASingleValueWithAnIssue() throws Exception {
        var operational = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(
                codec.toCompressedJson(completePassport()));
        operational.putArray("mixedValue").add("text").add(7);

        var expanded = codec.expandCompressedJson(operational.toString());
        var element = expanded.passport().getDataElements()
                .get(expanded.passport().getDataElements().size() - 1);
        assertInstanceOf(SingleValuedDataElement.class, element);
        assertEquals(List.of("text", 7), ((SingleValuedDataElement) element).getValue());
        assertTrue(expanded.issues().stream().anyMatch(issue ->
                issue.path().contains("mixedValue") && issue.reason().contains("mixed array")));
    }

    @Test
    void storedSubtypesTakePrecedenceOverAmbiguousCompressedShapes() throws Exception {
        var resourceLikeValue = new SingleValuedDataElement.Builder().elementId("resourceLike")
                .dictionaryReference("urn:dictionary:resource-like")
                .value(Map.of("contentType", "application/pdf", "url", "https://example.org/a",
                        "custom", "old")).build();
        var languageLikeValue = new SingleValuedDataElement.Builder().elementId("languageLike")
                .dictionaryReference("urn:dictionary:language-like")
                .value(List.of(Map.of("language", "en-GB", "value", "old"))).build();
        var previous = completePassport().toBuilder()
                .dataElements(List.of(resourceLikeValue, languageLikeValue)).build();
        var operational = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(
                codec.toCompressedJson(previous));
        operational.withObject("resourceLike").put("custom", "new");
        ((com.fasterxml.jackson.databind.node.ObjectNode) operational.withArray("languageLike").get(0))
                .put("value", "new");

        var restored = codec.expandCompressedJson(operational.toString(), previous).passport();
        assertEquals("new", ((Map<?, ?>) ((SingleValuedDataElement) restored.getDataElements().get(0))
                .getValue()).get("custom"));
        assertEquals("urn:dictionary:resource-like", restored.getDataElements().get(0).getDictionaryReference());
        assertEquals("urn:dictionary:language-like", restored.getDataElements().get(1).getDictionaryReference());
        assertInstanceOf(SingleValuedDataElement.class, restored.getDataElements().get(1));
    }

    @Test
    void compressedUpdatesCannotSilentlyChangeStoredStructuredSubtypes() throws Exception {
        var previous = completePassport();
        var original = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(
                codec.toCompressedJson(previous));

        for (int index : List.of(2, 3, 5)) {
            var changed = original.deepCopy();
            String id = previous.getDataElements().get(index).getElementId();
            if (index == 2) ((com.fasterxml.jackson.databind.node.ObjectNode) changed.get(id)).removeAll();
            if (index == 3) ((com.fasterxml.jackson.databind.node.ArrayNode) changed.get(id)).removeAll();
            if (index == 5) ((com.fasterxml.jackson.databind.node.ObjectNode) changed.get(id)).remove("url");
            assertThrows(DppJsonException.class, () -> codec.expandCompressedJson(changed.toString(), previous));
        }
    }

    @Test
    void compressedArrayItemUpdateRetainsItsStoredSingleValueMetadata() throws Exception {
        var item = new SingleValuedDataElement.Builder().elementId("originalItem")
                .dictionaryReference("urn:dictionary:item").valueDataType("xsd:string")
                .value("old").build();
        var multi = new MultiValuedDataElement.Builder().elementId("values")
                .dictionaryReference("urn:dictionary:values").valueDataType("xsd:string")
                .dataElements(List.of(item)).build();
        var previous = completePassport().toBuilder().dataElements(List.of(multi)).build();
        var operational = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(
                codec.toCompressedJson(previous));
        operational.withArray("values").set(0, JSON.valueToTree(Map.of("updated", "value")));

        var restored = (MultiValuedDataElement) codec.expandCompressedJson(
                operational.toString(), previous).passport().getDataElements().get(0);
        var restoredItem = (SingleValuedDataElement) restored.getDataElements().get(0);
        assertEquals("originalItem", restoredItem.getElementId());
        assertEquals("urn:dictionary:item", restoredItem.getDictionaryReference());
        assertEquals("xsd:string", restoredItem.getValueDataType());
        assertEquals(Map.of("updated", "value"), restoredItem.getValue());
    }

    @Test
    void compressedArrayItemUpdateRejectsLossOfStoredStructuredSubtype() throws Exception {
        var child = new SingleValuedDataElement.Builder().elementId("field").value("old").build();
        var item = new DataElementCollection.Builder().elementId("originalItem")
                .dictionaryReference("urn:dictionary:item").dataElements(List.of(child)).build();
        var multi = new MultiValuedDataElement.Builder().elementId("values")
                .dataElements(List.of(item)).build();
        var previous = completePassport().toBuilder().dataElements(List.of(multi)).build();
        var operational = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(
                codec.toCompressedJson(previous));
        ((com.fasterxml.jackson.databind.node.ObjectNode) operational.withArray("values").get(0)).removeAll();

        assertThrows(DppJsonException.class,
                () -> codec.expandCompressedJson(operational.toString(), previous));
    }

    @Test
    void resourceLikeObjectWithUnspecifiedFieldsKeepsOperationalData() throws Exception {
        var operational = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(
                codec.toCompressedJson(completePassport()));
        operational.putObject("resourceLike")
                .put("contentType", "application/pdf")
                .put("url", "https://example.org/a")
                .put("actField", "value");

        var expanded = codec.expandCompressedJson(operational.toString());
        var element = expanded.passport().getDataElements()
                .get(expanded.passport().getDataElements().size() - 1);
        assertInstanceOf(SingleValuedDataElement.class, element);
        assertEquals("value", ((Map<?, ?>) ((SingleValuedDataElement) element).getValue()).get("actField"));
        assertTrue(expanded.issues().stream().anyMatch(issue ->
                issue.path().contains("resourceLike") && issue.reason().contains("additional fields")));
    }
}
