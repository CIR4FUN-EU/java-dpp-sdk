package dppsdk.dpp4fun.validation;

import dppsdk.core.validation.ValidationException;
import dppsdk.core.validation.ValidationUtils;
import dppsdk.core.validation.Validator;
import dppsdk.dpp4fun.model.Part;

/**
 * Validator for Part value object in BOM.
 * Business rules:
 * - name must be meaningfully present
 * - if reference exists, it must not be blank
 */
public class PartValidator implements Validator<Part> {
    
    @Override
    public void validate(Part part) throws ValidationException {
        if (part == null) {
            return; // Part is optional
        }

        // name is semantically required
        ValidationUtils.requireNotBlank(part.getName(), "Part.name");

        // if reference exists, it must not be blank
        if (part.getReference() != null) {
            ValidationUtils.requireNotBlank(part.getReference(), "Part.reference");
        }
    }
}


