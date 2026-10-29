package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Dpp4FunUnknownPreservationTest {
    @Test
    void editingCatalogBoundAspectKeepsUnknownChildrenAndUntouchedFields() {
        Dpp4Fun original = Dpp4Fun.builder(TestDataFactory.validHeader())
                .chairCharacteristics(ChairCharacteristics.builder().color("blue")
                        .length(2.5, dppsdk.dpp4fun.model.unit.LengthUnit.METER).build())
                .build();
        DataElementCollection source = (DataElementCollection) original.getChairCharacteristics().toDataElement();
        DataElementCollection withExtension = source.toBuilder().dataElements(java.util.stream.Stream.concat(
                source.getDataElements().stream(), java.util.stream.Stream.of(
                        new SingleValuedDataElement.Builder().elementId("vendorExtension").value("keep").build()))
                .toList()).build();
        Dpp4Fun wrapped = Dpp4Fun.from(original.asDigitalProductPassport().toBuilder()
                .dataElements(List.of(withExtension)).build());

        Dpp4Fun edited = wrapped.toBuilder().chairCharacteristics(wrapped.getChairCharacteristics()
                .toBuilder().color("green").build()).build();

        assertEquals("green", edited.getChairCharacteristics().getColor());
        assertEquals(2.5, edited.getChairCharacteristics().getLengthValue());
        DataElementCollection actual = (DataElementCollection) edited.asDigitalProductPassport().getDataElements().get(0);
        assertEquals("keep", ((SingleValuedDataElement) actual.getDataElements().stream()
                .filter(element -> element.getElementId().equals("vendorExtension")).findFirst().orElseThrow()).getValue());
    }

    @Test
    void coreRepresentableUnknownRootsSurviveTypedProfileEdits() {
        Dpp4Fun original = Dpp4Fun.builder(TestDataFactory.validHeader())
                .chairCharacteristics(ChairCharacteristics.builder().color("blue").build()).build();
        DataElement extension = new SingleValuedDataElement.Builder().elementId("futureRoot")
                .value("preserve").build();
        Dpp4Fun wrapped = Dpp4Fun.from(original.asDigitalProductPassport().toBuilder()
                .dataElements(List.of(original.asDigitalProductPassport().getDataElements().get(0), extension)).build());
        Dpp4Fun edited = wrapped.toBuilder().chairCharacteristics(
                wrapped.getChairCharacteristics().toBuilder().color("red").build()).build();

        assertEquals(List.of("characteristics", "futureRoot"), edited.asDigitalProductPassport().getDataElements()
                .stream().map(DataElement::getElementId).toList());
        assertEquals("preserve", ((SingleValuedDataElement) edited.asDigitalProductPassport().getDataElements().get(1)).getValue());
    }
}
