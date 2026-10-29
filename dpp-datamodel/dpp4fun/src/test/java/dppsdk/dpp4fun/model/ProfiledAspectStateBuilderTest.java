package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

class ProfiledAspectStateBuilderTest {
    @Test
    void buildsACharacteristicsVariantFromCatalogSemantics() {
        ProfiledAspectState aspect = ProfiledAspectState.builder(Dpp4FunProductVariant.CHAIR)
                .field("color", "blue")
                .field("length", 2.5)
                .build();

        assertEquals("characteristics", aspect.asDataElement().getElementId());
        var color = (SingleValuedDataElement) aspect.asDataElement().getDataElements().stream()
                .filter(element -> element.getElementId().equals("color")).findFirst().orElseThrow();
        assertEquals("https://www.cir4fun.eu/ontology/dpp4fun#Color", color.getDictionaryReference());
        assertEquals("xsd:string", color.getValueDataType());
        assertEquals(2.5, aspect.number("length").doubleValue());
    }

    @Test
    void createsCatalogRepeatedItemIdsAndOmitsEmptyLists() {
        ProfiledAspectState aspect = ProfiledAspectState.builder(KnownProfileAspect.BILL_OF_MATERIALS)
                .field("materialSet[0].name", "Foam")
                .field("materialSet[0].mandatory", true)
                .build();

        MultiValuedDataElement materialSet = (MultiValuedDataElement) aspect.asDataElement().getDataElements().stream()
                .filter(element -> element.getElementId().equals("materialSet")).findFirst().orElseThrow();
        DataElementCollection material = (DataElementCollection) materialSet.getDataElements().get(0);
        assertEquals("material_00", material.getElementId());
        assertEquals(2, material.getDataElements().size());
        assertTrue(ProfiledAspectState.builder(KnownProfileAspect.BILL_OF_MATERIALS).build()
                .asDataElement().getDataElements().isEmpty());
    }

    @Test
    void createsCanonicalIdsForRepeatedScalarItems() {
        ProfiledAspectState aspect = ProfiledAspectState.builder(KnownProfileAspect.RIGHT_OF_END_USERS)
                .field("policies.permissionSet[0]", "share")
                .build();

        DataElementCollection policies = (DataElementCollection) aspect.asDataElement().getDataElements().get(0);
        MultiValuedDataElement permissions = (MultiValuedDataElement) policies.getDataElements().get(0);
        assertEquals("permission_00", permissions.getDataElements().get(0).getElementId());
    }

    @Test
    void rejectsGappedRepeatedIndexesAndExposesAllAspectRoots() {
        assertThrows(IllegalArgumentException.class, () -> ProfiledAspectState.builder(KnownProfileAspect.BILL_OF_MATERIALS)
                .field("materialSet[1].name", "Foam").build());
        for (KnownProfileAspect type : KnownProfileAspect.values()) {
            assertEquals(type.elementId(), ProfiledAspectState.builder(type).build().elementId());
        }
        for (Dpp4FunProductVariant variant : Dpp4FunProductVariant.values()) {
            assertEquals("characteristics", ProfiledAspectState.builder(variant).build().elementId());
        }
    }

    @Test
    void rejectsUnknownFieldsAndValuesWithTheWrongCatalogDatatype() {
        assertThrows(IllegalArgumentException.class, () -> ProfiledAspectState.builder(KnownProfileAspect.NAMEPLATE)
                .field("notANameplateField", "x"));
        assertThrows(IllegalArgumentException.class, () -> ProfiledAspectState.builder(Dpp4FunProductVariant.CHAIR)
                .field("indoorUse", "yes"));
    }

    @Test
    void bindsMultiLanguagePropertiesUsingTheirCanonicalKind() {
        ProfiledAspectState aspect = ProfiledAspectState.builder(KnownProfileAspect.NAMEPLATE)
                .languages("productType", Map.of("en", "Chair", "de", "Stuhl"))
                .build();
        MultiLanguageDataElement productType = (MultiLanguageDataElement) aspect.asDataElement().getDataElements()
                .stream().filter(element -> element.getElementId().equals("productType")).findFirst().orElseThrow();
        assertEquals(2, productType.getValues().size());
    }

    @Test
    void bindsEveryFrozenMeasurementToItsRecordedUnitSlotAcrossAllVariants() {
        String[][] mappings = {{"length", "lengthUnit"}, {"width", "widthUnit"},
                {"height", "heightUnit"}, {"diameter", "diameterUnit"},
                {"weight", "weightUnit"}, {"volume", "volumeUnit"}};
        SemanticUnit[] units = {LengthUnit.MILLIMETER, LengthUnit.CENTIMETER, LengthUnit.METER,
                LengthUnit.MILLIMETER, MassUnit.KILOGRAM, VolumeUnit.CUBIC_METER};
        for (Dpp4FunProductVariant variant : Dpp4FunProductVariant.values()) {
            ProfiledAspectState.Builder builder = ProfiledAspectState.builder(variant);
            for (int index = 0; index < mappings.length; index++) {
                builder.quantity(mappings[index][0], new Dpp4FunQuantity<>(index + 0.25, units[index]));
            }
            ProfiledAspectState aspect = builder.build();
            for (int index = 0; index < mappings.length; index++) {
                assertEquals(index + 0.25, aspect.number(mappings[index][0]).doubleValue());
                assertEquals(units[index].symbol(), aspect.text(mappings[index][1]));
            }
        }
    }

    @Test
    void rejectsAQuantityFromTheWrongSemanticDimension() {
        assertThrows(IllegalArgumentException.class, () -> ProfiledAspectState.builder(Dpp4FunProductVariant.CHAIR)
                .quantity("weight", new Dpp4FunQuantity<>(12.5, LengthUnit.CENTIMETER)));
    }
}
