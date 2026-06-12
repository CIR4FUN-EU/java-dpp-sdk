package demo.producer.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dppsdk.core.validation.ValidationException;
import dppsdk.dpp4fun.model.Dpp4Fun;
import org.junit.jupiter.api.Test;

class Dpp4FunAdaptersTest {

    private final DemoDppFactory factory = new DemoDppFactory();

    @Test
    void codecRoundTripPreservesIdentity() {
        Dpp4Fun original = factory.createValidBedDpp();
        Dpp4FunDppCodecAdapter codec = new Dpp4FunDppCodecAdapter();

        Dpp4Fun parsed = codec.fromJson(codec.toJson(original));

        assertEquals(DemoDppFactory.idOf(original), DemoDppFactory.idOf(parsed));
    }

    @Test
    void validatorAcceptsValidDpp() {
        new Dpp4FunDppValidatorAdapter().validate(factory.createValidBedDpp());
    }

    @Test
    void validatorRejectsInvalidDpp() {
        Dpp4FunDppValidatorAdapter validator = new Dpp4FunDppValidatorAdapter();

        assertThrows(ValidationException.class, () -> validator.validate(factory.createDppWithWrongSupplierRole()));
    }
}
