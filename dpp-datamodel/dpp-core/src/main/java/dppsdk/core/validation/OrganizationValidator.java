package dppsdk.core.validation;

import dppsdk.core.model.Organization;

/**
 * Validator for Organization.
 * Business rules:
 * - organization must not be null when validator is invoked
 * - name must be meaningfully present
 * - if uri exists, it must not be blank
 * - validates nested contact if present
 * - role remains optional at pure organization level
 *   (role-in-slot is enforced by parent validators like NameplateValidator)
 */
public class OrganizationValidator implements Validator<Organization> {
    
    private final ContactValidator contactValidator = new ContactValidator();

    @Override
    public void validate(Organization organization) throws ValidationException {
        if (organization == null) {
            throw new ValidationException("Organization is required");
        }

        // name is semantically required
        ValidationUtils.requireNotBlank(organization.getName(), "Organization.name");

        // uri must not be blank if present
        if (organization.getUri() != null) {
            ValidationUtils.requireNotBlank(organization.getUri(), "Organization.uri");
        }

        // Validate nested contact if present
        if (organization.getContact() != null) {
            contactValidator.validate(organization.getContact());
        }
    }
}

