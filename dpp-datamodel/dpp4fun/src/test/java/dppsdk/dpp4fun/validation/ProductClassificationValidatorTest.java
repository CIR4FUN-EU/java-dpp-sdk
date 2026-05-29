package dppsdk.dpp4fun.validation;

import dppsdk.dpp4fun.model.ProductClassification;
import dppsdk.core.validation.ValidationException;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProductClassificationValidator â€” semantic business rules.
 *
 * Rule summary:
 * - null â†’ fails
 * - sector and category are required
 * - tags: no nulls, no blanks, no duplicates
 * - hierarchy: subCategory requires category; group requires sector
 */
class ProductClassificationValidatorTest {

    private final ProductClassificationValidator validator = new ProductClassificationValidator();

    @Test
    void validate_validClassification_passes() {
        assertDoesNotThrow(() -> validator.validate(TestDataFactory.validProductClassification()));
    }

    @Test
    void validate_nullClassification_fails() {
        assertThrows(ValidationException.class, () -> validator.validate(null));
    }

    @Test
    void validate_duplicateTags_fails() {
        ProductClassification c = new ProductClassification.Builder()
                .sector("Furniture")
                .category("Beds")
                .addTag("premium")
                .addTag("premium") // duplicate
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(c));
        assertTrue(ex.getMessage().toLowerCase().contains("duplicate"),
                "Expected message about duplicate tag, got: " + ex.getMessage());
    }

    @Test
    void validate_uniqueTags_passes() {
        ProductClassification c = new ProductClassification.Builder()
                .sector("Furniture")
                .category("Beds")
                .addTag("premium")
                .addTag("ergonomic")
                .build();
        assertDoesNotThrow(() -> validator.validate(c));
    }
}

