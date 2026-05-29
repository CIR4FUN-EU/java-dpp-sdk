package dppsdk.core.validation;

import dppsdk.core.model.PassportMetadata;
import java.time.LocalDate;
import java.util.List;

/**
 * Validator for PassportMetadata.
 * Business rules:
 * - PassportMetadata must not be null
 * - uniqueProductIdentifier must exist
 * - passportUpdateDates must not be null or empty, must not contain nulls
 * - passportUpdateDates should not contain future dates
 * - qrCodeOrDigitalTag must not be blank if present
 * - externalDocumentationLink must not be blank if present
 */
public class PassportMetadataValidator implements Validator<PassportMetadata> {
    
    @Override
    public void validate(PassportMetadata metadata) throws ValidationException {
        if (metadata == null) {
            throw new ValidationException("PassportMetadata is required");
        }

        // uniqueProductIdentifier must exist
        ValidationUtils.requireNotNull(metadata.getUniqueProductIdentifier(), 
            "PassportMetadata.uniqueProductIdentifier");

        // passportUpdateDates must not be null or empty
        List<LocalDate> updateDates = metadata.getPassportUpdateDates();
        if (updateDates == null || updateDates.isEmpty()) {
            throw new ValidationException("PassportMetadata.passportUpdateDates must not be empty");
        }

        // Validate entries: no nulls, no future dates
        LocalDate today = LocalDate.now();
        for (int i = 0; i < updateDates.size(); i++) {
            LocalDate date = updateDates.get(i);
            if (date == null) {
                throw new ValidationException("PassportMetadata.passportUpdateDates[" + i + "] is null");
            }
            if (date.isAfter(today)) {
                throw new ValidationException(
                    "PassportMetadata.passportUpdateDates[" + i + "] is in the future: " + date
                );
            }
        }

        // qrCodeOrDigitalTag must not be blank if present
        if (metadata.getQrCodeOrDigitalTag() != null) {
            ValidationUtils.requireNotBlank(metadata.getQrCodeOrDigitalTag(), 
                "PassportMetadata.qrCodeOrDigitalTag");
        }

        // externalDocumentationLink must not be blank if present
        if (metadata.getExternalDocumentationLink() != null) {
            ValidationUtils.requireNotBlank(metadata.getExternalDocumentationLink(), 
                "PassportMetadata.externalDocumentationLink");
        }
    }
}

