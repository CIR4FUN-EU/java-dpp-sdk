package dppsdk.core.mapper;

import dppsdk.core.model.Documentation;
import dppsdk.core.payload.DocumentationPayload;

/**
 * Maps between {@link Documentation} and {@link DocumentationPayload}.
 */
public class DocumentationMapper implements Mapper<Documentation, DocumentationPayload> {

    @Override
    public DocumentationPayload toPayload(Documentation domain) {
        if (domain == null) return null;

        DocumentationPayload p = new DocumentationPayload();
        p.setDigitalInstructionsLink(domain.getDigitalInstructionsLink());
        p.setSafetyInstructionsLink(domain.getSafetyInstructionsLink());
        p.setDownloadable(domain.isDownloadable());
        p.setAvailableForYears(domain.getAvailableForYears());
        p.setPaperCopyAvailableOnRequest(domain.isPaperCopyAvailableOnRequest());
        return p;
    }

    @Override
    public Documentation toDomain(DocumentationPayload payload) {
        if (payload == null) return null;

        try {
            Documentation.Builder builder = new Documentation.Builder()
                    .downloadable(payload.isDownloadable())
                    .paperCopyAvailableOnRequest(payload.isPaperCopyAvailableOnRequest());
            if (payload.getDigitalInstructionsLink() != null) {
                builder.digitalInstructionsLink(payload.getDigitalInstructionsLink());
            }
            if (payload.getSafetyInstructionsLink() != null) {
                builder.safetyInstructionsLink(payload.getSafetyInstructionsLink());
            }
            if (payload.getAvailableForYears() != null) {
                builder.availableForYears(payload.getAvailableForYears());
            }
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new MappingException(
                    "Failed to map DocumentationPayload to Documentation: " + e.getMessage(), e);
        }
    }
}

