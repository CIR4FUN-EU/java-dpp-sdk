package dppsdk.postgres.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DppPageRequestTest {

    @Test
    void acceptsBlankOrNumericCursor() {
        assertDoesNotThrow(() -> new DppPageRequest(null, 1));
        DppPageRequest blankCursorRequest = new DppPageRequest("  ", 1);
        assertEquals(null, blankCursorRequest.cursor());
        DppPageRequest pageRequest = new DppPageRequest("2", 1);
        assertEquals("2", pageRequest.cursor());
    }

    @Test
    void rejectsNonPositiveLimit() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new DppPageRequest(null, 0));
        assertEquals("limit must be greater than zero", exception.getMessage());
    }

    @Test
    void rejectsMalformedCursor() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new DppPageRequest("next-page", 1));
        assertEquals("cursor must be a non-negative integer", exception.getMessage());
    }

    @Test
    void rejectsNegativeCursor() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new DppPageRequest("-1", 1));
        assertEquals("cursor must be a non-negative integer", exception.getMessage());
    }
}
