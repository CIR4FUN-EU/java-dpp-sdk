package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 Durability aspect. */
public final class Durability extends CatalogAspectModel {
    Durability(ProfiledAspectState aspect) { super(aspect); if (!"durability".equals(aspect.elementId())) throw new IllegalArgumentException("Expected durability aspect"); }
    public Durability(DataElement element) { this(new ProfiledAspectState("durability", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.DURABILITY).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public String getCycleLife() { return text("cycleLife"); }
    public String getExpectedLifetime() { return text("expectedLifetime"); }
    public List<Certificate> getCertificates() { return entries("certificateSet", Certificate::new); }
    public List<StandardAssessment> getStandards() { return entries("standardSet", StandardAssessment::new); }
    public List<Scoring> getScorings() { return entries("scoringSet", Scoring::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Durability that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder cycleLife(String value) { editor.text("cycleLife", value); return this; }
        public Builder expectedLifetime(String value) { editor.text("expectedLifetime", value); return this; }
        public Builder certificates(List<Certificate> value) { editor.entries("certificateSet", value); return this; }
        public Builder standards(List<StandardAssessment> value) { editor.entries("standardSet", value); return this; }
        public Builder scorings(List<Scoring> value) { editor.entries("scoringSet", value); return this; }
        public Durability build() { return new Durability(editor.aspect()); }
    }
}
