package dppsdk.dpp4fun.validation;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.aspect.BillOfMaterials;
import dppsdk.dpp4fun.model.internal.ProfiledAspectState;
import dppsdk.dpp4fun.model.internal.KnownProfileAspect;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Dpp4FunValidatorTest {
    private final Dpp4FunValidator validator = new Dpp4FunValidator();

    @Test
    void profileDoesNotRequireOptionalAspects() {
        Dpp4Fun empty = Dpp4Fun.builder(TestDataFactory.validHeader()).build();
        assertTrue(validator.validate(empty).isValid());
    }

    @Test
    void sourceBackedGeneralAspectUsesCatalogBindings() {
        Dpp4Fun value = Dpp4Fun.builder(TestDataFactory.validHeader())
                .billOfMaterials(BillOfMaterials.builder().addMaterial(
                        Material.builder().name("Foam").mandatory(true).portion(0.0).build()).build()).build();
        assertTrue(validator.validate(value).isValid());
    }

    @Test
    void catalogBoundCharacteristicsPassValidation() {
        Dpp4Fun value = Dpp4Fun.builder(TestDataFactory.validHeader())
                .chairCharacteristics(ChairCharacteristics.builder().color("blue").build()).build();
        assertTrue(validator.validate(value).isValid());
    }

    @Test
    void nullFacadeIsReportedWithoutThrowing() {
        var result = validator.validate(null);
        assertFalse(result.isValid());
        assertEquals("DPP4FUN_REQUIRED", result.issues().get(0).rule());
    }
}
