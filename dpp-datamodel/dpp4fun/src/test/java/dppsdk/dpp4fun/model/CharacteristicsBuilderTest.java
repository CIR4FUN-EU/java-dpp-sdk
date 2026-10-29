package dppsdk.dpp4fun.model;

import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.dpp4fun.model.unit.LengthUnit;
import dppsdk.dpp4fun.model.unit.MassUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacteristicsBuilderTest {
    @Test
    void typedVariantBuilderKeepsOptionalSourceFieldsOptional() {
        ChairCharacteristics value = ChairCharacteristics.builder().color("navy").build();
        assertEquals("navy", value.getColor());
        assertTrue(value.getLength().isEmpty());
    }

    @Test
    void typedQuantitiesAndToBuilderPreserveIndependentImmutableValues() {
        ChairCharacteristics original = ChairCharacteristics.builder()
                .length(200.0, LengthUnit.CENTIMETER).weight(75.0, MassUnit.KILOGRAM).build();
        ChairCharacteristics updated = original.toBuilder().color("blue").build();

        assertEquals("cm", original.getLengthUnitSymbol());
        assertEquals("kg", original.getWeightUnitSymbol());
        assertTrue(original.getColor() == null);
        assertEquals("blue", updated.getColor());
        assertNotSame(original, updated);
        assertEquals(original.getLength(), updated.getLength());
    }
}
