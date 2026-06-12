package dppsdk.dpp4fun.payload;

/**
 * Transport POJO for {@link dppsdk.dpp4fun.model.Dimensions}.
 */
public class DimensionsPayload {

    private Double width;
    private Double height;
    private Double depth;
    private String unit;

    public DimensionsPayload() {}

    public Double getWidth() { return width; }
    public void setWidth(Double width) { this.width = width; }

    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }

    public Double getDepth() { return depth; }
    public void setDepth(Double depth) { this.depth = depth; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}



