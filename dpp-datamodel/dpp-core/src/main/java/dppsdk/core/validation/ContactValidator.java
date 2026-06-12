package dppsdk.core.validation;

import dppsdk.core.model.Contact;

/**
 * Validator for Contact value object.
 * Business rules:
 * - organization must be meaningfully present
 * - at least one contact channel (address, email, or telephone) must exist
 * - validates nested address/email/telephone if present
 */
public class ContactValidator implements Validator<Contact> {
    
    private final AddressValidator addressValidator = new AddressValidator();
    private final EmailValidator emailValidator = new EmailValidator();
    private final TelephoneValidator telephoneValidator = new TelephoneValidator();

    @Override
    public void validate(Contact contact) throws ValidationException {
        if (contact == null) {
            return; // Contact is optional
        }

        // organization is semantically required
        ValidationUtils.requireNotBlank(contact.getOrganization(), "Contact.organization");

        // At least one contact channel must exist
        if (contact.getAddress() == null && contact.getEmail() == null && contact.getTelephone() == null) {
            throw new ValidationException(
                "Contact must provide at least one contact channel (address, email, or telephone)"
            );
        }

        // Validate nested objects if present
        if (contact.getAddress() != null) {
            addressValidator.validate(contact.getAddress());
        }

        if (contact.getEmail() != null) {
            emailValidator.validate(contact.getEmail());
        }

        if (contact.getTelephone() != null) {
            telephoneValidator.validate(contact.getTelephone());
        }
    }
}

