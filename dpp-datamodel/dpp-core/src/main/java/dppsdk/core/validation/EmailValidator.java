package dppsdk.core.validation;

import dppsdk.core.model.Email;

/**
 * Validator for Email value object.
 * Business rules:
 * - emailAddress must be meaningfully present
 * - typeOfEmail must not be blank if present
 * - Lightweight format sanity check (must contain @)
 */
public class EmailValidator implements Validator<Email> {
    
    @Override
    public void validate(Email email) throws ValidationException {
        if (email == null) {
            return; // Email is optional
        }

        // emailAddress is semantically required when Email object exists
        ValidationUtils.requireNotBlank(email.getEmailAddress(), "Email.emailAddress");

        // Lightweight format sanity: must contain @
        if (email.getEmailAddress() != null && !email.getEmailAddress().contains("@")) {
            throw new ValidationException("Email.emailAddress does not appear to be a valid email (missing @)");
        }

        // typeOfEmail must not be blank if present
        if (email.getTypeOfEmail() != null) {
            ValidationUtils.requireNotBlank(email.getTypeOfEmail(), "Email.typeOfEmail");
        }
    }
}

