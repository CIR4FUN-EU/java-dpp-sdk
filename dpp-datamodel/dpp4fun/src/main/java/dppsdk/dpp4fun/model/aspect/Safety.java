package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 Safety aspect. */
public final class Safety extends CatalogAspectModel {
    Safety(ProfiledAspectState aspect) { super(aspect); if (!"safety".equals(aspect.elementId())) throw new IllegalArgumentException("Expected safety aspect"); }
    public Safety(DataElement element) { this(new ProfiledAspectState("safety", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.SAFETY).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public String getAgeRestriction() { return text("ageRestriction"); }
    public Boolean getProfessionalUseOnly() { return bool("professionalUseOnly"); }
    public List<Certificate> getCertificates() { return entries("certificateSet", Certificate::new); }
    public List<StandardAssessment> getStandards() { return entries("standardSet", StandardAssessment::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Safety that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder ageRestriction(String value) { editor.text("ageRestriction", value); return this; }
        public Builder professionalUseOnly(Boolean value) { editor.bool("professionalUseOnly", value); return this; }
        public Builder certificates(List<Certificate> value) { editor.entries("certificateSet", value); return this; }
        public Builder standards(List<StandardAssessment> value) { editor.entries("standardSet", value); return this; }
        public Safety build() { return new Safety(editor.aspect()); }
    }
}
