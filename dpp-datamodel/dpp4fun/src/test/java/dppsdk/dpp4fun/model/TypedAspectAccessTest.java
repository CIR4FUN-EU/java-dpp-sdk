package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypedAspectAccessTest {
    @Test
    void editsImmutablyAndPreservesUnknownChildren() {
        var original = new DataElementCollection.Builder().elementId("nameplate")
                .dataElements(List.of(
                        new SingleValuedDataElement.Builder().elementId("brand").value("Old").build(),
                        new SingleValuedDataElement.Builder().elementId("extension").value("keep").build()))
                .build();
        TypedAspectAccess source = new TypedAspectAccess(new ProfiledAspectState("nameplate", original));

        TypedAspectAccess edited = source.withString("brand", "New");

        assertEquals("Old", source.string("brand"));
        assertEquals("New", edited.string("brand"));
        assertEquals("keep", ((SingleValuedDataElement) edited.dataElement("extension")).getValue());
    }

    @Test
    void removesAnOptionalFieldWhenBuilderReceivesNull() {
        ProfiledAspectState original = ProfiledAspectState.builder(KnownProfileAspect.NAMEPLATE)
                .field("brand", "Old").build();

        TypedAspectAccess edited = new TypedAspectAccess(original).withString("brand", null);

        assertNull(edited.optionalString("brand"));
    }

    @Test
    void restoresBuiltInUnitIdentityFromCanonicalStringSlot() {
        ProfiledAspectState original = ProfiledAspectState.builder(Dpp4FunProductVariant.CHAIR)
                .quantity("length", new Dpp4FunQuantity<>(125, LengthUnit.CENTIMETER)).build();

        var quantity = new TypedAspectAccess(original).quantity("length").orElseThrow();

        assertEquals(125, quantity.value());
        assertEquals(LengthUnit.CENTIMETER, quantity.unit());
    }

    @Test
    void rejectsUnitsWhoseSemanticIdentityCannotSurviveCanonicalStorage() {
        CustomSemanticUnit unit = new CustomSemanticUnit("yd",
                java.net.URI.create("https://example.org/unit/yard"), UnitDimension.LENGTH);

        assertThrows(IllegalArgumentException.class, () -> ProfiledAspectState.builder(Dpp4FunProductVariant.CHAIR)
                .quantity("length", new Dpp4FunQuantity<>(125, unit)));
    }
}
