package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 WasteManagement aspect. */
public final class WasteManagement extends CatalogAspectModel {
    WasteManagement(ProfiledAspectState aspect) { super(aspect); if (!"wasteManagement".equals(aspect.elementId())) throw new IllegalArgumentException("Expected wasteManagement aspect"); }
    public WasteManagement(DataElement element) { this(new ProfiledAspectState("wasteManagement", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.WASTE_MANAGEMENT).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public Boolean getToxicityRisk() { return bool("toxicityRisk"); }
    public Boolean getFireRisk() { return bool("fireRisk"); }
    public Boolean getElectricityRisk() { return bool("electricityRisk"); }
    public List<Instruction> getDisposalInstructions() { return entries("disposalInstructionSet", Instruction::new); }
    public List<Instruction> getDisassemblyInstructions() { return entries("disassemblyInstructionSet", Instruction::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof WasteManagement that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder toxicityRisk(Boolean value) { editor.bool("toxicityRisk", value); return this; }
        public Builder fireRisk(Boolean value) { editor.bool("fireRisk", value); return this; }
        public Builder electricityRisk(Boolean value) { editor.bool("electricityRisk", value); return this; }
        public Builder disposalInstructions(List<Instruction> value) { editor.entries("disposalInstructionSet", value); return this; }
        public Builder disassemblyInstructions(List<Instruction> value) { editor.entries("disassemblyInstructionSet", value); return this; }
        public WasteManagement build() { return new WasteManagement(editor.aspect()); }
    }
}
