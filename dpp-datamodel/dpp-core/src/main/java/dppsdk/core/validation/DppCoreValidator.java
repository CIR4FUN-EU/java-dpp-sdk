package dppsdk.core.validation;

import dppsdk.core.model.DppCore;

/**
 * Validator for the reusable core Digital Product Passport fields.
 *
 * Fail-fast: throws ValidationException on first error.
 */
public class DppCoreValidator implements Validator<DppCore> {

    private final PassportMetadataValidator passportMetadataValidator = new PassportMetadataValidator();
    private final NameplateValidator nameplateValidator = new NameplateValidator();
    private final DocumentationValidator documentationValidator = new DocumentationValidator();

    @Override
    public void validate(DppCore coreDpp) throws ValidationException {
        if (coreDpp == null) {
            throw new ValidationException("DppCore cannot be null");
        }

        ValidationUtils.requireNotNull(coreDpp.getPassportMetadata(), "DppCore.passportMetadata");
        ValidationUtils.requireNotNull(coreDpp.getNameplate(), "DppCore.nameplate");

        passportMetadataValidator.validate(coreDpp.getPassportMetadata());
        nameplateValidator.validate(coreDpp.getNameplate());
        documentationValidator.validate(coreDpp.getDocumentation());
    }
}

