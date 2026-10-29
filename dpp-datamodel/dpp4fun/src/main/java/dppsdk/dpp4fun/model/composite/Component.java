package dppsdk.dpp4fun.model.composite;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;

import java.util.List;
import java.net.URI;

/** Typed view over one bill-of-materials component entry. */
public final class Component {
    public static final List<String> ORDER = List.of("name", "mandatory", "reference");
    private final DataElementCollection element;
    public Component(DataElement value) { element = ProfileElementSupport.collection(value, "Component"); ProfileElementSupport.requireUniqueKnownIds(element, ORDER.toArray(String[]::new)); }
    public DataElementCollection element() { return element; }
    public String getName() { return ProfileElementSupport.string(element, "name"); }
    public Boolean getMandatory() {
        return ProfileElementSupport.find(element, "mandatory") == null ? null : ProfileElementSupport.bool(element, "mandatory");
    }
    public URI getReference() { String value = ProfileElementSupport.string(element, "reference"); return value == null ? null : URI.create(value); }
    public boolean sameValues(Component other) { return other != null && java.util.Objects.equals(getName(), other.getName())
            && java.util.Objects.equals(getMandatory(), other.getMandatory())
            && java.util.Objects.equals(getReference(), other.getReference()); }
    public Builder toBuilder() { return new Builder(element); }
    public static Builder builder() { return new Builder(); }
    @Override public boolean equals(Object other) {
        return this == other || other instanceof Component that
                && element.toBuilder().elementId(ProfileElementSupport.ITEM_ID).build()
                .equals(that.element.toBuilder().elementId(ProfileElementSupport.ITEM_ID).build());
    }
    @Override public int hashCode() { return element.toBuilder().elementId(ProfileElementSupport.ITEM_ID).build().hashCode(); }
    @Override public String toString() { return "Component{" + element + '}'; }
    public static final class Builder {
        private DataElementCollection element;
        public Builder() { this(ProfileElementSupport.empty(ProfileElementSupport.ITEM_ID)); }
        private Builder(DataElementCollection value) { element = value; }
        public Builder name(String value) { set("name", value); return this; }
        public Builder mandatory(Boolean value) { element = ProfileElementSupport.patchScalar(element, "mandatory", value, ORDER); return this; }
        public Builder reference(URI value) { set("reference", value == null ? null : value.toString()); return this; }
        public Component build() { return new Component(element); }
        private void set(String id, String value) { element = ProfileElementSupport.patchScalar(element, id, value, ORDER); }
    }
}
