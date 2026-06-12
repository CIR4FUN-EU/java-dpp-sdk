package dppsdk.core.payload;

/**
 * Transport POJO for {@link dppsdk.core.model.Organization}.
 *
 * The {@code role} field is represented as a String (not the enum)
 * for maximum transport flexibility. The mapper converts between
 * {@link dppsdk.core.model.OrganizationRole} enum and String.
 */
public class OrganizationPayload {

    private String name;
    private String gln;
    private String productDescription;
    private String productDesignation;
    private String productFamily;
    private String productRoot;
    private String productOrderSuffix;
    private String uri;
    private ContactPayload contact;
    private String role;

    public OrganizationPayload() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGln() { return gln; }
    public void setGln(String gln) { this.gln = gln; }

    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }

    public String getProductDesignation() { return productDesignation; }
    public void setProductDesignation(String productDesignation) { this.productDesignation = productDesignation; }

    public String getProductFamily() { return productFamily; }
    public void setProductFamily(String productFamily) { this.productFamily = productFamily; }

    public String getProductRoot() { return productRoot; }
    public void setProductRoot(String productRoot) { this.productRoot = productRoot; }

    public String getProductOrderSuffix() { return productOrderSuffix; }
    public void setProductOrderSuffix(String productOrderSuffix) { this.productOrderSuffix = productOrderSuffix; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public ContactPayload getContact() { return contact; }
    public void setContact(ContactPayload contact) { this.contact = contact; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}


