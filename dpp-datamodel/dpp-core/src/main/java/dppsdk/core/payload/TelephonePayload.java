package dppsdk.core.payload;

/**
 * Transport POJO for {@link dppsdk.core.model.Telephone}.
 */
public class TelephonePayload {

    private String telephoneNumber;
    private String typeOfTelephone;

    public TelephonePayload() {}

    public String getTelephoneNumber() { return telephoneNumber; }
    public void setTelephoneNumber(String telephoneNumber) { this.telephoneNumber = telephoneNumber; }

    public String getTypeOfTelephone() { return typeOfTelephone; }
    public void setTypeOfTelephone(String typeOfTelephone) { this.typeOfTelephone = typeOfTelephone; }
}


