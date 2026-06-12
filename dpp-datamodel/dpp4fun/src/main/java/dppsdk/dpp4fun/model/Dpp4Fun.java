package dppsdk.dpp4fun.model;

import dppsdk.core.model.Dpp;
import dppsdk.core.model.DppCore;

import java.util.List;
import java.util.Objects;

/**
 * Represents a complete Digital Product Passport for furniture in the Dpp4Fun domain.
 *
 * Responsibilities:
 * - Holds all logical submodels together
 * - Defines the root of the DPP object graph
 *
 * Notes:
 * - Immutable value object
 * - Built via Builder
 */
public class Dpp4Fun extends Dpp {

    private final DppCore coreDpp;
    private final ProductClassification classification;
    private final Characteristics characteristics;
    private final BillOfMaterials billOfMaterials;

    private Dpp4Fun(Builder builder) {
        this.coreDpp = builder.coreDpp;
        this.classification = builder.classification;
        this.characteristics = builder.characteristics;
        this.billOfMaterials = builder.billOfMaterials;
    }

    @Override
    public DppCore getCoreDpp() {
        return coreDpp;
    }

    public ProductClassification getClassification() {
        return classification;
    }

    public Characteristics getCharacteristics() {
        return characteristics;
    }

    public BillOfMaterials getBillOfMaterials() {
        return billOfMaterials;
    }

    public String getSector() {
        return classification.getSector();
    }

    public String getGroup() {
        return classification.getGroup();
    }

    public String getCategory() {
        return classification.getCategory();
    }

    public String getSubCategory() {
        return classification.getSubCategory();
    }

    public List<String> getTags() {
        return classification.getTags();
    }

    public String getProductName() {
        return characteristics.getProductName();
    }

    public String getDescription() {
        return characteristics.getDescription();
    }

    public String getBrand() {
        return characteristics.getBrand();
    }

    public String getProductType() {
        return characteristics.getProductType();
    }

    public Dimensions getDimensions() {
        return characteristics.getDimensions();
    }

    public Double getWeight() {
        return characteristics.getWeight();
    }

    public String getColor() {
        return characteristics.getColor();
    }

    public List<String> getFeatures() {
        return characteristics.getFeatures();
    }

    @Override
    public String getPassportType() {
        return "Dpp4Fun Furniture";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dpp4Fun that = (Dpp4Fun) o;
        return Objects.equals(coreDpp, that.coreDpp) &&
                Objects.equals(classification, that.classification) &&
                Objects.equals(characteristics, that.characteristics) &&
                Objects.equals(billOfMaterials, that.billOfMaterials);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coreDpp, classification, characteristics, billOfMaterials);
    }

    @Override
    public String toString() {
        return "Dpp4Fun{" +
                "passportType='" + getPassportType() + '\'' +
                ", coreDpp=" + coreDpp +
                ", classification=" + classification +
                ", characteristics=" + characteristics +
                '}';
    }

    public Builder toBuilder() {
        return new Builder()
                .coreDpp(this.coreDpp)
                .classification(this.classification)
                .characteristics(this.characteristics)
                .billOfMaterials(this.billOfMaterials);
    }

    public static class Builder {
        private DppCore coreDpp;
        private ProductClassification classification;
        private Characteristics characteristics;
        private BillOfMaterials billOfMaterials;

        public Builder coreDpp(DppCore coreDpp) {
            this.coreDpp = coreDpp;
            return this;
        }

        public Builder classification(ProductClassification classification) {
            this.classification = classification;
            return this;
        }

        public Builder characteristics(Characteristics characteristics) {
            this.characteristics = characteristics;
            return this;
        }

        public Builder billOfMaterials(BillOfMaterials billOfMaterials) {
            this.billOfMaterials = billOfMaterials;
            return this;
        }

        public Dpp4Fun build() {
            if (coreDpp == null) {
                throw new IllegalArgumentException("coreDpp is required");
            }
            if (classification == null) {
                throw new IllegalArgumentException("classification is required");
            }
            if (characteristics == null) {
                throw new IllegalArgumentException("characteristics is required");
            }
            return new Dpp4Fun(this);
        }
    }
}



