package dppsdk.dpp4fun.model.characteristics;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import java.util.Optional;

/** Immutable typed D4.1 Characteristics view for crib. */
public final class CribCharacteristics extends CatalogAspectModel implements FurnitureCharacteristics {
    CribCharacteristics(ProfiledAspectState aspect) { super(aspect); if (aspect.variant() != Dpp4FunProductVariant.CRIB) throw new IllegalArgumentException("Expected crib Characteristics"); }
    public CribCharacteristics(DataElement element) { this(new ProfiledAspectState("characteristics", element, Dpp4FunProductVariant.CRIB)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(Dpp4FunProductVariant.CRIB).build()); }
    @Override public Dpp4FunProductVariant variant() { return Dpp4FunProductVariant.CRIB; }
    public Builder toBuilder() { return new Builder(profileState()); }
    public Double getLengthValue() { return decimal("length"); }
    public String getLengthUnitSymbol() { return text("lengthUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getLength() { return new TypedAspectAccess(profileState()).quantity("length"); }
    public Double getWidthValue() { return decimal("width"); }
    public String getWidthUnitSymbol() { return text("widthUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getWidth() { return new TypedAspectAccess(profileState()).quantity("width"); }
    public Double getHeightValue() { return decimal("height"); }
    public String getHeightUnitSymbol() { return text("heightUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getHeight() { return new TypedAspectAccess(profileState()).quantity("height"); }
    public Double getDiameterValue() { return decimal("diameter"); }
    public String getDiameterUnitSymbol() { return text("diameterUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getDiameter() { return new TypedAspectAccess(profileState()).quantity("diameter"); }
    public Double getWeightValue() { return decimal("weight"); }
    public String getWeightUnitSymbol() { return text("weightUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getWeight() { return new TypedAspectAccess(profileState()).quantity("weight"); }
    public Double getVolumeValue() { return decimal("volume"); }
    public String getVolumeUnitSymbol() { return text("volumeUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getVolume() { return new TypedAspectAccess(profileState()).quantity("volume"); }
    public Double getInternalLengthValue() { return decimal("internalLength"); }
    public String getInternalLengthUnitSymbol() { return text("lengthUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getInternalLength() { return new TypedAspectAccess(profileState()).quantity("internalLength"); }
    public Double getInternalWidthValue() { return decimal("internalWidth"); }
    public String getInternalWidthUnitSymbol() { return text("widthUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getInternalWidth() { return new TypedAspectAccess(profileState()).quantity("internalWidth"); }
    public String getSurface() { return text("surface"); }
    public String getColor() { return text("color"); }
    public Double getPayload() { return decimal("payload"); }
    public Boolean getMattressIncluded() { return bool("matelasIncluded"); }
    public static final class Builder {
        private ProfiledAspectState aspect;
        private Builder(ProfiledAspectState aspect) { this.aspect = aspect; }
        public Builder length(double value, LengthUnit unit) { return quantity("length", value, unit); }
        public Builder length(double value, SemanticUnit unit) { return quantity("length", value, unit); }
        public Builder width(double value, LengthUnit unit) { return quantity("width", value, unit); }
        public Builder width(double value, SemanticUnit unit) { return quantity("width", value, unit); }
        public Builder height(double value, LengthUnit unit) { return quantity("height", value, unit); }
        public Builder height(double value, SemanticUnit unit) { return quantity("height", value, unit); }
        public Builder diameter(double value, LengthUnit unit) { return quantity("diameter", value, unit); }
        public Builder diameter(double value, SemanticUnit unit) { return quantity("diameter", value, unit); }
        public Builder weight(double value, MassUnit unit) { return quantity("weight", value, unit); }
        public Builder weight(double value, SemanticUnit unit) { return quantity("weight", value, unit); }
        public Builder volume(double value, VolumeUnit unit) { return quantity("volume", value, unit); }
        public Builder volume(double value, SemanticUnit unit) { return quantity("volume", value, unit); }
        public Builder internalLength(double value, LengthUnit unit) { return quantity("internalLength", value, unit); }
        public Builder internalLength(double value, SemanticUnit unit) { return quantity("internalLength", value, unit); }
        public Builder internalWidth(double value, LengthUnit unit) { return quantity("internalWidth", value, unit); }
        public Builder internalWidth(double value, SemanticUnit unit) { return quantity("internalWidth", value, unit); }
        public Builder surface(String value) { aspect = new TypedAspectAccess(aspect).withString("surface", value).asAspect(); return this; }
        public Builder color(String value) { aspect = new TypedAspectAccess(aspect).withString("color", value).asAspect(); return this; }
        public Builder payload(Double value) { aspect = new TypedAspectAccess(aspect).withDecimal("payload", value).asAspect(); return this; }
        public Builder mattressIncluded(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("matelasIncluded", value).asAspect(); return this; }
        private Builder quantity(String field, double value, SemanticUnit unit) {
            aspect = aspect.toBuilder().quantity(field, new Dpp4FunQuantity<>(value, unit)).build(); return this;
        }
        public CribCharacteristics build() { return new CribCharacteristics(aspect); }
    }
}
