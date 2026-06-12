package dppsdk.core.validation;

import dppsdk.core.model.Telephone;

/**
 * Validator for Telephone value object.
 * Business rules:
 * - telephoneNumber must be meaningfully present
 * - typeOfTelephone must not be blank if present
 */
public class TelephoneValidator implements Validator<Telephone> {
    
    @Override
    public void validate(Telephone telephone) throws ValidationException {
        if (telephone == null) {
            return; // Telephone is optional
        }

        // telephoneNumber is semantically required when Telephone object exists
        ValidationUtils.requireNotBlank(telephone.getTelephoneNumber(), "Telephone.telephoneNumber");

        // typeOfTelephone must not be blank if present
        if (telephone.getTypeOfTelephone() != null) {
            ValidationUtils.requireNotBlank(telephone.getTypeOfTelephone(), "Telephone.typeOfTelephone");
        }
    }
}

