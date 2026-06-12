package dppsdk.dpp4fun.payload;

/**
 * Transport POJO for {@link dppsdk.dpp4fun.model.Component}.
 */
public class ComponentPayload {

    private String name;
    private String reference;

    public ComponentPayload() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}



