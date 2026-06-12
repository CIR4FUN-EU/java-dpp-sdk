package dppsdk.core.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Represents the base Digital Product Passport structure.
 *
 * Responsibilities:
 * - Provides generic core DPP access
 * - Defines contract for specific DPP types
 *
 * Notes:
 * - Abstract base class
 */
public abstract class Dpp {

    private static final String DPP_ID_PATH =
            "PassportMetadata.uniqueProductIdentifier is required for dppId";
    private static final String PRODUCT_ID_PATH =
            "Nameplate.gtinCode is required for productId";

    public abstract DppCore getCoreDpp();

    public abstract String getPassportType();

    public PassportMetadata getPassportMetadata() {
        return getCoreDpp().getPassportMetadata();
    }

    public Nameplate getNameplate() {
        return getCoreDpp().getNameplate();
    }

    public Documentation getDocumentation() {
        return getCoreDpp().getDocumentation();
    }

    public UUID getUniqueProductIdentifier() {
        return getCoreDpp().getUniqueProductIdentifier();
    }

    public String getDppId() {
        UUID uniqueProductIdentifier = getUniqueProductIdentifier();
        if (uniqueProductIdentifier == null) {
            throw new IllegalStateException(DPP_ID_PATH);
        }
        return uniqueProductIdentifier.toString();
    }

    public List<LocalDate> getPassportUpdateDates() {
        return getCoreDpp().getPassportUpdateDates();
    }

    public String getQrCodeOrDigitalTag() {
        return getCoreDpp().getQrCodeOrDigitalTag();
    }

    public String getExternalDocumentationLink() {
        return getCoreDpp().getExternalDocumentationLink();
    }

    public String getGtinCode() {
        return getCoreDpp().getGtinCode();
    }

    public String getProductId() {
        String gtinCode = getGtinCode();
        if (gtinCode == null) {
            throw new IllegalStateException(PRODUCT_ID_PATH);
        }
        return gtinCode;
    }

    public Organization getManufacturer() {
        return getCoreDpp().getManufacturer();
    }

    public Organization getSupplier() {
        return getCoreDpp().getSupplier();
    }

    public String getDigitalInstructionsLink() {
        return getCoreDpp().getDigitalInstructionsLink();
    }

    public String getSafetyInstructionsLink() {
        return getCoreDpp().getSafetyInstructionsLink();
    }
}

