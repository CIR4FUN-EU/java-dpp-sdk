package dppsdk.dpp4fun.model.characteristics;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import java.util.Optional;

/** Immutable typed D4.1 Characteristics view for particleBoard. */
public final class ParticleBoardCharacteristics extends CatalogAspectModel implements FurnitureCharacteristics {
    ParticleBoardCharacteristics(ProfiledAspectState aspect) { super(aspect); if (aspect.variant() != Dpp4FunProductVariant.PARTICLE_BOARD) throw new IllegalArgumentException("Expected particleBoard Characteristics"); }
    public ParticleBoardCharacteristics(DataElement element) { this(new ProfiledAspectState("characteristics", element, Dpp4FunProductVariant.PARTICLE_BOARD)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(Dpp4FunProductVariant.PARTICLE_BOARD).build()); }
    @Override public Dpp4FunProductVariant variant() { return Dpp4FunProductVariant.PARTICLE_BOARD; }
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
    public String getSurface() { return text("surface"); }
    public String getColor() { return text("color"); }
    public String getTexture() { return text("texture"); }
    public Boolean getEasyToClean() { return bool("easyToClean"); }
    public Boolean getEasyToMill() { return bool("easyToMill"); }
    public Boolean getStainResistant() { return bool("stainResistant"); }
    public Boolean getScratchResistant() { return bool("scratchResistant"); }
    public Boolean getAntiBacterial() { return bool("antiBacterial"); }
    public Boolean getCarb2Epa() { return bool("carb2Epa"); }
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
        public Builder surface(String value) { aspect = new TypedAspectAccess(aspect).withString("surface", value).asAspect(); return this; }
        public Builder color(String value) { aspect = new TypedAspectAccess(aspect).withString("color", value).asAspect(); return this; }
        public Builder texture(String value) { aspect = new TypedAspectAccess(aspect).withString("texture", value).asAspect(); return this; }
        public Builder easyToClean(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("easyToClean", value).asAspect(); return this; }
        public Builder easyToMill(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("easyToMill", value).asAspect(); return this; }
        public Builder stainResistant(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("stainResistant", value).asAspect(); return this; }
        public Builder scratchResistant(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("scratchResistant", value).asAspect(); return this; }
        public Builder antiBacterial(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("antiBacterial", value).asAspect(); return this; }
        public Builder carb2Epa(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("carb2Epa", value).asAspect(); return this; }
        private Builder quantity(String field, double value, SemanticUnit unit) {
            aspect = aspect.toBuilder().quantity(field, new Dpp4FunQuantity<>(value, unit)).build(); return this;
        }
        public ParticleBoardCharacteristics build() { return new ParticleBoardCharacteristics(aspect); }
    }
}
