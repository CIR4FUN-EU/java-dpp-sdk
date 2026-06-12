package dppsdk.core.payload;

import java.util.List;

/**
 * Transport POJO for {@link dppsdk.core.model.PassportMetadata}.
 *
 * UUID and LocalDate are represented as Strings for transport safety:
 * - uniqueProductIdentifier: UUID.toString() format
 * - passportUpdateDates: ISO-8601 date strings (yyyy-MM-dd)
 */
public class PassportMetadataPayload {

    private String uniqueProductIdentifier;
    private List<String> passportUpdateDates;
    private String qrCodeOrDigitalTag;
    private String externalDocumentationLink;

    public PassportMetadataPayload() {}

    public String getUniqueProductIdentifier() { return uniqueProductIdentifier; }
    public void setUniqueProductIdentifier(String uniqueProductIdentifier) { this.uniqueProductIdentifier = uniqueProductIdentifier; }

    public List<String> getPassportUpdateDates() { return passportUpdateDates; }
    public void setPassportUpdateDates(List<String> passportUpdateDates) { this.passportUpdateDates = passportUpdateDates; }

    public String getQrCodeOrDigitalTag() { return qrCodeOrDigitalTag; }
    public void setQrCodeOrDigitalTag(String qrCodeOrDigitalTag) { this.qrCodeOrDigitalTag = qrCodeOrDigitalTag; }

    public String getExternalDocumentationLink() { return externalDocumentationLink; }
    public void setExternalDocumentationLink(String externalDocumentationLink) { this.externalDocumentationLink = externalDocumentationLink; }
}


