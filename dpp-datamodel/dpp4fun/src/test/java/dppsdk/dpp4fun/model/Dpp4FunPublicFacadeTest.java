package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.Granularity;
import dppsdk.core.model.SingleValuedDataElement;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Dpp4FunPublicFacadeTest {
    @Test
    void createsAndImmutablyEditsThroughNormalTypedFacade() {
        Dpp4Fun original = Dpp4Fun.builder()
                .digitalProductPassportId("urn:uuid:passport")
                .uniqueProductIdentifier("urn:example:product:1")
                .granularity(Granularity.ITEM)
                .economicOperatorId("operator")
                .dppStatus("draft")
                .lastUpdated(Instant.parse("2026-01-02T03:04:05Z"))
                .nameplate(Nameplate.builder().uriOfTheProduct(URI.create("https://example.test/item")).build())
                .build();

        Dpp4Fun edited = original.toBuilder()
                .nameplate(original.getNameplate().toBuilder().brand("Example").build())
                .build();

        assertEquals("Example", edited.getNameplate().getBrand());
        assertNull(original.getNameplate().getBrand());
        assertNotSame(original.asDigitalProductPassport(), edited.asDigitalProductPassport());
        assertEquals("urn:example:product:1", edited.getUniqueProductIdentifier());
    }

    @Test
    void createsTypedRepeatedRecordsAndCopiesOnlyTheEditedAspect() {
        Certificate certificate = Certificate.builder()
                .name("EN test")
                .issueDate("2026-01-01T00:00:00Z")
                .reference(URI.create("https://example.test/cert"))
                .build();
        Dpp4Fun original = Dpp4Fun.builder()
                .digitalProductPassportId("urn:uuid:passport")
                .uniqueProductIdentifier("urn:example:product:1")
                .granularity(Granularity.ITEM)
                .economicOperatorId("operator")
                .dppStatus("active")
                .durability(Durability.builder().expectedLifetime("10 years")
                        .certificates(List.of(certificate)).build())
                .build();

        DataElementCollection durability = (DataElementCollection) original.asDigitalProductPassport().getDataElements().get(0);
        DataElement extension = new SingleValuedDataElement.Builder().elementId("vendorExtension")
                .value("preserve").build();
        DataElementCollection withExtension = durability.toBuilder().dataElements(
                java.util.stream.Stream.concat(durability.getDataElements().stream(), java.util.stream.Stream.of(extension)).toList())
                .build();
        Dpp4Fun extended = Dpp4Fun.from(original.asDigitalProductPassport().toBuilder()
                .dataElements(List.of(withExtension)).build());
        Dpp4Fun edited = extended.toBuilder()
                .durability(extended.getDurability().toBuilder().cycleLife("10000 cycles").build())
                .build();

        assertEquals("EN test", edited.getDurability().getCertificates().get(0).getName());
        assertEquals("10000 cycles", edited.getDurability().getCycleLife());
        assertNull(extended.getDurability().getCycleLife());
        assertEquals("10 years", extended.getDurability().getExpectedLifetime());
        assertEquals("preserve", ((SingleValuedDataElement) ((DataElementCollection) edited.getDurability().toDataElement())
                .getDataElements().stream().filter(value -> "vendorExtension".equals(value.getElementId()))
                .findFirst().orElseThrow()).getValue());
        assertEquals(1, edited.asDigitalProductPassport().getDataElements().size());
    }

    @Test
    void selectsOneCharacteristicsVariantAndUsesSharedTypedUnitSlots() {
        ChairCharacteristics chair = ChairCharacteristics.builder()
                .height(80, LengthUnit.CENTIMETER)
                .seatHeight(45, LengthUnit.CENTIMETER)
                .length(100, LengthUnit.CENTIMETER)
                .surface("Smooth")
                .build();
        Dpp4Fun dpp = Dpp4Fun.builder()
                .digitalProductPassportId("urn:uuid:passport")
                .uniqueProductIdentifier("urn:example:product:1")
                .granularity(Granularity.ITEM)
                .economicOperatorId("operator")
                .dppStatus("active")
                .characteristics(chair)
                .build();

        assertEquals(ChairCharacteristics.class, dpp.getSelectedCharacteristics().getClass());
        assertEquals(LengthUnit.CENTIMETER, dpp.getChairCharacteristics().getHeight().orElseThrow().unit());
        assertEquals("Smooth", dpp.getChairCharacteristics().getSurface());
        IllegalArgumentException conflict = assertThrows(IllegalArgumentException.class, () -> ChairCharacteristics.builder()
                .height(80, LengthUnit.CENTIMETER).seatHeight(45, LengthUnit.METER).build());
        org.junit.jupiter.api.Assertions.assertTrue(conflict.getMessage().startsWith("DPP4FUN_UNIT_SLOT_CONFLICT"));
    }

    @Test
    void buildsEveryFrozenCharacteristicsVariantWithSourceNamesAndTypes() {
        assertEquals(Boolean.TRUE, CribCharacteristics.builder().mattressIncluded(true).build().getMattressIncluded());
        FabricCharacteristics fabric = FabricCharacteristics.builder().flameRetardant(true).colorFastness(5).build();
        assertEquals(Boolean.TRUE, fabric.getFlameRetardant());
        assertEquals(Integer.valueOf(5), fabric.getColorFastness());
        assertEquals(Boolean.TRUE, LockerCharacteristics.builder().lockIncluded(true).build().getLockIncluded());
        assertEquals(Boolean.TRUE, ParticleBoardCharacteristics.builder().carb2Epa(true).build().getCarb2Epa());
        assertEquals(Boolean.TRUE, SofaCharacteristics.builder().headrestIncluded(true).build().getHeadrestIncluded());
    }

    @Test
    void keepsUnspecifiedCompositionScalarsAbsent() {
        assertNull(Material.builder().build().getMandatory());
        assertNull(Material.builder().build().getPortion());
        assertNull(Component.builder().build().getMandatory());
        assertNull(Part.builder().build().getMandatory());
    }

    @Test
    void exposesNamedFunctionPolicyAndInstructionComposites() {
        Usage usage = Usage.builder()
                .primaryFunction(ProductFunction.builder().name("seating").status("primary").build())
                .secondaryFunctions(List.of(ProductFunction.builder().name("storage").build()))
                .assemblyInstructions(List.of(Instruction.builder().name("assemble")
                        .targetAudience("professional").build()))
                .build();
        RightOfEndUsers rights = RightOfEndUsers.builder()
                .postSaleServices(List.of(PostSaleService.builder().name("repair service").build()))
                .policies(Policies.builder().permissions(List.of("repair"))
                        .prohibitions(List.of("resell as new")).build())
                .build();

        assertEquals("seating", usage.getPrimaryFunction().getName());
        assertEquals("storage", usage.getSecondaryFunctions().get(0).getName());
        assertEquals("professional", usage.getAssemblyInstructions().get(0).getTargetAudience());
        assertEquals(List.of("repair"), rights.getPolicies().getPermissions());
        assertEquals("repair service", rights.getPostSaleServices().get(0).getName());
        Policies revisedPolicies = rights.getPolicies().toBuilder().permissions(List.of("reuse")).build();
        assertEquals(List.of("reuse"), revisedPolicies.getPermissions());
        assertEquals(List.of("repair"), rights.getPolicies().getPermissions());
    }
}
