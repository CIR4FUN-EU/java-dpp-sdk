package dppsdk.dpp4fun.validation;

import dppsdk.core.validation.ValidationException;
import dppsdk.core.validation.ValidationUtils;
import dppsdk.core.validation.Validator;
import dppsdk.dpp4fun.model.ProductClassification;
import java.util.List;

/**
 * Validator for ProductClassification.
 * Business rules:
 * - ProductClassification must not be null
 * - sector must be meaningfully present
 * - category must be meaningfully present
 * - if subCategory exists, category must exist (hierarchy consistency)
 * - if group exists, sector must exist (hierarchy consistency)
 * - tags must not contain null, blank, or duplicate entries
 */
public class ProductClassificationValidator implements Validator<ProductClassification> {
    
    @Override
    public void validate(ProductClassification classification) throws ValidationException {
        if (classification == null) {
            throw new ValidationException("ProductClassification is required");
        }

        // sector is semantically required
        ValidationUtils.requireNotBlank(classification.getSector(), "ProductClassification.sector");

        // category is semantically required
        ValidationUtils.requireNotBlank(classification.getCategory(), "ProductClassification.category");

        // group must not be blank if present
        if (classification.getGroup() != null) {
            ValidationUtils.requireNotBlank(classification.getGroup(), "ProductClassification.group");
        }

        // subCategory must not be blank if present
        if (classification.getSubCategory() != null) {
            ValidationUtils.requireNotBlank(classification.getSubCategory(), "ProductClassification.subCategory");
        }

        // Hierarchy consistency: if subCategory exists, category must exist
        if (ValidationUtils.hasText(classification.getSubCategory())
                && !ValidationUtils.hasText(classification.getCategory())) {
            throw new ValidationException(
                "ProductClassification.subCategory is set but category is missing"
            );
        }

        // Hierarchy consistency: if group exists, sector must exist
        if (ValidationUtils.hasText(classification.getGroup())
                && !ValidationUtils.hasText(classification.getSector())) {
            throw new ValidationException(
                "ProductClassification.group is set but sector is missing"
            );
        }

        // Tags: no null, blank, or duplicate entries
        List<String> tags = classification.getTags();
        if (tags != null && !tags.isEmpty()) {
            ValidationUtils.requireCleanStringList(tags, "ProductClassification.tags");
        }
    }
}


