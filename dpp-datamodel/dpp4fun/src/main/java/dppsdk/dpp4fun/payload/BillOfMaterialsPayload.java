package dppsdk.dpp4fun.payload;

import java.util.List;

/**
 * Transport POJO for {@link dppsdk.dpp4fun.model.BillOfMaterials}.
 */
public class BillOfMaterialsPayload {

    private List<MaterialPayload> materials;
    private List<ComponentPayload> components;
    private List<PartPayload> parts;

    public BillOfMaterialsPayload() {}

    public List<MaterialPayload> getMaterials() { return materials; }
    public void setMaterials(List<MaterialPayload> materials) { this.materials = materials; }

    public List<ComponentPayload> getComponents() { return components; }
    public void setComponents(List<ComponentPayload> components) { this.components = components; }

    public List<PartPayload> getParts() { return parts; }
    public void setParts(List<PartPayload> parts) { this.parts = parts; }
}



