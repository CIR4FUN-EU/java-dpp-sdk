package dppsdk.dpp4fun.validation;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.dpp4fun.model.aspect.BillOfMaterials;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.composite.Material;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BillOfMaterialsValidatorTest {
    @Test void duplicateItemsArePreservedAndNotRejectedByGuessedIdentityRules() {
        Material first = new Material.Builder().name("Steel").reference(java.net.URI.create("urn:material:same")).build();
        Material second = new Material.Builder().name("Steel").reference(java.net.URI.create("urn:material:same")).build();
        BillOfMaterials bom = BillOfMaterials.builder().addMaterial(first).addMaterial(second).build();
        assertEquals(2, bom.getMaterials().size());
        assertTrue(new Dpp4FunValidator().validate(TestDataFactory.validDpp().toBuilder().billOfMaterials(bom).build()).isValid());
    }

    @Test void legacyPortionValuesDoNotAcquireUnprovenProfileRules() {
        Material invalid = new Material.Builder().name("Steel").mandatory(true).portion(-1.0).build();
        var result = new Dpp4FunValidator().validate(TestDataFactory.validDpp().toBuilder()
                .billOfMaterials(BillOfMaterials.builder().addMaterial(invalid).build()).build());
        assertTrue(result.isValid());
        Material zero = new Material.Builder().name("Steel").mandatory(true).portion(0.0).build();
        var zeroResult = new Dpp4FunValidator().validate(TestDataFactory.validDpp().toBuilder()
                .billOfMaterials(BillOfMaterials.builder().addMaterial(zero).build()).build());
        assertTrue(zeroResult.isValid());
    }
}
