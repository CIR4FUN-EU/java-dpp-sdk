package dppsdk.dpp4fun.payload;

import java.util.List;

/**
 * Transport POJO for {@link dppsdk.dpp4fun.model.Characteristics}.
 */
public class CharacteristicsPayload {

    private String productName;
    private String description;
    private String brand;
    private String productType;
    private DimensionsPayload dimensions;
    private Double weight;
    private String color;
    private List<String> features;

    public CharacteristicsPayload() {}

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public DimensionsPayload getDimensions() { return dimensions; }
    public void setDimensions(DimensionsPayload dimensions) { this.dimensions = dimensions; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }
}



