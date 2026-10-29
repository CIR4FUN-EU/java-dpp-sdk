package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 Reliability aspect. */
public final class Reliability extends CatalogAspectModel {
    Reliability(ProfiledAspectState aspect) { super(aspect); if (!"reliability".equals(aspect.elementId())) throw new IllegalArgumentException("Expected reliability aspect"); }
    public Reliability(DataElement element) { this(new ProfiledAspectState("reliability", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.RELIABILITY).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public List<Certificate> getCertificates() { return entries("certificateSet", Certificate::new); }
    public List<StandardAssessment> getStandards() { return entries("standardSet", StandardAssessment::new); }
    public List<Scoring> getScorings() { return entries("scoringSet", Scoring::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Reliability that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder certificates(List<Certificate> value) { editor.entries("certificateSet", value); return this; }
        public Builder standards(List<StandardAssessment> value) { editor.entries("standardSet", value); return this; }
        public Builder scorings(List<Scoring> value) { editor.entries("scoringSet", value); return this; }
        public Reliability build() { return new Reliability(editor.aspect()); }
    }
}
