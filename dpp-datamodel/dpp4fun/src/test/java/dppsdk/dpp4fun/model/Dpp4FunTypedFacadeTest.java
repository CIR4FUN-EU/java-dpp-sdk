package dppsdk.dpp4fun.model;

import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.dpp4fun.model.aspect.Nameplate;
import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import dppsdk.dpp4fun.model.unit.LengthUnit;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Dpp4FunTypedFacadeTest {
    @Test
    void publicTypedBuildersSelectTheVariantAndCanonicalContentSpecification() {
        Dpp4Fun value = Dpp4Fun.builder(new DppHeader.Builder().build())
                .chairCharacteristics(ChairCharacteristics.builder().color("blue").build()).build();

        assertNotNull(value.getChairCharacteristics());
        assertNull(value.getSofaCharacteristics());
        assertEquals(Dpp4FunSemanticProfile.loadDefault().contentSpecificationIds(Dpp4FunProductVariant.CHAIR),
                value.getHeader().getContentSpecificationIds());
    }

    @Test
    void namedAspectGetterReturnsTheTypedNameplate() {
        Dpp4Fun value = Dpp4Fun.builder(dppsdk.support.TestDataFactory.validHeader())
                .nameplate(Nameplate.builder().gtinCode("GTIN-1").build()).build();

        assertEquals("GTIN-1", value.getNameplate().getGtinCode());
        assertTrue(new dppsdk.dpp4fun.validation.Dpp4FunValidator().validate(value).isValid());
    }

    @Test
    void typedCharacteristicsEditRetainsUntouchedValuesAndUnknownElements() {
        Dpp4Fun original = Dpp4Fun.builder(dppsdk.support.TestDataFactory.validHeader())
                .chairCharacteristics(ChairCharacteristics.builder().color("blue")
                        .length(2.5, LengthUnit.CENTIMETER).build()).build();
        var source = (DataElementCollection) original.asDigitalProductPassport().getDataElements().get(0);
        var extended = source.toBuilder().dataElements(java.util.stream.Stream.concat(
                source.getDataElements().stream(), java.util.stream.Stream.of(
                        new SingleValuedDataElement.Builder().elementId("vendorExtension").value("keep").build()))
                .toList()).build();
        Dpp4Fun withExtension = Dpp4Fun.from(original.asDigitalProductPassport().toBuilder()
                .dataElements(List.of(extended)).build());

        Dpp4Fun edited = withExtension.toBuilder().chairCharacteristics(
                withExtension.getChairCharacteristics().toBuilder().color("green").build()).build();

        assertEquals("green", edited.getChairCharacteristics().getColor());
        assertEquals(2.5, edited.getChairCharacteristics().getLengthValue());
        assertEquals("keep", ((SingleValuedDataElement) ((DataElementCollection) edited.getChairCharacteristics().toDataElement())
                .getDataElements().stream().filter(element -> element.getElementId().equals("vendorExtension"))
                .findFirst().orElseThrow()).getValue());
    }
}
