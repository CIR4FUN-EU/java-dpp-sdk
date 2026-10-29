package dppsdk.dpp4fun.model.composite;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;

import java.util.List;

/** Typed view over an organization contact subtree. */
public final class Contact {
    public static final List<String> ORDER = List.of("organization", "address", "email", "telephone");
    private final DataElementCollection element;
    public Contact(DataElement value) {
        element = ProfileElementSupport.collection(value, "Contact");
        ProfileElementSupport.requireUniqueKnownIds(element, ORDER.toArray(String[]::new));
        getOrganization();
        DataElement child = ProfileElementSupport.find(element, "address"); if (child != null) new Address(child);
        child = ProfileElementSupport.find(element, "email"); if (child != null) new Email(child);
        child = ProfileElementSupport.find(element, "telephone"); if (child != null) new Telephone(child);
    }
    public DataElementCollection element() { return element; }
    public String getOrganization() { return ProfileElementSupport.string(element, "organization"); }
    public Address getAddress() { return view("address", Address::new); }
    public Email getEmail() { return view("email", Email::new); }
    public Telephone getTelephone() { return view("telephone", Telephone::new); }
    public static Builder builder() { return new Builder(); }
    public Builder toBuilder() { return new Builder(element); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Contact that && element.equals(that.element); }
    @Override public int hashCode() { return element.hashCode(); }
    @Override public String toString() { return "Contact{" + element + '}'; }
    private <T> T view(String id, java.util.function.Function<DataElement, T> factory) {
        DataElement child = ProfileElementSupport.find(element, id);
        return child == null ? null : factory.apply(child);
    }

    public static final class Builder {
        private DataElementCollection element;
        public Builder() { this(ProfileElementSupport.empty("contact")); }
        private Builder(DataElementCollection value) { element = value; }
        public Builder organization(String value) { element = ProfileElementSupport.patchScalar(element, "organization", value, ORDER); return this; }
        public Builder address(Address value) { return group("address", value == null ? null : value.element(), Address.ORDER); }
        public Builder email(Email value) { return group("email", value == null ? null : value.element(), Email.ORDER); }
        public Builder telephone(Telephone value) { return group("telephone", value == null ? null : value.element(), Telephone.ORDER); }
        public Contact build() { return new Contact(element); }
        private Builder group(String id, DataElementCollection value, List<String> nestedOrder) {
            if (value == null) element = ProfileElementSupport.removeNestedKnown(element, id, nestedOrder, ORDER);
            else element = ProfileElementSupport.patchCollection(element, id, value, ORDER);
            return this;
        }
    }
}
