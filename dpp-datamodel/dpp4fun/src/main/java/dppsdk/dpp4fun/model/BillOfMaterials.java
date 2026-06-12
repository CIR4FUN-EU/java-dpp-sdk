package dppsdk.dpp4fun.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the composition of products into materials, components, and parts.
 *
 * Responsibilities:
 * - Aggregates physical makeup of the product
 * - Maintains lists of constituent items
 *
 * Notes:
 * - Immutable value object
 * - Built via Builder
 */
public class BillOfMaterials {

    private final List<Material> materials;
    private final List<Component> components;
    private final List<Part> parts;

    private BillOfMaterials(Builder builder) {
        this.materials = builder.materials != null ? new ArrayList<>(builder.materials) : new ArrayList<>();
        this.components = builder.components != null ? new ArrayList<>(builder.components) : new ArrayList<>();
        this.parts = builder.parts != null ? new ArrayList<>(builder.parts) : new ArrayList<>();
    }

    public List<Material> getMaterials() {
        return new ArrayList<>(materials);
    }

    public List<Component> getComponents() {
        return new ArrayList<>(components);
    }

    public List<Part> getParts() {
        return new ArrayList<>(parts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BillOfMaterials that = (BillOfMaterials) o;
        return Objects.equals(materials, that.materials) &&
                Objects.equals(components, that.components) &&
                Objects.equals(parts, that.parts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(materials, components, parts);
    }

    @Override
    public String toString() {
        return "BillOfMaterials{" +
                "materials=" + materials.size() +
                ", components=" + components.size() +
                ", parts=" + parts.size() +
                '}';
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        if (this.materials != null && !this.materials.isEmpty()) {
            this.materials.forEach(builder::addMaterial);
        }
        if (this.components != null && !this.components.isEmpty()) {
            this.components.forEach(builder::addComponent);
        }
        if (this.parts != null && !this.parts.isEmpty()) {
            this.parts.forEach(builder::addPart);
        }
        return builder;
    }

    public static class Builder {
        private List<Material> materials;
        private List<Component> components;
        private List<Part> parts;

        public Builder materials(List<Material> materials) {
            this.materials = materials;
            return this;
        }

        public Builder addMaterial(Material material) {
            if (this.materials == null) {
                this.materials = new ArrayList<>();
            }
            this.materials.add(material);
            return this;
        }

        public Builder components(List<Component> components) {
            this.components = components;
            return this;
        }

        public Builder addComponent(Component component) {
            if (this.components == null) {
                this.components = new ArrayList<>();
            }
            this.components.add(component);
            return this;
        }

        public Builder parts(List<Part> parts) {
            this.parts = parts;
            return this;
        }

        public Builder addPart(Part part) {
            if (this.parts == null) {
                this.parts = new ArrayList<>();
            }
            this.parts.add(part);
            return this;
        }

        public Builder removeMaterial(Material material) {
            if (this.materials != null) {
                this.materials.remove(material);
            }
            return this;
        }

        public Builder removeComponent(Component component) {
            if (this.components != null) {
                this.components.remove(component);
            }
            return this;
        }

        public Builder removePart(Part part) {
            if (this.parts != null) {
                this.parts.remove(part);
            }
            return this;
        }

        public BillOfMaterials build() {
            return new BillOfMaterials(this);
        }
    }
}



