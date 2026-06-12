package dppsdk.core.payload;

/**
 * Transport POJO for {@link dppsdk.core.model.Email}.
 */
public class EmailPayload {

    private String emailAddress;
    private String typeOfEmail;

    public EmailPayload() {}

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getTypeOfEmail() { return typeOfEmail; }
    public void setTypeOfEmail(String typeOfEmail) { this.typeOfEmail = typeOfEmail; }
}


