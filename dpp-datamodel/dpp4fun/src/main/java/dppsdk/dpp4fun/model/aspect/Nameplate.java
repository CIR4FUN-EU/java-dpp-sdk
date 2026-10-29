package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/** Immutable typed view of the D4.1 Nameplate aspect. */
public final class Nameplate {
    static final java.util.List<String> ORDER = java.util.List.of("gtinCode", "internalArticleNumber", "batchNumber",
            "manufacturerName", "manufacturerProductDescription", "manufacturerProductDesignation",
            "manufacturerProductFamily", "manufacturerProductRoot", "manufacturerProductOrderSuffix",
            "uriOfManufacturer", "glnOfManufacturer", "hsCodeOfTheWCO", "customsTariffNumber", "brand",
            "productType", "uriOfTheProduct", "productArticleNumberOfSupplier", "nameOfSupplier",
            "supplierProductDescription", "supplierProductDesignation", "supplierProductFamily",
            "supplierProductRoot", "supplierProductOrderSuffix", "supplierProductType", "uriOfSupplier",
            "glnOfSupplier", "addressOfAdditionalLink", "contact");
    private final ProfiledAspectState aspect;
    private final TypedAspectAccess access;

    public Nameplate(DataElement value) { this(new ProfiledAspectState("nameplate", value)); }

    Nameplate(ProfiledAspectState aspect) {
        this.aspect = Objects.requireNonNull(aspect, "aspect");
        if (!"nameplate".equals(aspect.elementId())) throw new IllegalArgumentException("Expected nameplate aspect");
        access = new TypedAspectAccess(aspect);
    }

    public DataElement toDataElement() { return aspect.asDataElement(); }
    DataElementCollection element() { return aspect.asDataElement(); }

    public String getGtinCode() { return access.optionalString("gtinCode"); }
    public String getInternalArticleNumber() { return access.optionalString("internalArticleNumber"); }
    public String getBatchNumber() { return access.optionalString("batchNumber"); }
    public String getManufacturerName() { return access.optionalString("manufacturerName"); }
    public Map<String, String> getManufacturerProductDescription() { return access.optionalLanguages("manufacturerProductDescription"); }
    public Map<String, String> getManufacturerProductDesignation() { return access.optionalLanguages("manufacturerProductDesignation"); }
    public Map<String, String> getManufacturerProductFamily() { return access.optionalLanguages("manufacturerProductFamily"); }
    public Map<String, String> getManufacturerProductRoot() { return access.optionalLanguages("manufacturerProductRoot"); }
    public Map<String, String> getManufacturerProductOrderSuffix() { return access.optionalLanguages("manufacturerProductOrderSuffix"); }
    public URI getUriOfManufacturer() { return access.uri("uriOfManufacturer"); }
    public String getGlnOfManufacturer() { return access.optionalString("glnOfManufacturer"); }
    public String getHsCodeOfTheWCO() { return access.optionalString("hsCodeOfTheWCO"); }
    public String getCustomsTariffNumber() { return access.optionalString("customsTariffNumber"); }
    public String getBrand() { return access.optionalString("brand"); }
    public Map<String, String> getProductType() { return access.optionalLanguages("productType"); }
    public URI getUriOfTheProduct() { return access.uri("uriOfTheProduct"); }
    public String getProductArticleNumberOfSupplier() { return access.optionalString("productArticleNumberOfSupplier"); }
    public String getNameOfSupplier() { return access.optionalString("nameOfSupplier"); }
    public Map<String, String> getSupplierProductDescription() { return access.optionalLanguages("supplierProductDescription"); }
    public Map<String, String> getSupplierProductDesignation() { return access.optionalLanguages("supplierProductDesignation"); }
    public Map<String, String> getSupplierProductFamily() { return access.optionalLanguages("supplierProductFamily"); }
    public Map<String, String> getSupplierProductRoot() { return access.optionalLanguages("supplierProductRoot"); }
    public Map<String, String> getSupplierProductOrderSuffix() { return access.optionalLanguages("supplierProductOrderSuffix"); }
    public Map<String, String> getSupplierProductType() { return access.optionalLanguages("supplierProductType"); }
    public URI getUriOfSupplier() { return access.uri("uriOfSupplier"); }
    public String getGlnOfSupplier() { return access.optionalString("glnOfSupplier"); }
    public URI getAddressOfAdditionalLink() { return access.uri("addressOfAdditionalLink"); }
    public Contact getContact() {
        DataElement value = aspect.asDataElement().getDataElements().stream()
                .filter(element -> "contact".equals(element.getElementId())).findFirst().orElse(null);
        return value == null ? null : new Contact(value);
    }

    public Builder toBuilder() { return new Builder(aspect); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.NAMEPLATE).build()); }

    @Override public boolean equals(Object other) {
        return this == other || other instanceof Nameplate that
                && aspect.asDataElement().equals(that.aspect.asDataElement());
    }
    @Override public int hashCode() { return aspect.asDataElement().hashCode(); }
    @Override public String toString() { return "Nameplate{" + aspect + '}'; }

    public static final class Builder {
        private ProfiledAspectState aspect;

        private Builder(ProfiledAspectState value) { aspect = Objects.requireNonNull(value, "aspect"); }

        public Builder gtinCode(String value) { return string("gtinCode", value); }
        public Builder internalArticleNumber(String value) { return string("internalArticleNumber", value); }
        public Builder batchNumber(String value) { return string("batchNumber", value); }
        public Builder manufacturerName(String value) { return string("manufacturerName", value); }
        public Builder manufacturerProductDescription(Map<String, String> value) { return languages("manufacturerProductDescription", value); }
        public Builder manufacturerProductDesignation(Map<String, String> value) { return languages("manufacturerProductDesignation", value); }
        public Builder manufacturerProductFamily(Map<String, String> value) { return languages("manufacturerProductFamily", value); }
        public Builder manufacturerProductRoot(Map<String, String> value) { return languages("manufacturerProductRoot", value); }
        public Builder manufacturerProductOrderSuffix(Map<String, String> value) { return languages("manufacturerProductOrderSuffix", value); }
        public Builder uriOfManufacturer(URI value) { return uri("uriOfManufacturer", value); }
        public Builder glnOfManufacturer(String value) { return string("glnOfManufacturer", value); }
        public Builder hsCodeOfTheWCO(String value) { return string("hsCodeOfTheWCO", value); }
        public Builder customsTariffNumber(String value) { return string("customsTariffNumber", value); }
        public Builder brand(String value) { return string("brand", value); }
        public Builder productType(Map<String, String> value) { return languages("productType", value); }
        public Builder uriOfTheProduct(URI value) { return uri("uriOfTheProduct", value); }
        public Builder productArticleNumberOfSupplier(String value) { return string("productArticleNumberOfSupplier", value); }
        public Builder nameOfSupplier(String value) { return string("nameOfSupplier", value); }
        public Builder supplierProductDescription(Map<String, String> value) { return languages("supplierProductDescription", value); }
        public Builder supplierProductDesignation(Map<String, String> value) { return languages("supplierProductDesignation", value); }
        public Builder supplierProductFamily(Map<String, String> value) { return languages("supplierProductFamily", value); }
        public Builder supplierProductRoot(Map<String, String> value) { return languages("supplierProductRoot", value); }
        public Builder supplierProductOrderSuffix(Map<String, String> value) { return languages("supplierProductOrderSuffix", value); }
        public Builder supplierProductType(Map<String, String> value) { return languages("supplierProductType", value); }
        public Builder uriOfSupplier(URI value) { return uri("uriOfSupplier", value); }
        public Builder glnOfSupplier(String value) { return string("glnOfSupplier", value); }
        public Builder addressOfAdditionalLink(URI value) { return uri("addressOfAdditionalLink", value); }
        public Builder contact(Contact value) {
            if (value == null) {
                var children = aspect.asDataElement().getDataElements().stream()
                        .filter(element -> !"contact".equals(element.getElementId())).toList();
                aspect = new ProfiledAspectState("nameplate", aspect.asDataElement().toBuilder().dataElements(children).build());
            } else {
                ProfiledAspectState.Builder builder = aspect.toBuilder();
                builder.importElement("contact", value.element());
                aspect = builder.build();
            }
            return this;
        }

        public Nameplate build() { return new Nameplate(aspect); }

        private Builder string(String field, String value) {
            if (value == null) return this;
            ProfiledAspectState.Builder builder = aspect.toBuilder();
            builder.field(field, value);
            aspect = builder.build();
            return this;
        }
        private Builder uri(String field, URI value) { return string(field, value == null ? null : value.toString()); }
        private Builder languages(String field, Map<String, String> value) {
            if (value == null || value.isEmpty()) return this;
            ProfiledAspectState.Builder builder = aspect.toBuilder();
            builder.languages(field, value);
            aspect = builder.build();
            return this;
        }
    }
}
