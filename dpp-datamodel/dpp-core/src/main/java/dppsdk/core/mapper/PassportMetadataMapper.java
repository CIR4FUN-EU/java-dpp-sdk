package dppsdk.core.mapper;

import dppsdk.core.model.PassportMetadata;
import dppsdk.core.payload.PassportMetadataPayload;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Maps between {@link PassportMetadata} and {@link PassportMetadataPayload}.
 *
 * <p>Type conversions handled here:</p>
 * <ul>
 *   <li>UUID ↔ String (via {@code toString()} / {@code UUID.fromString()})</li>
 *   <li>LocalDate ↔ String (ISO-8601 format via {@code toString()} / {@code LocalDate.parse()})</li>
 * </ul>
 */
public class PassportMetadataMapper implements Mapper<PassportMetadata, PassportMetadataPayload> {

    @Override
    public PassportMetadataPayload toPayload(PassportMetadata domain) {
        if (domain == null) return null;

        PassportMetadataPayload p = new PassportMetadataPayload();
        p.setUniqueProductIdentifier(
                domain.getUniqueProductIdentifier() != null
                        ? domain.getUniqueProductIdentifier().toString()
                        : null);

        if (domain.getPassportUpdateDates() != null) {
            p.setPassportUpdateDates(
                    domain.getPassportUpdateDates().stream()
                            .map(LocalDate::toString)
                            .collect(Collectors.toList()));
        }

        p.setQrCodeOrDigitalTag(domain.getQrCodeOrDigitalTag());
        p.setExternalDocumentationLink(domain.getExternalDocumentationLink());
        return p;
    }

    @Override
    public PassportMetadata toDomain(PassportMetadataPayload payload) {
        if (payload == null) return null;

        UUID uuid = null;
        if (payload.getUniqueProductIdentifier() != null) {
            try {
                uuid = UUID.fromString(payload.getUniqueProductIdentifier());
            } catch (IllegalArgumentException e) {
                throw new MappingException(
                        "Invalid UUID in PassportMetadataPayload: '"
                                + payload.getUniqueProductIdentifier() + "'", e);
            }
        }

        try {
            PassportMetadata.Builder builder = new PassportMetadata.Builder();
            if (payload.getUniqueProductIdentifier() != null) {
                builder.uniqueProductIdentifier(uuid);
            }
            if (payload.getQrCodeOrDigitalTag() != null) {
                builder.qrCodeOrDigitalTag(payload.getQrCodeOrDigitalTag());
            }
            if (payload.getExternalDocumentationLink() != null) {
                builder.externalDocumentationLink(payload.getExternalDocumentationLink());
            }

            if (payload.getPassportUpdateDates() != null) {
                for (String dateStr : payload.getPassportUpdateDates()) {
                    try {
                        builder.addPassportUpdateDate(LocalDate.parse(dateStr));
                    } catch (DateTimeParseException e) {
                        throw new MappingException(
                                "Invalid date in PassportMetadataPayload.passportUpdateDates: '"
                                        + dateStr + "'", e);
                    }
                }
            }

            return builder.build();
        } catch (MappingException e) {
            throw e; // re-throw our own exceptions
        } catch (IllegalArgumentException e) {
            throw new MappingException(
                    "Failed to map PassportMetadataPayload to PassportMetadata: " + e.getMessage(), e);
        }
    }
}

