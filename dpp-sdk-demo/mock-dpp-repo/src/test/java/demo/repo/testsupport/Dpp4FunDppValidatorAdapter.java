package demo.repo.testsupport;

import dpp.repo.client.core.DppValidator;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import java.util.Objects;

public final class Dpp4FunDppValidatorAdapter implements DppValidator<Dpp4Fun> {

    private final Dpp4FunValidationService validationService;

    public Dpp4FunDppValidatorAdapter() {
        this(new Dpp4FunValidationService());
    }

    public Dpp4FunDppValidatorAdapter(Dpp4FunValidationService validationService) {
        this.validationService = Objects.requireNonNull(validationService, "validationService must not be null");
    }

    @Override
    public void validate(Dpp4Fun dpp) {
        validationService.validate(dpp);
    }
}
