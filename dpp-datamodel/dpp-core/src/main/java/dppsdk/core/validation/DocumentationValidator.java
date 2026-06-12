package dppsdk.core.validation;

import dppsdk.core.model.Documentation;

/**
 * Validator for Documentation.
 * Business rules:
 * - if downloadable == true, at least one documentation link must exist
 * - if a link field is present, it must not be blank
 * - availableForYears must be non-negative if present
 * - if availableForYears is set, there should be at least some documentation basis
 *   (at least one digital or safety link)
 */
public class DocumentationValidator implements Validator<Documentation> {
    
    @Override
    public void validate(Documentation documentation) throws ValidationException {
        if (documentation == null) {
            return; // Documentation is optional
        }

        String digitalLink = documentation.getDigitalInstructionsLink();
        String safetyLink = documentation.getSafetyInstructionsLink();

        // Link fields must not be blank if present
        if (digitalLink != null) {
            ValidationUtils.requireNotBlank(digitalLink, "Documentation.digitalInstructionsLink");
        }
        if (safetyLink != null) {
            ValidationUtils.requireNotBlank(safetyLink, "Documentation.safetyInstructionsLink");
        }

        boolean hasAnyLink = ValidationUtils.hasText(digitalLink) || ValidationUtils.hasText(safetyLink);

        // If downloadable, at least one link should exist
        if (documentation.isDownloadable() && !hasAnyLink) {
            throw new ValidationException(
                "Documentation is marked as downloadable but no documentation links are provided"
            );
        }

        // Available for years must be non-negative if provided
        if (documentation.getAvailableForYears() != null) {
            ValidationUtils.requireNonNegativeIfPresent(documentation.getAvailableForYears(),
                "Documentation.availableForYears");

            // If availableForYears is set, there should be some documentation basis
            if (!hasAnyLink) {
                throw new ValidationException(
                    "Documentation.availableForYears is set but no documentation links are provided"
                );
            }
        }
    }
}

