package demo.repo;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.validation.ValidationException;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticResolver;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import org.springframework.stereotype.Component;

/** Dpp4Fun facade validation, which already composes the SDK core validation layer. */
@Component
final class Dpp4FunRepositoryProfile implements RepositoryProfile {
    private final Dpp4FunValidationService validator = new Dpp4FunValidationService();
    private final Dpp4FunSemanticResolver resolver = new Dpp4FunSemanticResolver();

    @Override
    public String id() {
        return "dpp4fun";
    }

    @Override
    public Dpp4FunSemanticResolver semanticResolver() { return resolver; }

    @Override
    public void validateProfile(DigitalProductPassport passport) {
        try {
            var result = validator.validateProfile(Dpp4Fun.from(passport));
            if (!result.isValid()) {
                throw new ValidationException("Dpp4Fun validation failed: " + result.issues());
            }
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Dpp4Fun profile validation failed: " + exception.getMessage(), exception);
        }
    }
}
