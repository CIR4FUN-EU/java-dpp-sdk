package dppsdk.core.validation;

import dppsdk.core.model.Nameplate;
import dppsdk.core.support.CoreTestDataFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationService — facade dispatch.
 *
 * Rule summary:
 * - Correctly dispatches to typed validators
 * - Throws for unsupported types
 */
class ValidationServiceTest {

    private final ValidationService service = new ValidationService();

    @Test
    void validate_validCore_passes() {
        assertDoesNotThrow(() -> service.validate(CoreTestDataFactory.validDppCore()));
    }

    @Test
    void validate_validNameplate_passes() {
        assertDoesNotThrow(() -> service.validate(CoreTestDataFactory.validNameplate()));
    }

    @Test
    void validate_unsupportedType_throws() {
        // String has no registered validator
        assertThrows(ValidationException.class, () -> service.validate("unregistered-type"));
    }

    @Test
    void validate_nameplateWithWrongRole_fails() {
        Nameplate invalid = new Nameplate.Builder()
                .gtinCode("GTIN-001")
                .manufacturer(CoreTestDataFactory.validSupplier())
                .build();

        assertThrows(ValidationException.class, () -> service.validate(invalid));
    }
}
