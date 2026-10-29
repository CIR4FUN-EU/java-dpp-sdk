package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 Repairability aspect. */
public final class Repairability extends CatalogAspectModel {
    Repairability(ProfiledAspectState aspect) { super(aspect); if (!"repairability".equals(aspect.elementId())) throw new IllegalArgumentException("Expected repairability aspect"); }
    public Repairability(DataElement element) { this(new ProfiledAspectState("repairability", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.REPAIRABILITY).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public Boolean getSparePartsAvailable() { return bool("sparePartsAvailable"); }
    public List<Instruction> getRepairInstructions() { return entries("repairInstructionSet", Instruction::new); }
    public List<RepairRecord> getRepairRecords() { return entries("repairRecordSet", RepairRecord::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Repairability that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder sparePartsAvailable(Boolean value) { editor.bool("sparePartsAvailable", value); return this; }
        public Builder repairInstructions(List<Instruction> value) { editor.entries("repairInstructionSet", value); return this; }
        public Builder repairRecords(List<RepairRecord> value) { editor.entries("repairRecordSet", value); return this; }
        public Repairability build() { return new Repairability(editor.aspect()); }
    }
}
