package dppsdk.dpp4fun.validation;

import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.core.validation.ValidationException;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CharacteristicsValidator â€” semantic business rules.
 *
 * Rule summary:
 * - null â†’ fails
 * - productName must be present (builder also enforces this)
 * - features: no nulls, no blanks, no duplicates
 * - dimensions: delegated to DimensionsValidator if present
 */
class CharacteristicsValidatorTest {

    private final CharacteristicsValidator validator = new CharacteristicsValidator();

    @Test
    void validate_validCharacteristics_passes() {
        assertDoesNotThrow(() -> validator.validate(TestDataFactory.validCharacteristics()));
    }

    @Test
    void validate_nullCharacteristics_fails() {
        assertThrows(ValidationException.class, () -> validator.validate(null));
    }

    @Test
    void validate_duplicateFeatures_fails() {
        Characteristics c = new Characteristics.Builder()
                .productName("Test")
                .addFeature("Feature A")
                .addFeature("Feature A") // duplicate
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(c));
        assertTrue(ex.getMessage().toLowerCase().contains("duplicate"),
                "Expected message about duplicate, got: " + ex.getMessage());
    }

    @Test
    void validate_uniqueFeatures_passes() {
        Characteristics c = new Characteristics.Builder()
                .productName("Test")
                .addFeature("Feature A")
                .addFeature("Feature B")
                .build();
        assertDoesNotThrow(() -> validator.validate(c));
    }

    @Test
    void validate_dimensionsDelegated_noUnit_fails() {
        Characteristics c = new Characteristics.Builder()
                .productName("Test")
                .dimensions(new Dimensions.Builder()
                        .width(100.0).height(50.0).depth(30.0)
                        // no unit â€” DimensionsValidator should catch this
                        .build())
                .build();

        assertThrows(ValidationException.class, () -> validator.validate(c));
    }
}


