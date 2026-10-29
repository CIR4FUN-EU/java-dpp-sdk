package dppsdk.dpp4fun.model;

import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.dpp4fun.model.unit.LengthUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChairCharacteristicsBuilderTest {
    @Test
    void supportsOptionalSourceMeasurementsAndTheirUnits() {
        ChairCharacteristics chair = ChairCharacteristics.builder()
                .length(200.0, LengthUnit.CENTIMETER).height(90.0, LengthUnit.CENTIMETER).build();

        assertEquals(200.0, chair.getLengthValue());
        assertEquals("cm", chair.getLengthUnitSymbol());
        assertEquals(90.0, chair.getHeightValue());
        assertNull(chair.getColor());
    }

    @Test
    void toBuilderChangesOneFieldAndKeepsTheSourceMeasurement() {
        ChairCharacteristics original = ChairCharacteristics.builder()
                .length(200.0, LengthUnit.CENTIMETER).color("navy").build();
        ChairCharacteristics edited = original.toBuilder().color("blue").build();

        assertEquals("navy", original.getColor());
        assertEquals("blue", edited.getColor());
        assertEquals(original.getLength(), edited.getLength());
    }
}
