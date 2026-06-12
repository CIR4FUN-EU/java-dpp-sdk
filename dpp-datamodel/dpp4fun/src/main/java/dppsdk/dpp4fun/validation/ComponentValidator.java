package dppsdk.dpp4fun.validation;

import dppsdk.core.validation.ValidationException;
import dppsdk.core.validation.ValidationUtils;
import dppsdk.core.validation.Validator;
import dppsdk.dpp4fun.model.Component;

/**
 * Validator for Component value object in BOM.
 * Business rules:
 * - name must be meaningfully present
 * - if reference exists, it must not be blank
 */
public class ComponentValidator implements Validator<Component> {
    
    @Override
    public void validate(Component component) throws ValidationException {
        if (component == null) {
            return; // Component is optional
        }

        // name is semantically required
        ValidationUtils.requireNotBlank(component.getName(), "Component.name");

        // if reference exists, it must not be blank
        if (component.getReference() != null) {
            ValidationUtils.requireNotBlank(component.getReference(), "Component.reference");
        }
    }
}


