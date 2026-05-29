package dppsdk.core.payload;

/**
 * Transport POJO for {@link dppsdk.core.model.Nameplate}.
 */
public class NameplatePayload {

    private String gtinCode;
    private String internalArticleNumber;
    private String batchNumber;
    private String customsTariffNumber;
    private String uriOfTheProduct;
    private OrganizationPayload manufacturer;
    private OrganizationPayload supplier;

    public NameplatePayload() {}

    public String getGtinCode() { return gtinCode; }
    public void setGtinCode(String gtinCode) { this.gtinCode = gtinCode; }

    public String getInternalArticleNumber() { return internalArticleNumber; }
    public void setInternalArticleNumber(String internalArticleNumber) { this.internalArticleNumber = internalArticleNumber; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getCustomsTariffNumber() { return customsTariffNumber; }
    public void setCustomsTariffNumber(String customsTariffNumber) { this.customsTariffNumber = customsTariffNumber; }

    public String getUriOfTheProduct() { return uriOfTheProduct; }
    public void setUriOfTheProduct(String uriOfTheProduct) { this.uriOfTheProduct = uriOfTheProduct; }

    public OrganizationPayload getManufacturer() { return manufacturer; }
    public void setManufacturer(OrganizationPayload manufacturer) { this.manufacturer = manufacturer; }

    public OrganizationPayload getSupplier() { return supplier; }
    public void setSupplier(OrganizationPayload supplier) { this.supplier = supplier; }
}


