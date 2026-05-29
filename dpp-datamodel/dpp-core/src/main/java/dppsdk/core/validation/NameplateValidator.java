package dppsdk.core.validation;

import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;

/**
 * Validator for Nameplate.
 * Business rules:
 * - Nameplate must not be null
 * - gtinCode must be meaningfully present
 * - Nameplate must have at least one of manufacturer or supplier
 * - if manufacturer is present: validate it, role must be MANUFACTURER (null role fails)
 * - if supplier is present: validate it, role must be SUPPLIER (null role fails)
 * - optional identifier fields must not be blank if present
 */
public class NameplateValidator implements Validator<Nameplate> {
    
    private final OrganizationValidator organizationValidator = new OrganizationValidator();

    @Override
    public void validate(Nameplate nameplate) throws ValidationException {
        if (nameplate == null) {
            throw new ValidationException("Nameplate is required");
        }

        // gtinCode is semantically required
        ValidationUtils.requireNotBlank(nameplate.getGtinCode(), "Nameplate.gtinCode");

        // Optional identifier fields must not be blank if present
        if (nameplate.getInternalArticleNumber() != null) {
            ValidationUtils.requireNotBlank(nameplate.getInternalArticleNumber(), "Nameplate.internalArticleNumber");
        }
        if (nameplate.getBatchNumber() != null) {
            ValidationUtils.requireNotBlank(nameplate.getBatchNumber(), "Nameplate.batchNumber");
        }
        if (nameplate.getCustomsTariffNumber() != null) {
            ValidationUtils.requireNotBlank(nameplate.getCustomsTariffNumber(), "Nameplate.customsTariffNumber");
        }
        if (nameplate.getUriOfTheProduct() != null) {
            ValidationUtils.requireNotBlank(nameplate.getUriOfTheProduct(), "Nameplate.uriOfTheProduct");
        }

        // Must have at least one of manufacturer or supplier
        if (nameplate.getManufacturer() == null && nameplate.getSupplier() == null) {
            throw new ValidationException(
                "Nameplate must have at least a manufacturer or supplier"
            );
        }

        // Validate manufacturer if present — role must be MANUFACTURER (null role fails)
        Organization manufacturer = nameplate.getManufacturer();
        if (manufacturer != null) {
            organizationValidator.validate(manufacturer);
            if (manufacturer.getRole() == null) {
                throw new ValidationException(
                    "Nameplate.manufacturer must have role MANUFACTURER, but role is null"
                );
            }
            if (manufacturer.getRole() != OrganizationRole.MANUFACTURER) {
                throw new ValidationException(
                    "Nameplate.manufacturer must have role MANUFACTURER, but got " + manufacturer.getRole()
                );
            }
        }

        // Validate supplier if present — role must be SUPPLIER (null role fails)
        Organization supplier = nameplate.getSupplier();
        if (supplier != null) {
            organizationValidator.validate(supplier);
            if (supplier.getRole() == null) {
                throw new ValidationException(
                    "Nameplate.supplier must have role SUPPLIER, but role is null"
                );
            }
            if (supplier.getRole() != OrganizationRole.SUPPLIER) {
                throw new ValidationException(
                    "Nameplate.supplier must have role SUPPLIER, but got " + supplier.getRole()
                );
            }
        }
    }
}

