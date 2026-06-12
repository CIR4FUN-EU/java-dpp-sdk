package dppsdk.core.validation;

import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.support.CoreTestDataFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NameplateValidator — semantic business rules.
 *
 * Rule summary:
 * - gtinCode must be present
 * - at least one of manufacturer/supplier must be present
 * - manufacturer slot → role must be MANUFACTURER (null role fails)
 * - supplier slot → role must be SUPPLIER (null role fails)
 */
class NameplateValidatorTest {

    private final NameplateValidator validator = new NameplateValidator();

    @Test
    void validate_validManufacturer_passes() {
        assertDoesNotThrow(() -> validator.validate(CoreTestDataFactory.validNameplate()));
    }

    @Test
    void validate_validSupplier_passes() {
        Nameplate n = new Nameplate.Builder()
                .gtinCode("GTIN-001")
                .supplier(CoreTestDataFactory.validSupplier())
                .build();
        assertDoesNotThrow(() -> validator.validate(n));
    }

    @Test
    void validate_bothManufacturerAndSupplier_passes() {
        Nameplate n = new Nameplate.Builder()
                .gtinCode("GTIN-001")
                .manufacturer(CoreTestDataFactory.validManufacturer())
                .supplier(CoreTestDataFactory.validSupplier())
                .build();
        assertDoesNotThrow(() -> validator.validate(n));
    }

    @Test
    void validate_noManufacturerNoSupplier_fails() {
        Nameplate n = new Nameplate.Builder().gtinCode("GTIN-001").build();
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(n));
        assertTrue(ex.getMessage().contains("manufacturer") || ex.getMessage().contains("supplier"),
                "Expected message about missing manufacturer/supplier, got: " + ex.getMessage());
    }

    @Test
    void validate_manufacturerWithWrongRole_fails() {
        // Organization built with SUPPLIER role placed in manufacturer slot
        Organization wrongRole = new Organization.Builder()
                .name("Wrong Role Co")
                .role(OrganizationRole.SUPPLIER)
                .build();

        Nameplate n = new Nameplate.Builder()
                .gtinCode("GTIN-001")
                .manufacturer(wrongRole) // wrong role for this slot
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(n));
        assertTrue(ex.getMessage().contains("MANUFACTURER"),
                "Expected message about MANUFACTURER role, got: " + ex.getMessage());
    }

    @Test
    void validate_supplierWithWrongRole_fails() {
        Organization wrongRole = new Organization.Builder()
                .name("Wrong Role Co")
                .role(OrganizationRole.MANUFACTURER)
                .build();

        Nameplate n = new Nameplate.Builder()
                .gtinCode("GTIN-001")
                .supplier(wrongRole) // wrong role for supplier slot
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(n));
        assertTrue(ex.getMessage().contains("SUPPLIER"),
                "Expected message about SUPPLIER role, got: " + ex.getMessage());
    }

    @Test
    void validate_nullNameplate_fails() {
        assertThrows(ValidationException.class, () -> validator.validate(null));
    }

    @Test
    void validate_nullInput_fails() {
        assertThrows(ValidationException.class, () -> validator.validate(null));
    }
}
