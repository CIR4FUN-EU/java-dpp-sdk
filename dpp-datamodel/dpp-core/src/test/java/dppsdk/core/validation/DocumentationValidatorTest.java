package dppsdk.core.validation;

import dppsdk.core.model.Documentation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DocumentationValidator — semantic business rules.
 *
 * Rule summary:
 * - downloadable=true requires at least one link
 * - availableForYears requires at least one link
 * - link fields must not be blank if present
 */
class DocumentationValidatorTest {

    private final DocumentationValidator validator = new DocumentationValidator();

    @Test
    void validate_nullDocumentation_passes() {
        // Documentation is optional — null passes silently
        assertDoesNotThrow(() -> validator.validate(null));
    }

    @Test
    void validate_downloadableWithLink_passes() {
        Documentation doc = new Documentation.Builder()
                .digitalInstructionsLink("https://example.com/instructions.pdf")
                .downloadable(true)
                .build();
        assertDoesNotThrow(() -> validator.validate(doc));
    }

    @Test
    void validate_downloadableWithoutLink_fails() {
        Documentation doc = new Documentation.Builder()
                .downloadable(true) // no links provided
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(doc));
        assertTrue(ex.getMessage().contains("downloadable"),
                "Expected message about downloadable, got: " + ex.getMessage());
    }

    @Test
    void validate_availableForYearsWithLink_passes() {
        Documentation doc = new Documentation.Builder()
                .safetyInstructionsLink("https://example.com/safety.pdf")
                .availableForYears(10)
                .build();
        assertDoesNotThrow(() -> validator.validate(doc));
    }

    @Test
    void validate_availableForYearsWithoutLink_fails() {
        Documentation doc = new Documentation.Builder()
                .availableForYears(10) // no links provided
                .build();

        assertThrows(ValidationException.class, () -> validator.validate(doc));
    }

    @Test
    void validate_notDownloadableNoLinks_passes() {
        // downloadable=false, no links — that's fine
        Documentation doc = new Documentation.Builder()
                .downloadable(false)
                .paperCopyAvailableOnRequest(true)
                .build();
        assertDoesNotThrow(() -> validator.validate(doc));
    }
}
