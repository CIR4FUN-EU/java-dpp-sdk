package dppsdk.core.payload;

/**
 * Transport POJO for {@link dppsdk.core.model.DppCore}.
 * Canonical payload representation of the reusable DPP core structure.
 */
public class DppCorePayload {

    private PassportMetadataPayload passportMetadata;
    private NameplatePayload nameplate;
    private DocumentationPayload documentation;

    public DppCorePayload() {}

    public PassportMetadataPayload getPassportMetadata() {
        return passportMetadata;
    }

    public void setPassportMetadata(PassportMetadataPayload passportMetadata) {
        this.passportMetadata = passportMetadata;
    }

    public NameplatePayload getNameplate() {
        return nameplate;
    }

    public void setNameplate(NameplatePayload nameplate) {
        this.nameplate = nameplate;
    }

    public DocumentationPayload getDocumentation() {
        return documentation;
    }

    public void setDocumentation(DocumentationPayload documentation) {
        this.documentation = documentation;
    }
}


