package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 Usage aspect. */
public final class Usage extends CatalogAspectModel {
    Usage(ProfiledAspectState aspect) { super(aspect); if (!"usage".equals(aspect.elementId())) throw new IllegalArgumentException("Expected usage aspect"); }
    public Usage(DataElement element) { this(new ProfiledAspectState("usage", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.USAGE).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public Integer getOperatingHours() { return integer("operatingHours"); }
    public ProductFunction getPrimaryFunction() {
        var value = profileState().asDataElement().getDataElements().stream()
                .filter(child -> "primaryFunction".equals(child.getElementId())).findFirst().orElse(null);
        return value == null ? null : new ProductFunction(value);
    }
    public List<ProductFunction> getSecondaryFunctions() { return entries("secondaryFunctionSet", ProductFunction::new); }
    public List<Instruction> getAssemblyInstructions() { return entries("assemblyInstructionSet", Instruction::new); }
    public List<Instruction> getDisassemblyInstructions() { return entries("disassemblyInstructionSet", Instruction::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Usage that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder operatingHours(Integer value) { editor.integer("operatingHours", value); return this; }
        public Builder primaryFunction(ProductFunction value) {
            if (value == null) editor.remove("primaryFunction"); else editor.entry("primaryFunction", value);
            return this;
        }
        public Builder secondaryFunctions(List<ProductFunction> value) { editor.entries("secondaryFunctionSet", value); return this; }
        public Builder assemblyInstructions(List<Instruction> value) { editor.entries("assemblyInstructionSet", value); return this; }
        public Builder disassemblyInstructions(List<Instruction> value) { editor.entries("disassemblyInstructionSet", value); return this; }
        public Usage build() { return new Usage(editor.aspect()); }
    }
}
