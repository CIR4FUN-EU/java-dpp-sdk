package dppsdk.dpp4fun.validation;

import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.dpp4fun.model.unit.LengthUnit;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Dpp4FunCharacteristicNonRuleTest {
    @Test
    void partialDimensionsAreValidWhenTheSourceUnitIsPresent() {
        var chair = ChairCharacteristics.builder().length(12.0, LengthUnit.CENTIMETER).build();
        var value = TestDataFactory.validDpp().toBuilder().chairCharacteristics(chair).build();
        assertTrue(new Dpp4FunValidator().validate(value).isValid());
    }

    @Test
    void negativeDimensionHasNoUnprovenProfileRule() {
        var chair = ChairCharacteristics.builder().length(-12.0, LengthUnit.CENTIMETER).build();
        var value = TestDataFactory.validDpp().toBuilder().chairCharacteristics(chair).build();
        assertTrue(new Dpp4FunValidator().validate(value).isValid());
    }
}
