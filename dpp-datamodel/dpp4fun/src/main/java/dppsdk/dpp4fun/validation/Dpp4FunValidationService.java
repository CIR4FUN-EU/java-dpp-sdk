package dppsdk.dpp4fun.validation;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.validation.ValidationIssue;
import dppsdk.core.validation.ValidationResult;
import dppsdk.core.validation.DppValidator;
import dppsdk.core.validation.format.DataFormatValidator;
import dppsdk.core.validation.identifier.IdentifierValidationContext;
import dppsdk.core.validation.identifier.IdentifierValidator;
import dppsdk.dpp4fun.model.Dpp4Fun;

import java.util.ArrayList;
import java.util.List;

/** Stateless orchestration facade for core and Dpp4Fun profile validation. */
public final class Dpp4FunValidationService {
    private final DppValidator dppValidator = new DppValidator();
    private final DataFormatValidator formatValidator = new DataFormatValidator();
    private final IdentifierValidator identifierValidator = new IdentifierValidator();
    private final Dpp4FunValidator profileValidator = new Dpp4FunValidator();

    /** Runs generic core semantics, generic format rules, and Dpp4Fun profile rules. */
    public ValidationResult validate(Dpp4Fun value) { return validate(value, null); }

    /** Also runs core identifier validation using the exact caller-selected methods. */
    public ValidationResult validate(Dpp4Fun value, IdentifierValidationContext context) {
        if (value == null) return profileValidator.validate(null);
        List<ValidationIssue> issues = new ArrayList<>();
        issues.addAll(dppValidator.validate(value.asDigitalProductPassport()).issues());
        issues.addAll(formatValidator.validate(value.asDigitalProductPassport()).issues());
        if (context != null) issues.addAll(identifierValidator.validate(value.getHeader(), context).issues());
        issues.addAll(profileValidator.validate(value).issues());
        return ValidationResult.of(issues);
    }

    /** Runs only Dpp4Fun-specific profile rules; useful when identifier context is not available. */
    public ValidationResult validateProfile(Dpp4Fun value) { return profileValidator.validate(value); }
}
