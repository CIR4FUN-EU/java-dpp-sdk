package dppsdk.dpp4fun.validation;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.aspect.Nameplate;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameplateValidatorTest {
    @Test void manufacturerNameIsOptionalProfileData() {
        Dpp4Fun value = TestDataFactory.validDpp().toBuilder().nameplate(
                Nameplate.builder().gtinCode("GTIN-DEMO-001").manufacturerName("Demo Manufacturer GmbH").build())
                .build();
        assertEquals("Demo Manufacturer GmbH", value.getNameplate().getManufacturerName());
        assertTrue(new Dpp4FunValidator().validate(value).isValid());
    }

    @Test void nameplateGtinRemainsProfileDataAndDoesNotReplaceHeaderIdentity() {
        Dpp4Fun value = TestDataFactory.validDpp();
        assertEquals("GTIN-DEMO-001", value.getNameplate().getGtinCode());
        assertEquals("urn:example:dpp:1", value.getDigitalProductPassportId());
        assertEquals("urn:example:product:1", value.getUniqueProductIdentifier());
        assertNotEquals(value.getDigitalProductPassportId(), value.getUniqueProductIdentifier());
        assertNotEquals(value.getNameplate().getGtinCode(), value.getUniqueProductIdentifier());
        assertNotEquals(value.getNameplate().getGtinCode(), value.getDigitalProductPassportId());
    }
}
