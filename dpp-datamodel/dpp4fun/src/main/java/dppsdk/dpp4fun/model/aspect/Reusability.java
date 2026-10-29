package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 Reusability aspect. */
public final class Reusability extends CatalogAspectModel {
    Reusability(ProfiledAspectState aspect) { super(aspect); if (!"reusability".equals(aspect.elementId())) throw new IllegalArgumentException("Expected reusability aspect"); }
    public Reusability(DataElement element) { this(new ProfiledAspectState("reusability", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.REUSABILITY).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public Boolean getSecondHand() { return bool("secondHand"); }
    public List<Scoring> getScorings() { return entries("scoringSet", Scoring::new); }
    public List<Instruction> getScoringInstructions() { return entries("scoringInstructionSet", Instruction::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Reusability that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder secondHand(Boolean value) { editor.bool("secondHand", value); return this; }
        public Builder scorings(List<Scoring> value) { editor.entries("scoringSet", value); return this; }
        public Builder scoringInstructions(List<Instruction> value) { editor.entries("scoringInstructionSet", value); return this; }
        public Reusability build() { return new Reusability(editor.aspect()); }
    }
}
