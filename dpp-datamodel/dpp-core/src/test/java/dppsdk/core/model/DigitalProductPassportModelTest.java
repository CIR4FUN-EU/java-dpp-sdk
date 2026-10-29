package dppsdk.core.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dppsdk.core.semantic.SemanticPath;

import static org.junit.jupiter.api.Assertions.*;

class DigitalProductPassportModelTest {

    @Test
    void headerAndPassportRetainEveryFieldAndDefensivelyCopyOrderedElements() {
        List<String> specifications = new ArrayList<>(List.of("spec-a", "spec-b"));
        DppHeader header = new DppHeader.Builder()
                .digitalProductPassportId("passport-id")
                .uniqueProductIdentifier("product-id")
                .granularity(Granularity.ITEM)
                .dppSchemaVersion("2026")
                .dppStatus("active")
                .lastUpdated(Instant.parse("2026-01-02T03:04:05Z"))
                .economicOperatorId("operator-id")
                .facilityId("facility-id")
                .contentSpecificationIds(specifications)
                .build();
        specifications.add("late");
        assertEquals(List.of("spec-a", "spec-b"), header.getContentSpecificationIds());
        assertThrows(UnsupportedOperationException.class, () -> header.getContentSpecificationIds().add("x"));
        assertEquals("passport-id", header.getDigitalProductPassportId());
        assertEquals("product-id", header.getUniqueProductIdentifier());
        assertEquals(Granularity.ITEM, header.getGranularity());
        assertEquals("2026", header.getDppSchemaVersion());
        assertEquals("active", header.getDppStatus());
        assertEquals(Instant.parse("2026-01-02T03:04:05Z"), header.getLastUpdated());
        assertEquals("operator-id", header.getEconomicOperatorId());
        assertEquals("facility-id", header.getFacilityId());
        assertEquals(header, header.toBuilder().build());
        assertNull(header.toBuilder().facilityId(null).build().getFacilityId());
        assertEquals("facility-id", header.getFacilityId());

        DataElement first = new SingleValuedDataElement.Builder()
                .elementId("first").dictionaryReference("dictionary:first")
                .value("chair").valueDataType("xsd:string").build();
        DataElement second = new RelatedResource.Builder()
                .elementId("second").contentType("text/plain")
                .url("https://example.com/resource").language("en")
                .resourceTitle("Instructions").build();
        List<DataElement> source = new ArrayList<>(List.of(first, second));
        DigitalProductPassport passport = new DigitalProductPassport.Builder()
                .dppHeader(header).dataElements(source).build();
        source.clear();
        assertEquals(header, passport.getDppHeader());
        assertEquals(List.of(first, second), passport.getDataElements());
        assertThrows(UnsupportedOperationException.class, () -> passport.getDataElements().clear());
        assertEquals(passport, passport.toBuilder().build());
        assertEquals(1, passport.toBuilder().dataElements(List.of(first)).build().getDataElements().size());
        assertEquals(2, passport.getDataElements().size());
    }

    @Test
    void elementSubtypesKeepInheritanceNestedOrderingAndOptionalProperties() {
        SingleValuedDataElement leaf = new SingleValuedDataElement.Builder()
                .elementId("leaf").value(7).valueDataType("xsd:integer").build();
        assertInstanceOf(DataElement.class, leaf);
        assertNull(leaf.getDictionaryReference());
        assertEquals(leaf, leaf.toBuilder().build());
        assertEquals(7, leaf.getValue());
        assertEquals(8, leaf.toBuilder().value(8).build().getValue());
        assertEquals(7, leaf.getValue());

        DataElementCollection nested = new DataElementCollection.Builder()
                .elementId("nested").dataElements(List.of(leaf)).build();
        DataElementCollection outer = new DataElementCollection.Builder()
                .elementId("outer").dictionaryReference("dictionary:outer")
                .dataElements(List.of(nested)).build();
        assertInstanceOf(DataElement.class, nested);
        assertEquals(List.of(nested), outer.getDataElements());
        assertEquals(List.of(leaf), ((DataElementCollection) outer.getDataElements().get(0)).getDataElements());
        assertEquals(outer, outer.toBuilder().build());

        List<DataElement> ordered = new ArrayList<>(List.of(outer, nested));
        MultiValuedDataElement multi = new MultiValuedDataElement.Builder()
                .elementId("multi")
                .dataElements(ordered).build();
        ordered.clear();
        assertInstanceOf(DataElement.class, multi);
        assertEquals(List.of(outer, nested), multi.getDataElements());
        assertThrows(UnsupportedOperationException.class, () -> multi.getDataElements().remove(0));
        assertEquals(multi, multi.toBuilder().build());
        assertNull(multi.getValueDataType());

        MultiLanguageValue english = new MultiLanguageValue.Builder()
                .value("chair").language("en").build();
        MultiLanguageValue german = new MultiLanguageValue.Builder()
                .value("Stuhl").language("de").build();
        List<MultiLanguageValue> translations = new ArrayList<>(List.of(english, german));
        MultiLanguageDataElement localized = new MultiLanguageDataElement.Builder()
                .elementId("localized").values(translations).build();
        translations.clear();
        assertInstanceOf(DataElement.class, localized);
        assertEquals(List.of(english, german), localized.getValues());
        assertThrows(UnsupportedOperationException.class, () -> localized.getValues().clear());
        assertEquals(localized, localized.toBuilder().build());
        assertEquals(english, english.toBuilder().build());
        assertEquals("chair", english.getValue());
        assertEquals("en", english.getLanguage());

        RelatedResource resource = new RelatedResource.Builder()
                .elementId("resource").contentType("application/pdf")
                .url("https://example.com/manual.pdf").language("en")
                .resourceTitle("Manual").build();
        assertInstanceOf(DataElement.class, resource);
        assertEquals(resource, resource.toBuilder().build());
        assertEquals("application/pdf", resource.getContentType());
        assertEquals("https://example.com/manual.pdf", resource.getUrl());
        assertEquals("en", resource.getLanguage());
        assertEquals("Manual", resource.getResourceTitle());
    }

    @Test
    void jsonCompatibleSingleValueIsDeeplyImmutableAndCanContainNull() {
        List<Object> sourceList = new ArrayList<>(List.of("first"));
        sourceList.add(null);
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("items", sourceList);
        source.put("missing", null);
        SingleValuedDataElement element = new SingleValuedDataElement.Builder()
                .elementId("complex").value(source).build();
        sourceList.add("late");
        source.put("extra", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> retained = (Map<String, Object>) element.getValue();
        assertEquals(java.util.Arrays.asList("first", null), retained.get("items"));
        assertTrue(retained.containsKey("missing"));
        assertFalse(retained.containsKey("extra"));
        assertThrows(UnsupportedOperationException.class, () -> retained.put("new", "value"));
        @SuppressWarnings("unchecked")
        List<Object> retainedList = (List<Object>) retained.get("items");
        assertThrows(UnsupportedOperationException.class, () -> retainedList.add("new"));
        assertEquals(element, element.toBuilder().build());
        assertThrows(IllegalArgumentException.class,
                () -> new SingleValuedDataElement.Builder().value(new StringBuilder("mutable")).build());
        List<Object> cyclic = new ArrayList<>();
        cyclic.add(cyclic);
        assertThrows(IllegalArgumentException.class,
                () -> new SingleValuedDataElement.Builder().value(cyclic).build());
    }

    @Test
    void findsNestedElementsByIdentifierOrChildIndex() {
        DataElement leaf = new SingleValuedDataElement.Builder().elementId("name").value("chair").build();
        DataElementCollection inner = new DataElementCollection.Builder().elementId("details")
                .dataElements(List.of(leaf)).build();
        DataElementCollection outer = new DataElementCollection.Builder().elementId("product")
                .dataElements(List.of(inner)).build();
        DigitalProductPassport passport = new DigitalProductPassport.Builder()
                .dppHeader(new DppHeader.Builder().digitalProductPassportId("dpp").build())
                .dataElements(List.of(outer)).build();

        assertEquals(java.util.Optional.of(leaf), passport.findDataElement(SemanticPath.of("product", "details", "name")));
        assertEquals(java.util.Optional.of(leaf), passport.findDataElement(SemanticPath.of("product", "0", "0")));
        assertTrue(passport.findDataElement(SemanticPath.of("product", "5")).isEmpty());
    }
}
