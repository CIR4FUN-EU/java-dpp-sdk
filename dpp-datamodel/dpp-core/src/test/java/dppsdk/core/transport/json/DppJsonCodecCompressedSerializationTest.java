package dppsdk.core.transport.json;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static dppsdk.core.transport.json.DppJsonTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DppJsonCodecCompressedSerializationTest {
    private final DppJsonCodec codec = new DppJsonCodec();

    @Test
    void writesCompleteOperationalCompressedFixture() throws Exception {
        assertEquals(tree("compressed/complete-nested-passport.json"),
                JSON.readTree(codec.toCompressedJson(completePassport())));
    }

    @Test
    void writesHeaderOnlyCompressedFixture() throws Exception {
        var passport = new dppsdk.core.model.DigitalProductPassport.Builder()
                .dppHeader(header().toBuilder().contentSpecificationIds(java.util.List.of()).build()).build();
        assertEquals(tree("compressed/header-only.json"),
                JSON.readTree(codec.toCompressedJson(passport)));
    }

    @Test
    void writesEachCompressedDataElementFormToItsFixture() throws Exception {
        var complete = completePassport();
        assertFixture("compressed/single-valued.json", complete.getDataElements().get(0));
        assertFixture("compressed/collection.json", complete.getDataElements().get(2));
        assertFixture("compressed/multi-valued-native.json", complete.getDataElements().get(3));
        assertFixture("compressed/multi-valued-object.json", complete.getDataElements().get(4));
        assertFixture("compressed/related-resource.json", complete.getDataElements().get(5));
        assertFixture("compressed/multi-language.json", complete.getDataElements().get(6));
    }

    @Test
    void rejectsCompressedMultiValuedArraysWithMixedJsonTypes() {
        var number = new dppsdk.core.model.SingleValuedDataElement.Builder()
                .elementId("number").value(1).build();
        var text = new dppsdk.core.model.SingleValuedDataElement.Builder()
                .elementId("text").value("1").build();
        var mixed = new dppsdk.core.model.MultiValuedDataElement.Builder()
                .elementId("mixed").dataElements(List.of(number, text)).build();

        var passport = new dppsdk.core.model.DigitalProductPassport.Builder()
                .dppHeader(header()).dataElements(List.of(mixed)).build();
        assertThrows(DppJsonException.class, () -> codec.toCompressedJson(passport));
    }

    @Test
    void acceptsDifferentJavaNumbersBecauseBothSerializeAsJsonNumbers() throws Exception {
        var integer = new dppsdk.core.model.SingleValuedDataElement.Builder()
                .elementId("integer").value(1).build();
        var decimal = new dppsdk.core.model.SingleValuedDataElement.Builder()
                .elementId("decimal").value(new BigDecimal("2.5")).build();
        var mixed = new dppsdk.core.model.MultiValuedDataElement.Builder()
                .elementId("numbers").dataElements(List.of(integer, decimal)).build();
        var passport = new dppsdk.core.model.DigitalProductPassport.Builder()
                .dppHeader(header()).dataElements(List.of(mixed)).build();

        assertEquals("[1,2.5]", JSON.writeValueAsString(
                JSON.readTree(codec.toCompressedJson(passport)).get("numbers")));
    }

    @Test
    void acceptsObjectItemsWithDifferentFieldLayouts() throws Exception {
        var first = new dppsdk.core.model.DataElementCollection.Builder()
                .elementId("first").dataElements(List.of(
                        new dppsdk.core.model.SingleValuedDataElement.Builder()
                                .elementId("name").value("one").build())).build();
        var second = new dppsdk.core.model.DataElementCollection.Builder()
                .elementId("second").dataElements(List.of(
                        new dppsdk.core.model.SingleValuedDataElement.Builder()
                                .elementId("name").value("two").build(),
                        new dppsdk.core.model.SingleValuedDataElement.Builder()
                                .elementId("count").value(2).build())).build();
        var parts = new dppsdk.core.model.MultiValuedDataElement.Builder()
                .elementId("parts").dataElements(List.of(first, second)).build();
        var passport = new dppsdk.core.model.DigitalProductPassport.Builder()
                .dppHeader(header()).dataElements(List.of(parts)).build();

        assertEquals(2, JSON.readTree(codec.toCompressedJson(passport)).get("parts").size());
    }

    private void assertFixture(String path, dppsdk.core.model.DataElement element) throws Exception {
        var passport = new dppsdk.core.model.DigitalProductPassport.Builder()
                .dppHeader(header().toBuilder().contentSpecificationIds(List.of()).build())
                .dataElements(List.of(element)).build();
        assertEquals(tree(path), JSON.readTree(codec.toCompressedJson(passport)), path);
    }
}
