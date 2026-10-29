package dppsdk.dpp4fun.model;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.validation.DppValidator;
import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.dpp4fun.model.characteristics.CribCharacteristics;
import dppsdk.dpp4fun.model.characteristics.FabricCharacteristics;
import dppsdk.dpp4fun.model.characteristics.LockerCharacteristics;
import dppsdk.dpp4fun.model.characteristics.ParticleBoardCharacteristics;
import dppsdk.dpp4fun.model.characteristics.SofaCharacteristics;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import dppsdk.dpp4fun.validation.Dpp4FunValidator;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Dpp4FunComprehensiveAcceptanceTest {
    private final Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();

    @Test
    void comprehensivePublicTypedFixtureValidatesAndRoundTripsExpandedAndCompressed() {
        Dpp4Fun fixture = TestDataFactory.comprehensiveDpp4Fun();
        assertEquals(19, List.of(fixture.getBillOfMaterials(), fixture.getCarbonFootprint(), fixture.getDurability(),
                fixture.getEnvironmentalImpact(), fixture.getHealthImpact(), fixture.getNameplate(),
                fixture.getProductRelationship(), fixture.getRecyclability(), fixture.getRefurbishment(),
                fixture.getReliability(), fixture.getRemanufacturing(), fixture.getRepairability(),
                fixture.getReusability(), fixture.getRightOfEndUsers(), fixture.getSafety(),
                fixture.getTraceability(), fixture.getUpgradability(), fixture.getUsage(),
                fixture.getWasteManagement()).stream().filter(java.util.Objects::nonNull).count());
        assertEquals(ChairCharacteristics.class, fixture.getSelectedCharacteristics().getClass());
        assertTrue(new DppValidator().validate(fixture.asDigitalProductPassport()).isValid());
        assertTrue(new Dpp4FunValidator().validate(fixture).isValid(),
                new Dpp4FunValidator().validate(fixture).issues().toString());
        assertTrue(new Dpp4FunValidationService().validate(fixture).isValid(),
                new Dpp4FunValidationService().validate(fixture).issues().toString());

        String expanded = codec.toExpandedJson(fixture);
        Dpp4Fun expandedRoundTrip = codec.fromExpandedJson(expanded);
        assertEquals(fixture, expandedRoundTrip);

        String compressed = codec.toCompressedJson(fixture);
        Dpp4Fun compressedRoundTrip = codec.fromCompressedJson(compressed);
        assertEquals(fixture, compressedRoundTrip);
        assertEquals(fixture.getHeader(), compressedRoundTrip.getHeader());
        assertEquals(fixture.getNameplate().getManufacturerProductDescription(),
                compressedRoundTrip.getNameplate().getManufacturerProductDescription());
        assertEquals(fixture.getChairCharacteristics(), compressedRoundTrip.getChairCharacteristics());
        assertEquals(fixture.getBillOfMaterials().getMaterials(), compressedRoundTrip.getBillOfMaterials().getMaterials());
        assertEquals("cm", compressedRoundTrip.getChairCharacteristics().getLength().orElseThrow().unit().symbol());
    }

    @Test
    void canonicalExpansionPreservesUnknownCompatibleExtensionThroughTypedEdit() {
        Dpp4Fun fixture = TestDataFactory.comprehensiveDpp4Fun();
        List<DataElement> elements = new ArrayList<>(fixture.asDigitalProductPassport().getDataElements());
        elements.add(new SingleValuedDataElement.Builder().elementId("vendorExtension")
                .dictionaryReference("https://vendor.example.test/terms#extension")
                .valueDataType("xsd:string").value("preserve-me").build());
        Dpp4Fun extended = Dpp4Fun.from(fixture.asDigitalProductPassport().toBuilder().dataElements(elements).build());

        assertEquals(extended, codec.fromExpandedJson(codec.toExpandedJson(extended)));
        var compressed = codec.expandCompressedJson(codec.toCompressedJson(extended),
                extended.asDigitalProductPassport());
        Dpp4Fun edited = Dpp4Fun.from(compressed.passport()).toBuilder()
                .chairCharacteristics(Dpp4Fun.from(compressed.passport()).getChairCharacteristics().toBuilder()
                        .color("green").build()).build();
        SingleValuedDataElement extension = (SingleValuedDataElement) edited.asDigitalProductPassport()
                .getDataElements().stream().filter(element -> "vendorExtension".equals(element.getElementId()))
                .findFirst().orElseThrow();
        assertEquals("preserve-me", extension.getValue());
        assertEquals("https://vendor.example.test/terms#extension", extension.getDictionaryReference());
    }

    @Test
    void allSixCharacteristicsVariantsHaveTypedSmokeRoundTrips() {
        List<Dpp4Fun> fixtures = List.of(
                Dpp4Fun.builder(TestDataFactory.validHeader()).chairCharacteristics(ChairCharacteristics.builder().color("blue").build()).build(),
                Dpp4Fun.builder(TestDataFactory.validHeader()).cribCharacteristics(CribCharacteristics.builder().mattressIncluded(true).build()).build(),
                Dpp4Fun.builder(TestDataFactory.validHeader()).fabricCharacteristics(FabricCharacteristics.builder().flameRetardant(true).colorFastness(5).build()).build(),
                Dpp4Fun.builder(TestDataFactory.validHeader()).lockerCharacteristics(LockerCharacteristics.builder().lockIncluded(true).build()).build(),
                Dpp4Fun.builder(TestDataFactory.validHeader()).particleBoardCharacteristics(ParticleBoardCharacteristics.builder().carb2Epa(true).build()).build(),
                Dpp4Fun.builder(TestDataFactory.validHeader()).sofaCharacteristics(SofaCharacteristics.builder().headrestIncluded(true).build()).build());

        assertEquals(Dpp4FunProductVariant.values().length, fixtures.size());
        for (Dpp4Fun fixture : fixtures) {
            assertTrue(new Dpp4FunValidationService().validate(fixture).isValid());
            assertEquals(fixture, codec.fromExpandedJson(codec.toExpandedJson(fixture)));
            assertEquals(fixture, codec.fromCompressedJson(codec.toCompressedJson(fixture)));
        }
    }
}
