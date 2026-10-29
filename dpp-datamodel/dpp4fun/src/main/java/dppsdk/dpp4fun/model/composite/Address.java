package dppsdk.dpp4fun.model.composite;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;

import java.util.List;

/** Typed view over an address element subtree. */
public final class Address {
    public static final List<String> ORDER = List.of("country", "zipCode", "region", "town", "street");
    private final DataElementCollection element;

    public Address(DataElement value) {
        element = ProfileElementSupport.collection(value, "Address");
        ProfileElementSupport.requireUniqueKnownIds(element, ORDER.toArray(String[]::new));
        getCountry(); getZipCode(); getRegion(); getTown(); getStreet();
    }

    public DataElementCollection element() { return element; }
    public String getCountry() { return ProfileElementSupport.string(element, "country"); }
    public String getZipCode() { return ProfileElementSupport.string(element, "zipCode"); }
    public String getRegion() { return ProfileElementSupport.string(element, "region"); }
    public String getTown() { return ProfileElementSupport.string(element, "town"); }
    public String getStreet() { return ProfileElementSupport.string(element, "street"); }
    public static Builder builder() { return new Builder(); }
    public Builder toBuilder() { return new Builder(element); }

    @Override public boolean equals(Object other) { return this == other || other instanceof Address that && element.equals(that.element); }
    @Override public int hashCode() { return element.hashCode(); }
    @Override public String toString() { return "Address{" + element + '}'; }

    public static final class Builder {
        private DataElementCollection element;
        public Builder() { this(ProfileElementSupport.empty("address")); }
        private Builder(DataElementCollection value) { element = value; }
        public Builder country(String value) { set("country", value); return this; }
        public Builder zipCode(String value) { set("zipCode", value); return this; }
        public Builder region(String value) { set("region", value); return this; }
        public Builder town(String value) { set("town", value); return this; }
        public Builder street(String value) { set("street", value); return this; }
        public Address build() { return new Address(element); }
        private void set(String id, String value) { element = ProfileElementSupport.patchScalar(element, id, value, ORDER); }
    }
}
