package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 EnvironmentalImpact aspect. */
public final class EnvironmentalImpact extends CatalogAspectModel {
    EnvironmentalImpact(ProfiledAspectState aspect) { super(aspect); if (!"environmentalImpact".equals(aspect.elementId())) throw new IllegalArgumentException("Expected environmentalImpact aspect"); }
    public EnvironmentalImpact(DataElement element) { this(new ProfiledAspectState("environmentalImpact", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.ENVIRONMENTAL_IMPACT).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public List<Indicator> getIndicators() { return entries("indicatorSet", Indicator::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof EnvironmentalImpact that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder indicators(List<Indicator> value) { editor.entries("indicatorSet", value); return this; }
        public EnvironmentalImpact build() { return new EnvironmentalImpact(editor.aspect()); }
    }
}
