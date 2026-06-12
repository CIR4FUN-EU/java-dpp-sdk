package dppsdk.dpp4fun.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dppsdk.core.payload.DocumentationPayload;
import dppsdk.core.payload.DppCorePayload;
import dppsdk.core.payload.NameplatePayload;
import dppsdk.core.payload.PassportMetadataPayload;

/**
 * Transport POJO for {@link dppsdk.dpp4fun.model.Dpp4Fun}.
 * Top-level payload representing the complete Digital Product Passport.
 *
 * <p>The canonical payload structure mirrors the domain model and nests shared
 * fields under {@code coreDpp}. The flat accessors remain as compatibility
 * helpers for existing Java callers, but they are not the primary structure.</p>
 */
public class Dpp4FunPayload {

    private DppCorePayload coreDpp;
    private ProductClassificationPayload classification;
    private CharacteristicsPayload characteristics;
    private BillOfMaterialsPayload billOfMaterials;

    public Dpp4FunPayload() {}

    public DppCorePayload getCoreDpp() {
        return coreDpp;
    }

    public void setCoreDpp(DppCorePayload coreDpp) {
        this.coreDpp = coreDpp;
    }

    public ProductClassificationPayload getClassification() { return classification; }
    public void setClassification(ProductClassificationPayload classification) { this.classification = classification; }

    public CharacteristicsPayload getCharacteristics() { return characteristics; }
    public void setCharacteristics(CharacteristicsPayload characteristics) { this.characteristics = characteristics; }

    public BillOfMaterialsPayload getBillOfMaterials() { return billOfMaterials; }
    public void setBillOfMaterials(BillOfMaterialsPayload billOfMaterials) { this.billOfMaterials = billOfMaterials; }

    @JsonIgnore
    public PassportMetadataPayload getPassportMetadata() {
        return coreDpp == null ? null : coreDpp.getPassportMetadata();
    }

    @JsonIgnore
    public void setPassportMetadata(PassportMetadataPayload passportMetadata) {
        ensureCoreDpp().setPassportMetadata(passportMetadata);
    }

    @JsonIgnore
    public NameplatePayload getNameplate() {
        return coreDpp == null ? null : coreDpp.getNameplate();
    }

    @JsonIgnore
    public void setNameplate(NameplatePayload nameplate) {
        ensureCoreDpp().setNameplate(nameplate);
    }

    @JsonIgnore
    public DocumentationPayload getDocumentation() {
        return coreDpp == null ? null : coreDpp.getDocumentation();
    }

    @JsonIgnore
    public void setDocumentation(DocumentationPayload documentation) {
        ensureCoreDpp().setDocumentation(documentation);
    }

    private DppCorePayload ensureCoreDpp() {
        if (coreDpp == null) {
            coreDpp = new DppCorePayload();
        }
        return coreDpp;
    }
}



