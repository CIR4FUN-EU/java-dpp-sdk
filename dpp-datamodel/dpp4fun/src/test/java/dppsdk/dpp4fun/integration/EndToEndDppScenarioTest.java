package dppsdk.dpp4fun.integration;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.*;
import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.unit.LengthUnit;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndToEndDppScenarioTest {
    @Test void typedFacadeCoreJsonCoreFacadeKeepsFullPassportAndConvenientValues() {
        Dpp4Fun value = typedDpp();
        Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();
        Dpp4Fun roundTrip = codec.fromJson(codec.toJson(value));

        assertEquals(value.asDigitalProductPassport(), roundTrip.asDigitalProductPassport());
        assertTrue(new Dpp4FunValidationService().validate(roundTrip).isValid());
        assertEquals("GTIN-DEMO-001", roundTrip.getNameplate().getGtinCode());
        assertEquals("blue", roundTrip.getChairCharacteristics().getColor());
        assertEquals(200.0, roundTrip.getChairCharacteristics().getLengthValue());
        assertEquals("cm", roundTrip.getChairCharacteristics().getLengthUnitSymbol());
    }

    @Test void immutableAggregateEditRetainsHeaderAndOriginalCorePassport() {
        Dpp4Fun original = typedDpp();
        Dpp4Fun edited = original.toBuilder().characteristics(original.getChairCharacteristics().toBuilder()
                .color("green").build()).build();
        assertEquals("blue", original.getChairCharacteristics().getColor());
        assertEquals("green", edited.getChairCharacteristics().getColor());
        assertEquals(original.getHeader(), edited.getHeader());
        assertNotSame(original.asDigitalProductPassport(), edited.asDigitalProductPassport());
    }

    private static Dpp4Fun typedDpp() {
        return Dpp4Fun.builder(TestDataFactory.validHeader())
                .nameplate(TestDataFactory.validNameplate())
                .billOfMaterials(TestDataFactory.validBillOfMaterials())
                .chairCharacteristics(ChairCharacteristics.builder().color("blue")
                        .length(200.0, LengthUnit.CENTIMETER).build())
                .build();
    }
}
