package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 Remanufacturing aspect. */
public final class Remanufacturing extends CatalogAspectModel {
    Remanufacturing(ProfiledAspectState aspect) { super(aspect); if (!"remanufacturing".equals(aspect.elementId())) throw new IllegalArgumentException("Expected remanufacturing aspect"); }
    public Remanufacturing(DataElement element) { this(new ProfiledAspectState("remanufacturing", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.REMANUFACTURING).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public List<Instruction> getAssemblyInstructions() { return entries("assemblyInstructionSet", Instruction::new); }
    public List<Instruction> getDisassemblyInstructions() { return entries("disassemblyInstructionSet", Instruction::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Remanufacturing that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder assemblyInstructions(List<Instruction> value) { editor.entries("assemblyInstructionSet", value); return this; }
        public Builder disassemblyInstructions(List<Instruction> value) { editor.entries("disassemblyInstructionSet", value); return this; }
        public Remanufacturing build() { return new Remanufacturing(editor.aspect()); }
    }
}
