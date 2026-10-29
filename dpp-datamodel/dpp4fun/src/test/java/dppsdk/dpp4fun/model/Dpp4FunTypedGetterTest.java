package dppsdk.dpp4fun.model;

import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class Dpp4FunTypedGetterTest {
    @Test
    void selectedCharacteristicsAndAspectGettersReturnTypedViews() {
        Dpp4Fun dpp = TestDataFactory.validDpp();

        assertEquals("navy", dpp.getChairCharacteristics().getColor());
        assertEquals(dpp.getChairCharacteristics(), dpp.getSelectedCharacteristics());
        assertNull(dpp.getSofaCharacteristics());
        assertNotNull(dpp.getBillOfMaterials());
        assertNotNull(dpp.getNameplate());
    }

    @Test
    void characteristicsEditReturnsAnotherTypedVariant() {
        ChairCharacteristics original = TestDataFactory.validDpp().getChairCharacteristics();
        ChairCharacteristics edited = original.toBuilder().color("green").build();

        assertEquals("navy", original.getColor());
        assertEquals("green", edited.getColor());
    }

    @Test
    void typedProductAggregateHasNoLegacySuperclass() {
        assertEquals(Object.class, Dpp4Fun.class.getSuperclass());
    }
}
