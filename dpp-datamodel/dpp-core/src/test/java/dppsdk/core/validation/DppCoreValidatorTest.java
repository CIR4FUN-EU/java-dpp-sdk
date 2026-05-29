package dppsdk.core.validation;

import dppsdk.core.model.DppCore;
import dppsdk.core.support.CoreTestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DppCoreValidatorTest {

    private final DppCoreValidator validator = new DppCoreValidator();

    @Test
    void validate_validCore_passes() {
        assertDoesNotThrow(() -> validator.validate(CoreTestDataFactory.validDppCore()));
    }

    @Test
    void validate_nullCore_fails() {
        assertThrows(ValidationException.class, () -> validator.validate(null));
    }

    @Test
    void build_missingPassportMetadata_fails() {
        assertThrows(IllegalArgumentException.class, () ->
                new DppCore.Builder()
                        .nameplate(CoreTestDataFactory.validNameplate())
                        .build());
    }

    @Test
    void build_missingNameplate_fails() {
        assertThrows(IllegalArgumentException.class, () ->
                new DppCore.Builder()
                        .passportMetadata(CoreTestDataFactory.validPassportMetadata())
                        .build());
    }

    @Test
    void validate_nullDocumentation_passes() {
        DppCore core = new DppCore.Builder()
                .passportMetadata(CoreTestDataFactory.validPassportMetadata())
                .nameplate(CoreTestDataFactory.validNameplate())
                .build();

        assertDoesNotThrow(() -> validator.validate(core));
    }
}
