package dppsdk.core.payload;

/**
 * Transport POJO for {@link dppsdk.core.model.Address}.
 */
public class AddressPayload {

    private String country;
    private String zipCode;
    private String region;
    private String town;
    private String street;

    public AddressPayload() {}

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getTown() { return town; }
    public void setTown(String town) { this.town = town; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
}


