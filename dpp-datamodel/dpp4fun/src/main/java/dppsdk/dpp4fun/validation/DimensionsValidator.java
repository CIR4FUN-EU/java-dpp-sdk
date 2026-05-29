package dppsdk.dpp4fun.validation;

import dppsdk.core.validation.ValidationException;
import dppsdk.core.validation.ValidationUtils;
import dppsdk.core.validation.Validator;
import dppsdk.dpp4fun.model.Dimensions;

/**
 * Validator for Dimensions value object.
 * Business rules:
 * - If dimensions exist, at least one of width/height/depth should be present
 * - If any dimension value is present, unit must be present and not blank
 * - All dimension values must be non-negative if present
 */
public class DimensionsValidator implements Validator<Dimensions> {
    
    @Override
    public void validate(Dimensions dimensions) throws ValidationException {
        if (dimensions == null) {
            return; // Dimensions are optional
        }

        // Non-negative checks
        ValidationUtils.requireNonNegativeIfPresent(dimensions.getWidth(), "Dimensions.width");
        ValidationUtils.requireNonNegativeIfPresent(dimensions.getHeight(), "Dimensions.height");
        ValidationUtils.requireNonNegativeIfPresent(dimensions.getDepth(), "Dimensions.depth");

        boolean hasAnyValue = dimensions.getWidth() != null
                || dimensions.getHeight() != null
                || dimensions.getDepth() != null;

        // If Dimensions object exists, at least one dimension value should be present
        if (!hasAnyValue) {
            throw new ValidationException(
                "Dimensions object exists but no dimension values (width/height/depth) are provided"
            );
        }

        // If any dimension value is present, unit must be present
        if (hasAnyValue) {
            ValidationUtils.requireNotBlank(dimensions.getUnit(), "Dimensions.unit");
        }
    }
}


