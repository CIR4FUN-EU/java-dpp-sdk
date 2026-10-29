package dppsdk.dpp4fun.model.composite;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;

import java.util.List;

/** Typed view over an email communication subtree. */
public final class Email {
    public static final List<String> ORDER = List.of("emailAddress", "typeOfEmail");
    private final DataElementCollection element;
    public Email(DataElement value) {
        element = ProfileElementSupport.collection(value, "Email");
        ProfileElementSupport.requireUniqueKnownIds(element, ORDER.toArray(String[]::new));
        getEmailAddress(); getTypeOfEmail();
    }
    public DataElementCollection element() { return element; }
    public String getEmailAddress() { return ProfileElementSupport.string(element, "emailAddress"); }
    public String getTypeOfEmail() { return ProfileElementSupport.string(element, "typeOfEmail"); }
    public static Builder builder() { return new Builder(); }
    public Builder toBuilder() { return new Builder(element); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Email that && element.equals(that.element); }
    @Override public int hashCode() { return element.hashCode(); }
    @Override public String toString() { return "Email{" + element + '}'; }
    public static final class Builder {
        private DataElementCollection element;
        public Builder() { this(ProfileElementSupport.empty("email")); }
        private Builder(DataElementCollection value) { element = value; }
        public Builder emailAddress(String value) { set("emailAddress", value); return this; }
        public Builder typeOfEmail(String value) { set("typeOfEmail", value); return this; }
        public Email build() { return new Email(element); }
        private void set(String id, String value) { element = ProfileElementSupport.patchScalar(element, id, value, ORDER); }
    }
}
