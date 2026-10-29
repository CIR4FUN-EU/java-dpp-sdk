package dppsdk.dpp4fun.mapper;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonRoundTripTest {
    @Test void domainCoreJsonCoreDomainRoundTripKeepsTheCanonicalPassport() {
        Dpp4Fun original = TestDataFactory.validDpp();
        Dpp4Fun restored = new Dpp4FunJsonCodec().fromJson(new Dpp4FunJsonCodec().toJson(original));
        DigitalProductPassport before = original.asDigitalProductPassport();
        assertEquals(before, restored.asDigitalProductPassport());
        assertEquals(original.getNameplate().getGtinCode(), restored.getNameplate().getGtinCode());
        assertEquals(original.getChairCharacteristics(), restored.getChairCharacteristics());
    }
}
