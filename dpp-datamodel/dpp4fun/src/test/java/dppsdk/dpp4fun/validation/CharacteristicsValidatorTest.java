package dppsdk.dpp4fun.validation;

import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.dpp4fun.model.unit.MassUnit;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacteristicsValidatorTest {
    @Test
    void negativeFiniteWeightHasNoUnprovenProfileRule() {
        var chair = ChairCharacteristics.builder().weight(-1.0, MassUnit.KILOGRAM).build();
        var value = TestDataFactory.validDpp().toBuilder().chairCharacteristics(chair).build();
        assertTrue(new Dpp4FunValidator().validate(value).isValid());
    }

    @Test
    void repeatedSourceAspectsRemainOptional() {
        assertTrue(new Dpp4FunValidator().validate(TestDataFactory.validDpp()).isValid());
    }
}
