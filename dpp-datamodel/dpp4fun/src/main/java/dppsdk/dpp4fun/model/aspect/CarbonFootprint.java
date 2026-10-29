package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 CarbonFootprint aspect. */
public final class CarbonFootprint extends CatalogAspectModel {
    CarbonFootprint(ProfiledAspectState aspect) { super(aspect); if (!"carbonFootprint".equals(aspect.elementId())) throw new IllegalArgumentException("Expected carbonFootprint aspect"); }
    public CarbonFootprint(DataElement element) { this(new ProfiledAspectState("carbonFootprint", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.CARBON_FOOTPRINT).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public List<CarbonFootprintCalculation> getCalculations() { return entries("carbonFootprintCalculationSet", CarbonFootprintCalculation::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof CarbonFootprint that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder calculations(List<CarbonFootprintCalculation> value) { editor.entries("carbonFootprintCalculationSet", value); return this; }
        public CarbonFootprint build() { return new CarbonFootprint(editor.aspect()); }
    }
}
