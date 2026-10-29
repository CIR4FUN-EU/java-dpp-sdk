package dppsdk.dpp4fun.model.composite;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;

import java.util.List;

/** Typed view over a telephone communication subtree. */
public final class Telephone {
    public static final List<String> ORDER = List.of("telephoneNumber", "typeOfTelephone");
    private final DataElementCollection element;
    public Telephone(DataElement value) {
        element = ProfileElementSupport.collection(value, "Telephone");
        ProfileElementSupport.requireUniqueKnownIds(element, ORDER.toArray(String[]::new));
        getTelephoneNumber(); getTypeOfTelephone();
    }
    public DataElementCollection element() { return element; }
    public String getTelephoneNumber() { return ProfileElementSupport.string(element, "telephoneNumber"); }
    public String getTypeOfTelephone() { return ProfileElementSupport.string(element, "typeOfTelephone"); }
    public static Builder builder() { return new Builder(); }
    public Builder toBuilder() { return new Builder(element); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Telephone that && element.equals(that.element); }
    @Override public int hashCode() { return element.hashCode(); }
    @Override public String toString() { return "Telephone{" + element + '}'; }
    public static final class Builder {
        private DataElementCollection element;
        public Builder() { this(ProfileElementSupport.empty("telephone")); }
        private Builder(DataElementCollection value) { element = value; }
        public Builder telephoneNumber(String value) { set("telephoneNumber", value); return this; }
        public Builder typeOfTelephone(String value) { set("typeOfTelephone", value); return this; }
        public Telephone build() { return new Telephone(element); }
        private void set(String id, String value) { element = ProfileElementSupport.patchScalar(element, id, value, ORDER); }
    }
}
