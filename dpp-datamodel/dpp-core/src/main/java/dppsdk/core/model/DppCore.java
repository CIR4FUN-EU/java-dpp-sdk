package dppsdk.core.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the reusable core fields shared by Digital Product Passports.
 *
 * Responsibilities:
 * - Groups common DPP metadata and identity submodels
 * - Keeps optional documentation with the common passport data
 *
 * Notes:
 * - Immutable value object
 * - Built via Builder
 */
public class DppCore {

    private final PassportMetadata passportMetadata;
    private final Nameplate nameplate;
    private final Documentation documentation;

    private DppCore(Builder builder) {
        this.passportMetadata = builder.passportMetadata;
        this.nameplate = builder.nameplate;
        this.documentation = builder.documentation;
    }

    public PassportMetadata getPassportMetadata() {
        return passportMetadata;
    }

    public Nameplate getNameplate() {
        return nameplate;
    }

    public Documentation getDocumentation() {
        return documentation;
    }

    public UUID getUniqueProductIdentifier() {
        return passportMetadata.getUniqueProductIdentifier();
    }

    public List<LocalDate> getPassportUpdateDates() {
        return passportMetadata.getPassportUpdateDates();
    }

    public String getQrCodeOrDigitalTag() {
        return passportMetadata.getQrCodeOrDigitalTag();
    }

    public String getExternalDocumentationLink() {
        return passportMetadata.getExternalDocumentationLink();
    }

    public String getGtinCode() {
        return nameplate.getGtinCode();
    }

    public Organization getManufacturer() {
        return nameplate.getManufacturer();
    }

    public Organization getSupplier() {
        return nameplate.getSupplier();
    }

    public String getDigitalInstructionsLink() {
        return documentation == null ? null : documentation.getDigitalInstructionsLink();
    }

    public String getSafetyInstructionsLink() {
        return documentation == null ? null : documentation.getSafetyInstructionsLink();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DppCore dppCore = (DppCore) o;
        return Objects.equals(passportMetadata, dppCore.passportMetadata) &&
                Objects.equals(nameplate, dppCore.nameplate) &&
                Objects.equals(documentation, dppCore.documentation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passportMetadata, nameplate, documentation);
    }

    @Override
    public String toString() {
        return "DppCore{" +
                "passportMetadata=" + passportMetadata +
                ", nameplate=" + nameplate +
                ", documentation=" + documentation +
                '}';
    }

    public Builder toBuilder() {
        return new Builder()
                .passportMetadata(this.passportMetadata)
                .nameplate(this.nameplate)
                .documentation(this.documentation);
    }

    public static class Builder {
        private PassportMetadata passportMetadata;
        private Nameplate nameplate;
        private Documentation documentation;

        public Builder passportMetadata(PassportMetadata passportMetadata) {
            this.passportMetadata = passportMetadata;
            return this;
        }

        public Builder nameplate(Nameplate nameplate) {
            this.nameplate = nameplate;
            return this;
        }

        public Builder documentation(Documentation documentation) {
            this.documentation = documentation;
            return this;
        }

        public DppCore build() {
            if (passportMetadata == null) {
                throw new IllegalArgumentException("passportMetadata is required");
            }
            if (nameplate == null) {
                throw new IllegalArgumentException("nameplate is required");
            }
            return new DppCore(this);
        }
    }
}

