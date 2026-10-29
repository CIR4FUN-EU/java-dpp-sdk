package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 Recyclability aspect. */
public final class Recyclability extends CatalogAspectModel {
    Recyclability(ProfiledAspectState aspect) { super(aspect); if (!"recyclability".equals(aspect.elementId())) throw new IllegalArgumentException("Expected recyclability aspect"); }
    public Recyclability(DataElement element) { this(new ProfiledAspectState("recyclability", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.RECYCLABILITY).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public Double getRecycledContent() { return decimal("recycledContent"); }
    public Boolean getRecyclable() { return bool("recyclable"); }
    public Boolean getRecycledMaterialIncluded() { return bool("recycledMaterialIncluded"); }
    public List<Certificate> getCertificates() { return entries("certificateSet", Certificate::new); }
    public List<StandardAssessment> getStandards() { return entries("standardSet", StandardAssessment::new); }
    public List<Instruction> getRecyclingInstructions() { return entries("recyclingInstructionSet", Instruction::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Recyclability that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder recycledContent(Double value) { editor.decimal("recycledContent", value); return this; }
        public Builder recyclable(Boolean value) { editor.bool("recyclable", value); return this; }
        public Builder recycledMaterialIncluded(Boolean value) { editor.bool("recycledMaterialIncluded", value); return this; }
        public Builder certificates(List<Certificate> value) { editor.entries("certificateSet", value); return this; }
        public Builder standards(List<StandardAssessment> value) { editor.entries("standardSet", value); return this; }
        public Builder recyclingInstructions(List<Instruction> value) { editor.entries("recyclingInstructionSet", value); return this; }
        public Recyclability build() { return new Recyclability(editor.aspect()); }
    }
}
