package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import org.junit.jupiter.api.Test;
import dppsdk.dpp4fun.validation.Dpp4FunValidator;
import dppsdk.support.TestDataFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiniteNumberContractTest {

    @Test
    void chairWeightMustBeFinite() {
        assertDoesNotThrow(() -> ChairCharacteristics.builder().weight(0.0, MassUnit.KILOGRAM).build());
        assertDoesNotThrow(() -> ChairCharacteristics.builder().weight(1.25e2, MassUnit.KILOGRAM).build());
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ChairCharacteristics.builder().weight(invalid, MassUnit.KILOGRAM).build());
        }
    }

    @Test
    void characteristicMeasurementMustBeFinite() {
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ChairCharacteristics.builder().length(invalid, LengthUnit.CENTIMETER).build());
        }
    }

    @Test
    void materialPortionMustBeFinite() {
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Material.Builder().name("Steel").portion(invalid).build());
        }
    }

    @Test
    void negativeMeasurementHasNoUnprovenProfileRule() {
        var characteristics = ChairCharacteristics.builder().weight(-1.0, MassUnit.KILOGRAM).build();
        var value = TestDataFactory.validDpp().toBuilder().chairCharacteristics(characteristics).build();
        assertTrue(new Dpp4FunValidator().validate(value).isValid());
    }
}
