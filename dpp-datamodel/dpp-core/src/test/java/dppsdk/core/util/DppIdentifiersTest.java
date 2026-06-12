package dppsdk.core.util;

import dppsdk.core.model.Dpp;
import dppsdk.core.support.CoreTestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DppIdentifiersTest {

    @Test
    void utilityReturnsStandardApiIdentifiers() {
        Dpp dpp = CoreTestDataFactory.validDpp();

        assertEquals(dpp.getUniqueProductIdentifier().toString(), DppIdentifiers.dppId(dpp));
        assertEquals(dpp.getGtinCode(), DppIdentifiers.productId(dpp));
        assertEquals(dpp.getDppId(), DppIdentifiers.dppId(dpp));
        assertEquals(dpp.getProductId(), DppIdentifiers.productId(dpp));
    }

    @Test
    void nullDppThrowsClearException() {
        IllegalArgumentException dppIdException =
                assertThrows(IllegalArgumentException.class, () -> DppIdentifiers.dppId(null));
        IllegalArgumentException productIdException =
                assertThrows(IllegalArgumentException.class, () -> DppIdentifiers.productId(null));

        assertEquals("dpp must not be null", dppIdException.getMessage());
        assertEquals("dpp must not be null", productIdException.getMessage());
    }
}
