package dppsdk.postgres.dpp4fun;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Dpp4FunSearchCriteriaTest {

    @Test
    void acceptsNullLimitAndOffsetDefaults() {
        Dpp4FunSearchCriteria criteria = new Dpp4FunSearchCriteria(null, null, null, null, null, null, null, null, null);
        assertEquals(50, criteria.limitOrDefault());
        assertEquals(0, criteria.offsetOrDefault());
    }

    @Test
    void acceptsPositiveLimitAndNonNegativeOffset() {
        assertDoesNotThrow(() -> new Dpp4FunSearchCriteria(null, null, null, null, null, null, null, 10, 0));
    }

    @Test
    void rejectsNonPositiveLimit() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Dpp4FunSearchCriteria(null, null, null, null, null, null, null, 0, 0));
        assertEquals("limit must be greater than zero", exception.getMessage());
    }

    @Test
    void rejectsNegativeOffset() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Dpp4FunSearchCriteria(null, null, null, null, null, null, null, 10, -1));
        assertEquals("offset must be zero or greater", exception.getMessage());
    }
}
