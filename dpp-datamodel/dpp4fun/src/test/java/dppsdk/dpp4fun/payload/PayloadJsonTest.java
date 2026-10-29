package dppsdk.dpp4fun.payload;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.transport.json.DppJsonCodec;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Replaces the old mutable-payload transport assertion with the frozen core wire contract. */
class PayloadJsonTest {
    @Test void convenienceCodecUsesExactlyTheCoreExpandedJsonRepresentation() {
        Dpp4Fun dpp = TestDataFactory.validDpp();
        assertEquals(new DppJsonCodec().toExpandedJson(dpp.asDigitalProductPassport()),
                new Dpp4FunJsonCodec().toJson(dpp));
    }
}
