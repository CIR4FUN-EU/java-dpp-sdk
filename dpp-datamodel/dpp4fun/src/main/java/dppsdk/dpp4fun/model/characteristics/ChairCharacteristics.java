package dppsdk.dpp4fun.model.characteristics;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import java.util.Optional;

/** Immutable typed D4.1 Characteristics view for chair. */
public final class ChairCharacteristics extends CatalogAspectModel implements FurnitureCharacteristics {
    ChairCharacteristics(ProfiledAspectState aspect) { super(aspect); if (aspect.variant() != Dpp4FunProductVariant.CHAIR) throw new IllegalArgumentException("Expected chair Characteristics"); }
    public ChairCharacteristics(DataElement element) { this(new ProfiledAspectState("characteristics", element, Dpp4FunProductVariant.CHAIR)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(Dpp4FunProductVariant.CHAIR).build()); }
    @Override public Dpp4FunProductVariant variant() { return Dpp4FunProductVariant.CHAIR; }
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
    public Double getSeatHeightValue() { return decimal("seatHeight"); }
    public String getSeatHeightUnitSymbol() { return text("heightUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getSeatHeight() { return new TypedAspectAccess(profileState()).quantity("seatHeight"); }
    public Double getSeatWidthValue() { return decimal("seatWidth"); }
    public String getSeatWidthUnitSymbol() { return text("widthUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getSeatWidth() { return new TypedAspectAccess(profileState()).quantity("seatWidth"); }
    public Double getSeatLengthValue() { return decimal("seatLength"); }
    public String getSeatLengthUnitSymbol() { return text("lengthUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getSeatLength() { return new TypedAspectAccess(profileState()).quantity("seatLength"); }
    public Double getArmrestHeightValue() { return decimal("armrestHeight"); }
    public String getArmrestHeightUnitSymbol() { return text("heightUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getArmrestHeight() { return new TypedAspectAccess(profileState()).quantity("armrestHeight"); }
    public Double getArmrestWidthValue() { return decimal("armrestWidth"); }
    public String getArmrestWidthUnitSymbol() { return text("widthUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getArmrestWidth() { return new TypedAspectAccess(profileState()).quantity("armrestWidth"); }
    public Double getBackrestHeightValue() { return decimal("backrestHeight"); }
    public String getBackrestHeightUnitSymbol() { return text("heightUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getBackrestHeight() { return new TypedAspectAccess(profileState()).quantity("backrestHeight"); }
    public Double getLegHeightValue() { return decimal("legHeight"); }
    public String getLegHeightUnitSymbol() { return text("heightUnit"); }
    public Optional<Dpp4FunQuantity<? extends SemanticUnit>> getLegHeight() { return new TypedAspectAccess(profileState()).quantity("legHeight"); }
    public String getSurface() { return text("surface"); }
    public String getColor() { return text("color"); }
    public String getFoamHardnessLevel() { return text("foamHardnessLevel"); }
    public Integer getSeatingCapacity() { return integer("seatingCapacity"); }
    public Boolean getIndoorUse() { return bool("indoorUse"); }
    public Boolean getOutdoorUse() { return bool("outdoorUse"); }
    public Boolean getReclining() { return bool("reclining"); }
    public Boolean getSeatSliding() { return bool("seatSliding"); }
    public Boolean getSwivel() { return bool("swivel"); }
    public Boolean getWaterRepellent() { return bool("waterRepellent"); }
    public Boolean getStainResistant() { return bool("stainResistant"); }
    public Boolean getEasyToClean() { return bool("easyToClean"); }
    public Boolean getFireResistant() { return bool("fireResistant"); }
    public Boolean getSeatRemovableCover() { return bool("seatRemovableCover"); }
    public Boolean getBackrestRemovableCover() { return bool("backrestRemovableCover"); }
    public Boolean getAdjustableFeet() { return bool("adjustableFeet"); }
    public Boolean getArmrestIncluded() { return bool("armrestIncluded"); }
    public Boolean getArmrestRemovableCover() { return bool("armrestRemovableCover"); }
    public Boolean getStackable() { return bool("stackable"); }
    public Boolean getHeightAdjustable() { return bool("heightAdjustable"); }
    public Boolean getPadded() { return bool("padded"); }
    public Double getPayload() { return decimal("payload"); }
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
        public Builder seatHeight(double value, LengthUnit unit) { return quantity("seatHeight", value, unit); }
        public Builder seatHeight(double value, SemanticUnit unit) { return quantity("seatHeight", value, unit); }
        public Builder seatWidth(double value, LengthUnit unit) { return quantity("seatWidth", value, unit); }
        public Builder seatWidth(double value, SemanticUnit unit) { return quantity("seatWidth", value, unit); }
        public Builder seatLength(double value, LengthUnit unit) { return quantity("seatLength", value, unit); }
        public Builder seatLength(double value, SemanticUnit unit) { return quantity("seatLength", value, unit); }
        public Builder armrestHeight(double value, LengthUnit unit) { return quantity("armrestHeight", value, unit); }
        public Builder armrestHeight(double value, SemanticUnit unit) { return quantity("armrestHeight", value, unit); }
        public Builder armrestWidth(double value, LengthUnit unit) { return quantity("armrestWidth", value, unit); }
        public Builder armrestWidth(double value, SemanticUnit unit) { return quantity("armrestWidth", value, unit); }
        public Builder backrestHeight(double value, LengthUnit unit) { return quantity("backrestHeight", value, unit); }
        public Builder backrestHeight(double value, SemanticUnit unit) { return quantity("backrestHeight", value, unit); }
        public Builder legHeight(double value, LengthUnit unit) { return quantity("legHeight", value, unit); }
        public Builder legHeight(double value, SemanticUnit unit) { return quantity("legHeight", value, unit); }
        public Builder surface(String value) { aspect = new TypedAspectAccess(aspect).withString("surface", value).asAspect(); return this; }
        public Builder color(String value) { aspect = new TypedAspectAccess(aspect).withString("color", value).asAspect(); return this; }
        public Builder foamHardnessLevel(String value) { aspect = new TypedAspectAccess(aspect).withString("foamHardnessLevel", value).asAspect(); return this; }
        public Builder seatingCapacity(Integer value) { aspect = new TypedAspectAccess(aspect).withInteger("seatingCapacity", value).asAspect(); return this; }
        public Builder indoorUse(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("indoorUse", value).asAspect(); return this; }
        public Builder outdoorUse(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("outdoorUse", value).asAspect(); return this; }
        public Builder reclining(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("reclining", value).asAspect(); return this; }
        public Builder seatSliding(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("seatSliding", value).asAspect(); return this; }
        public Builder swivel(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("swivel", value).asAspect(); return this; }
        public Builder waterRepellent(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("waterRepellent", value).asAspect(); return this; }
        public Builder stainResistant(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("stainResistant", value).asAspect(); return this; }
        public Builder easyToClean(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("easyToClean", value).asAspect(); return this; }
        public Builder fireResistant(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("fireResistant", value).asAspect(); return this; }
        public Builder seatRemovableCover(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("seatRemovableCover", value).asAspect(); return this; }
        public Builder backrestRemovableCover(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("backrestRemovableCover", value).asAspect(); return this; }
        public Builder adjustableFeet(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("adjustableFeet", value).asAspect(); return this; }
        public Builder armrestIncluded(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("armrestIncluded", value).asAspect(); return this; }
        public Builder armrestRemovableCover(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("armrestRemovableCover", value).asAspect(); return this; }
        public Builder stackable(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("stackable", value).asAspect(); return this; }
        public Builder heightAdjustable(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("heightAdjustable", value).asAspect(); return this; }
        public Builder padded(Boolean value) { aspect = new TypedAspectAccess(aspect).withBoolean("padded", value).asAspect(); return this; }
        public Builder payload(Double value) { aspect = new TypedAspectAccess(aspect).withDecimal("payload", value).asAspect(); return this; }
        private Builder quantity(String field, double value, SemanticUnit unit) {
            aspect = aspect.toBuilder().quantity(field, new Dpp4FunQuantity<>(value, unit)).build(); return this;
        }
        public ChairCharacteristics build() { return new ChairCharacteristics(aspect); }
    }
}
