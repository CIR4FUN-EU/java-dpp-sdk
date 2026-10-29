package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 HealthImpact aspect. */
public final class HealthImpact extends CatalogAspectModel {
    HealthImpact(ProfiledAspectState aspect) { super(aspect); if (!"healthImpact".equals(aspect.elementId())) throw new IllegalArgumentException("Expected healthImpact aspect"); }
    public HealthImpact(DataElement element) { this(new ProfiledAspectState("healthImpact", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.HEALTH_IMPACT).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public List<HealthIndicator> getIndicators() { return entries("indicatorSet", HealthIndicator::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof HealthImpact that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder indicators(List<HealthIndicator> value) { editor.entries("indicatorSet", value); return this; }
        public HealthImpact build() { return new HealthImpact(editor.aspect()); }
    }
}
