package dppsdk.dpp4fun.validation;

import dppsdk.core.validation.ValidationException;
import dppsdk.core.validation.ValidationUtils;
import dppsdk.core.validation.Validator;
import dppsdk.dpp4fun.model.Characteristics;
import java.util.List;

/**
 * Validator for Characteristics.
 * Business rules:
 * - Characteristics must not be null
 * - productName must be meaningfully present
 * - weight must be non-negative if present
 * - validates Dimensions if present
 * - features list must not contain null, blank, or duplicate entries
 */
public class CharacteristicsValidator implements Validator<Characteristics> {
    
    private final DimensionsValidator dimensionsValidator = new DimensionsValidator();

    @Override
    public void validate(Characteristics characteristics) throws ValidationException {
        if (characteristics == null) {
            throw new ValidationException("Characteristics is required");
        }

        // productName is semantically required
        ValidationUtils.requireNotBlank(characteristics.getProductName(), "Characteristics.productName");

        // Weight must be non-negative if provided
        ValidationUtils.requireNonNegativeIfPresent(characteristics.getWeight(), "Characteristics.weight");

        // Validate dimensions if present
        if (characteristics.getDimensions() != null) {
            dimensionsValidator.validate(characteristics.getDimensions());
        }

        // Features list: no null, blank, or duplicate entries
        List<String> features = characteristics.getFeatures();
        if (features != null && !features.isEmpty()) {
            ValidationUtils.requireCleanStringList(features, "Characteristics.features");
        }
    }
}


