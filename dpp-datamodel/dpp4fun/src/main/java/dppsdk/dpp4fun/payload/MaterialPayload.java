package dppsdk.dpp4fun.payload;

/**
 * Transport POJO for {@link dppsdk.dpp4fun.model.Material}.
 */
public class MaterialPayload {

    private String name;
    private boolean mandatory;
    private double portion;
    private String reference;

    public MaterialPayload() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public double getPortion() { return portion; }
    public void setPortion(double portion) { this.portion = portion; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}



