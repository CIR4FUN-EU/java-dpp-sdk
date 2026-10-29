package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileElementSupportTest {
    @Test
    void addingKnownElementUsesProfileOrderAndKeepsUnknownRelativeOrder() {
        DataElement before = scalar("extension-before", "a");
        DataElement existing = scalar("name", "old");
        DataElement after = scalar("extension-after", "b");
        DataElementCollection source = new DataElementCollection.Builder()
                .elementId("nameplate")
                .dataElements(List.of(before, existing, after))
                .build();

        DataElementCollection edited = ProfileElementSupport.patchScalar(
                source, "gtinCode", "04012345678901", List.of("gtinCode", "name"));

        assertEquals(List.of("extension-before", "gtinCode", "name", "extension-after"),
                edited.getDataElements().stream().map(DataElement::getElementId).toList());
        assertEquals(before, edited.getDataElements().get(0));
        assertEquals(after, edited.getDataElements().get(3));
    }

    @Test
    void replacingKnownElementRetainsItsSlot() {
        DataElementCollection source = new DataElementCollection.Builder()
                .elementId("nameplate")
                .dataElements(List.of(scalar("gtinCode", "old"), scalar("extension", "x")))
                .build();

        DataElementCollection edited = ProfileElementSupport.patchScalar(
                source, "gtinCode", "new", List.of("gtinCode", "name"));

        assertEquals(List.of("gtinCode", "extension"),
                edited.getDataElements().stream().map(DataElement::getElementId).toList());
        assertEquals("new", ((SingleValuedDataElement) edited.getDataElements().get(0)).getValue());
    }

    @Test
    void duplicateReservedCharacteristicsRootsAreRejectedWhenWrappingCore() {
        DigitalProductPassport source = TestDataFactory.validDpp().asDigitalProductPassport();
        DataElementCollection characteristics = new DataElementCollection.Builder().elementId("characteristics").build();
        DigitalProductPassport passport = source.toBuilder()
                .dataElements(List.of(characteristics, characteristics)).build();
        assertThrows(IllegalArgumentException.class, () -> Dpp4Fun.from(passport));
    }

    @Test
    void addingFirstKnownListEntryInsertsBeforeUnknownExtensions() {
        MultiValuedDataElement sourceList = new MultiValuedDataElement.Builder().elementId("tags")
                .dataElements(List.of(new SingleValuedDataElement.Builder()
                        .elementId("futureTagExtension").value("x").build())).build();
        DataElementCollection source = new DataElementCollection.Builder().elementId("classification")
                .dataElements(List.of(sourceList)).build();

        DataElementCollection edited = ProfileElementSupport.patchStringList(source, "tags", List.of("new"),
                List.of("sector", "group", "class", "category", "tagSet"));

        assertEquals(List.of("item-0", "futureTagExtension"),
                ((MultiValuedDataElement) ProfileElementSupport.find(edited, "tags")).getDataElements()
                        .stream().map(DataElement::getElementId).toList());
    }

    @Test
    void reservedProfileElementWithWrongSubtypeIsRejected() {
        DigitalProductPassport source = TestDataFactory.validDpp().asDigitalProductPassport();
        List<DataElement> elements = new java.util.ArrayList<>(source.getDataElements());
        for (int index = 0; index < elements.size(); index++) {
            if ("characteristics".equals(elements.get(index).getElementId())) {
                elements.set(index, new SingleValuedDataElement.Builder()
                        .elementId("characteristics").value("wrong shape").build());
            }
        }
        elements.add(new SingleValuedDataElement.Builder().elementId("characteristics").value("wrong shape").build());
        DigitalProductPassport malformed = source.toBuilder().dataElements(elements).build();
        assertThrows(IllegalArgumentException.class, () -> Dpp4Fun.from(malformed));
    }

    private static List<DataElement> replaceTop(List<DataElement> source, String id, DataElement replacement) {
        java.util.ArrayList<DataElement> updated = new java.util.ArrayList<>(source);
        for (int i = 0; i < updated.size(); i++) {
            if (id.equals(updated.get(i).getElementId())) { updated.set(i, replacement); return updated; }
        }
        throw new IllegalArgumentException("Missing element " + id);
    }

    private static SingleValuedDataElement scalar(String id, String value) {
        return new SingleValuedDataElement.Builder().elementId(id).value(value).build();
    }
}
