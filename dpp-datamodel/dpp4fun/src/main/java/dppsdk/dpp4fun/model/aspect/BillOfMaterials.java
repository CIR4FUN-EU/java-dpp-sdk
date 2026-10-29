package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.MultiValuedDataElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Immutable typed view over the frozen D4.1 BillOfMaterials aspect. */
public final class BillOfMaterials {
    static final List<String> ORDER = List.of("materialSet", "componentSet", "partSet");
    private final ProfiledAspectState aspect;

    public BillOfMaterials(DataElement value) { this(new ProfiledAspectState("billOfMaterials", value)); }

    BillOfMaterials(ProfiledAspectState value) {
        aspect = Objects.requireNonNull(value, "aspect");
        if (!"billOfMaterials".equals(aspect.elementId())) throw new IllegalArgumentException("Expected billOfMaterials aspect");
        getMaterials(); getComponents(); getParts();
    }

    DataElementCollection element() { return aspect.asDataElement(); }
    public DataElement toDataElement() { return aspect.asDataElement(); }
    public List<Material> getMaterials() { return entries("materialSet", Material::new); }
    public List<Component> getComponents() { return entries("componentSet", Component::new); }
    public List<Part> getParts() { return entries("partSet", Part::new); }
    public Builder toBuilder() { return new Builder(aspect); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.BILL_OF_MATERIALS).build()); }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof BillOfMaterials that
                && aspect.asDataElement().equals(that.aspect.asDataElement());
    }
    @Override public int hashCode() { return aspect.asDataElement().hashCode(); }
    @Override public String toString() { return "BillOfMaterials{" + aspect.asDataElement() + '}'; }

    private <T> List<T> entries(String field, Function<DataElement, T> factory) {
        DataElement child = element().getDataElements().stream()
                .filter(value -> field.equals(value.getElementId())).findFirst().orElse(null);
        if (child == null) return List.of();
        if (!(child instanceof MultiValuedDataElement list)) throw new IllegalArgumentException(field + " must be a list");
        List<T> values = new ArrayList<>();
        list.getDataElements().forEach(value -> values.add(factory.apply(value)));
        return List.copyOf(values);
    }

    public static final class Builder {
        private ProfiledAspectState aspect;
        private List<Material> materials;
        private List<Component> components;
        private List<Part> parts;

        private Builder(ProfiledAspectState source) {
            aspect = Objects.requireNonNull(source, "aspect");
            BillOfMaterials view = new BillOfMaterials(source);
            materials = new ArrayList<>(view.getMaterials());
            components = new ArrayList<>(view.getComponents());
            parts = new ArrayList<>(view.getParts());
        }

        public Builder materials(List<Material> values) { materials = copy(values, "materials"); update("materialSet", materials); return this; }
        public Builder components(List<Component> values) { components = copy(values, "components"); update("componentSet", components); return this; }
        public Builder parts(List<Part> values) { parts = copy(values, "parts"); update("partSet", parts); return this; }
        public Builder addMaterial(Material value) { materials.add(Objects.requireNonNull(value, "material")); update("materialSet", materials); return this; }
        public Builder addComponent(Component value) { components.add(Objects.requireNonNull(value, "component")); update("componentSet", components); return this; }
        public Builder addPart(Part value) { parts.add(Objects.requireNonNull(value, "part")); update("partSet", parts); return this; }
        public Builder removeMaterial(Material value) { materials.removeIf(item -> item.sameValues(value)); update("materialSet", materials); return this; }
        public Builder removeComponent(Component value) { components.removeIf(item -> item.sameValues(value)); update("componentSet", components); return this; }
        public Builder removePart(Part value) { parts.removeIf(item -> item.sameValues(value)); update("partSet", parts); return this; }
        public BillOfMaterials build() { return new BillOfMaterials(aspect); }

        private void update(String path, List<?> values) {
            ProfiledAspectState.Builder builder = aspect.toBuilder().replaceList(path);
            for (int index = 0; index < values.size(); index++) {
                Object composite = values.get(index);
                DataElement data;
                if (composite instanceof Material material) data = material.element();
                else if (composite instanceof Component component) data = component.element();
                else if (composite instanceof Part part) data = part.element();
                else throw new IllegalArgumentException("Unsupported bill-of-materials entry");
                builder.importElement(path + "[" + index + "]", data);
            }
            aspect = builder.build();
        }

        private static <T> List<T> copy(List<T> values, String label) {
            if (values == null) return new ArrayList<>();
            if (values.stream().anyMatch(Objects::isNull)) throw new NullPointerException(label + " contains null");
            return new ArrayList<>(values);
        }
    }
}
