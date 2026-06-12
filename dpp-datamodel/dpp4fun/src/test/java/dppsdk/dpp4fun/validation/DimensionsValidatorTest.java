package dppsdk.dpp4fun.validation;

import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.core.validation.ValidationException;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DimensionsValidator â€” semantic business rules.
 *
 * Rule summary:
 * - null dimensions passes silently (optional)
 * - If dimensions object exists, at least one value must be present
 * - If any value is present, unit must be present
 */
class DimensionsValidatorTest {

    private final DimensionsValidator validator = new DimensionsValidator();

    @Test
    void validate_nullDimensions_passes() {
        assertDoesNotThrow(() -> validator.validate(null));
    }

    @Test
    void validate_validDimensions_passes() {
        assertDoesNotThrow(() -> validator.validate(TestDataFactory.validDimensions()));
    }

    @Test
    void validate_dimensionsWithNoUnit_fails() {
        // Dimensions.Builder.build() already requires all three dims be non-negative but doesn't
        // require unit. We create via direct Dimensions if needed, but since Builder checks
        // non-null for w/h/d, we rely on the validator's unit check.
        // The easiest way: build with unit=null (Builder allows it)
        Dimensions d = new Dimensions.Builder()
                .width(100.0).height(50.0).depth(30.0)
                // unit intentionally omitted
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(d));
        assertTrue(ex.getMessage().contains("unit"),
                "Expected message about unit, got: " + ex.getMessage());
    }

    @Test
    void validate_dimensionsWithAllValuesButNoUnit_fails() {
        // All three dims present but no unit â€” validator requires unit when values exist
        // Note: Dimensions.Builder requires all three dims so we provide them all;
        // only unit is optional at builder level.
        Dimensions d = new Dimensions.Builder()
                .width(100.0).height(50.0).depth(30.0)
                // unit intentionally omitted â€” the validator should catch this
                .build();
        assertThrows(ValidationException.class, () -> validator.validate(d));
    }
}

