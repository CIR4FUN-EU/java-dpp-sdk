package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 Upgradability aspect. */
public final class Upgradability extends CatalogAspectModel {
    Upgradability(ProfiledAspectState aspect) { super(aspect); if (!"upgradability".equals(aspect.elementId())) throw new IllegalArgumentException("Expected upgradability aspect"); }
    public Upgradability(DataElement element) { this(new ProfiledAspectState("upgradability", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.UPGRADABILITY).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public Boolean getUpgradeOptionsAvailable() { return bool("upgradeOptionsAvailable"); }
    public List<Instruction> getUpgradeInstructions() { return entries("upgradeInstructionSet", Instruction::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Upgradability that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder upgradeOptionsAvailable(Boolean value) { editor.bool("upgradeOptionsAvailable", value); return this; }
        public Builder upgradeInstructions(List<Instruction> value) { editor.entries("upgradeInstructionSet", value); return this; }
        public Upgradability build() { return new Upgradability(editor.aspect()); }
    }
}
