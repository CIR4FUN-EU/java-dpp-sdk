package dppsdk.dpp4fun.validation;

import dppsdk.core.validation.ValidationException;
import dppsdk.core.validation.ValidationUtils;
import dppsdk.core.validation.Validator;
import dppsdk.dpp4fun.model.Material;

/**
 * Validator for Material value object in BOM.
 * Business rules:
 * - name must be meaningfully present
 * - portion must be non-negative
 * - if reference exists, it must not be blank
 * - if mandatory == true, portion should be > 0
 */
public class MaterialValidator implements Validator<Material> {
    
    @Override
    public void validate(Material material) throws ValidationException {
        if (material == null) {
            return; // Material is optional
        }

        // name is semantically required
        ValidationUtils.requireNotBlank(material.getName(), "Material.name");

        // portion must be non-negative
        ValidationUtils.requireNonNegative(material.getPortion(), "Material.portion");

        // if reference exists, it must not be blank
        if (material.getReference() != null) {
            ValidationUtils.requireNotBlank(material.getReference(), "Material.reference");
        }

        // if mandatory, portion should be > 0
        if (material.isMandatory() && material.getPortion() <= 0) {
            throw new ValidationException(
                "Material '" + material.getName() + "' is mandatory but has zero or no portion"
            );
        }
    }
}


