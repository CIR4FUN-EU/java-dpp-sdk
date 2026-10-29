package dppsdk.dpp4fun.model.characteristics;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import java.util.Optional;

/** Immutable typed D4.1 Characteristics view for fabric. */
public final class FabricCharacteristics extends CatalogAspectModel implements FurnitureCharacteristics {
    FabricCharacteristics(ProfiledAspectState aspect) { super(aspect); if (aspect.variant() != Dpp4FunProductVariant.FABRIC) throw new IllegalArgumentException("Expected fabric Characteristics"); }
    public FabricCharacteristics(DataElement element) { this(new ProfiledAspectState("characteristics", element, Dpp4FunProductVariant.FABRIC)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(Dpp4FunProductVariant.FABRIC).build()); }
    @Override public Dpp4FunProductVariant variant() { return Dpp4FunProductVariant.FABRIC; }
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
    public String getLightFastness() { return text("lightFastness"); }
    public String getAbrasionResistance() { return text("abrasionResistance"); }
    public String getPilling() { return text("pilling"); }
    public String getSeamSlippage() { return text("seamSlippage"); }
    public Boolean getFlameRetardant() { return bool("flameRetardant"); }
    public Integer getColorFastness() { return integer("colorFastness"); }
    public String getTexture() { return text("texture"); }
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
        public Builder lightFastness(String value) { aspect = new TypedAspectAccess(aspect).withString("lightFastness", value).asAspect(); return this; }
        public Builder abrasionResistance(String value) { aspect = new TypedAspectAccess(aspect).withString("abrasionResistance", value).asAspect(); return this; }
        public Builder pilling(String value) { aspect = new TypedAspectAccess(aspect).withString("pilling", value).asAspect(); return this; }
        public Builder seamSlippage(String value) { aspect = new TypedAspectAccess(aspect).withString("seamSlippage", value).asAspect(); return this; }
        public Builder flameRetardant(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("flameRetardant", value).asAspect(); return this; }
        public Builder colorFastness(Integer value) { aspect = new TypedAspectAccess(aspect).withInteger("colorFastness", value).asAspect(); return this; }
        public Builder texture(String value) { aspect = new TypedAspectAccess(aspect).withString("texture", value).asAspect(); return this; }
        private Builder quantity(String field, double value, SemanticUnit unit) {
            aspect = aspect.toBuilder().quantity(field, new Dpp4FunQuantity<>(value, unit)).build(); return this;
        }
        public FabricCharacteristics build() { return new FabricCharacteristics(aspect); }
    }
}
