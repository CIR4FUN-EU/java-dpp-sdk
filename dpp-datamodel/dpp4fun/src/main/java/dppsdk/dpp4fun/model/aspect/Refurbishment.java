package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 Refurbishment aspect. */
public final class Refurbishment extends CatalogAspectModel {
    Refurbishment(ProfiledAspectState aspect) { super(aspect); if (!"refurbishment".equals(aspect.elementId())) throw new IllegalArgumentException("Expected refurbishment aspect"); }
    public Refurbishment(DataElement element) { this(new ProfiledAspectState("refurbishment", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.REFURBISHMENT).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public List<Instruction> getInstructions() { return entries("refurbishmentInstructionSet", Instruction::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Refurbishment that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder instructions(List<Instruction> value) { editor.entries("refurbishmentInstructionSet", value); return this; }
        public Refurbishment build() { return new Refurbishment(editor.aspect()); }
    }
}
