package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Nameplate.Builder — local integrity only.
 */
class NameplateBuilderTest {

    @Test
    void build_withGtinCode_succeeds() {
        Nameplate n = Nameplate.builder().gtinCode("GTIN-001").build();
        assertEquals("GTIN-001", n.getGtinCode());
    }

    @Test
    void build_missingGtinCodeIsRepresentableAsLegacyExtensionData() {
        Nameplate nameplate = Nameplate.builder().internalArticleNumber("ART-001").build();
        var dpp = dppsdk.support.TestDataFactory.validDpp().toBuilder().nameplate(nameplate).build();
        assertTrue(new dppsdk.dpp4fun.validation.Dpp4FunValidator().validate(dpp).isValid());
    }

    @Test
    void build_blankGtinCodeHasNoUnprovenProfileRule() {
        Nameplate nameplate = Nameplate.builder().gtinCode("   ").build();
        var dpp = dppsdk.support.TestDataFactory.validDpp().toBuilder().nameplate(nameplate).build();
        assertTrue(new dppsdk.dpp4fun.validation.Dpp4FunValidator().validate(dpp).isValid());
    }

    @Test
    void build_withD4ManufacturerName_succeeds() {
        Nameplate n = Nameplate.builder().gtinCode("GTIN-001").manufacturerName("Manufacturer Co").build();
        assertEquals("Manufacturer Co", n.getManufacturerName());
    }
}
