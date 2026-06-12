package dppsdk.dpp4fun.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Dimensions.Builder â€” local integrity only.
 * Semantic validator rules (unit required, at-least-one) are in DimensionsValidatorTest.
 */
class DimensionsBuilderTest {

    @Test
    void build_allPositiveValues_succeeds() {
        Dimensions d = new Dimensions.Builder()
                .width(100.0).height(50.0).depth(30.0).unit("cm").build();
        assertEquals(100.0, d.getWidth());
        assertEquals(50.0, d.getHeight());
        assertEquals(30.0, d.getDepth());
        assertEquals("cm", d.getUnit());
    }

    @Test
    void build_negativeWidth_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new Dimensions.Builder().width(-1.0).height(50.0).depth(30.0).unit("cm").build()
        );
    }

    @Test
    void build_negativeHeight_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new Dimensions.Builder().width(100.0).height(-5.0).depth(30.0).unit("cm").build()
        );
    }

    @Test
    void build_negativeDepth_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new Dimensions.Builder().width(100.0).height(50.0).depth(-1.0).unit("cm").build()
        );
    }

    @Test
    void build_zeroValues_succeeds() {
        // Zero is allowed â€” non-negative
        assertDoesNotThrow(() ->
                new Dimensions.Builder().width(0.0).height(0.0).depth(0.0).unit("cm").build()
        );
    }
}

