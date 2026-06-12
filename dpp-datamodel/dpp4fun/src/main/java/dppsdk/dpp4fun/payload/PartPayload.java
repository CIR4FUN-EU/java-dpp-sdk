package dppsdk.dpp4fun.payload;

/**
 * Transport POJO for {@link dppsdk.dpp4fun.model.Part}.
 */
public class PartPayload {

    private String name;
    private boolean mandatory;
    private String reference;

    public PartPayload() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}



