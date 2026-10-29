package demo.repo;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.validation.DppValidator;
import dppsdk.core.validation.ValidationException;
import dppsdk.core.validation.ValidationResult;
import dppsdk.core.validation.format.DataFormatValidator;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

/** Core semantic and format validation for any canonical passport. */
@Component
final class CoreRepositoryProfile implements RepositoryProfile {
    private final DppValidator coreValidator = new DppValidator();
    private final DataFormatValidator formatValidator = new DataFormatValidator();

    @Override
    public String id() {
        return "core";
    }

    @Override
    public void validateProfile(DigitalProductPassport passport) {
        ValidationResult core = coreValidator.validate(passport);
        ValidationResult formats = formatValidator.validate(passport);
        ValidationResult combined = ValidationResult.of(Stream.concat(core.issues().stream(),
                formats.issues().stream()).toList());
        if (!combined.isValid()) {
            throw new ValidationException("Core validation failed: " + combined.issues());
        }
    }
}
