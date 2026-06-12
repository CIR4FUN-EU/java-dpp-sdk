package dppsdk.core.payload;

/**
 * Transport POJO for {@link dppsdk.core.model.Contact}.
 */
public class ContactPayload {

    private String organization;
    private AddressPayload address;
    private EmailPayload email;
    private TelephonePayload telephone;

    public ContactPayload() {}

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public AddressPayload getAddress() { return address; }
    public void setAddress(AddressPayload address) { this.address = address; }

    public EmailPayload getEmail() { return email; }
    public void setEmail(EmailPayload email) { this.email = email; }

    public TelephonePayload getTelephone() { return telephone; }
    public void setTelephone(TelephonePayload telephone) { this.telephone = telephone; }
}


