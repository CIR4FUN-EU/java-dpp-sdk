package dppsdk.core.validation.identifier;

import dppsdk.core.model.DppHeader;
import dppsdk.core.validation.ValidationIssue;
import dppsdk.core.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

/** Validates explicitly selected identifier profiles using local syntax and semantics only. */
public final class IdentifierValidator {

    /** Validate the selected identifier methods on a DPP header. */
    public ValidationResult validate(DppHeader header, IdentifierValidationContext context) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (header == null) {
            issues.add(new ValidationIssue("header", "IDENTIFIER_HEADER_REQUIRED",
                    "DppHeader is required"));
            return ValidationResult.of(issues);
        }
        if (context == null) {
            issues.add(new ValidationIssue("context", "IDENTIFIER_VALIDATION_CONTEXT_REQUIRED",
                    "IdentifierValidationContext is required"));
            return ValidationResult.of(issues);
        }

        switch (context.productIdentifierMethod()) {
            case GS1_DIGITAL_LINK -> new Gs1DigitalLinkValidator().validate(
                    header.getUniqueProductIdentifier(), header.getGranularity(), issues);
            case IDENTIFICATION_LINK -> new IdentificationLinkValidator().validate(
                    header.getUniqueProductIdentifier(), header.getGranularity(), issues);
            case UNSUPPORTED -> unsupported(issues, "header.uniqueProductIdentifier",
                    "The selected product identifier method is outside this SDK milestone");
        }
        if (context.economicOperatorIdentifierMethod()
                == EconomicOperatorIdentifierMethod.UNSUPPORTED) {
            unsupported(issues, "header.economicOperatorId",
                    "The selected economic-operator identifier method is outside this SDK milestone");
        } else {
            new Gs1GlnValidator().validate(header.getEconomicOperatorId(),
                    "header.economicOperatorId", Gs1GlnValidator.Role.PARTY, issues);
        }
        if (header.getFacilityId() != null) {
            if (context.facilityIdentifierMethod() == null
                    || context.facilityIdentifierMethod() == FacilityIdentifierMethod.UNSUPPORTED) {
                unsupported(issues, "header.facilityId",
                        "The selected facility identifier method is outside this SDK milestone");
            } else {
                new Gs1GlnValidator().validate(header.getFacilityId(), "header.facilityId",
                        Gs1GlnValidator.Role.PHYSICAL_LOCATION, issues);
            }
        }
        return ValidationResult.of(issues);
    }

    private static void unsupported(List<ValidationIssue> issues, String path, String message) {
        issues.add(new ValidationIssue(path, "IDENTIFIER_METHOD_UNSUPPORTED", message));
    }
}
