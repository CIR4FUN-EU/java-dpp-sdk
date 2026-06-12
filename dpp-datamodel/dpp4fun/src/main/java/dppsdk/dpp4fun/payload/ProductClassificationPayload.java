package dppsdk.dpp4fun.payload;

import java.util.List;

/**
 * Transport POJO for {@link dppsdk.dpp4fun.model.ProductClassification}.
 */
public class ProductClassificationPayload {

    private String sector;
    private String group;
    private String category;
    private String subCategory;
    private List<String> tags;

    public ProductClassificationPayload() {}

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}



