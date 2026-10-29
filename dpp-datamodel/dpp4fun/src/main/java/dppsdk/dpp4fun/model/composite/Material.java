package dppsdk.dpp4fun.model.composite;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;

import java.util.List;
import java.net.URI;

/** Typed view over one bill-of-materials material entry. */
public final class Material {
    public static final List<String> ORDER = List.of("name", "mandatory", "portion", "reference");
    private final DataElementCollection element;
    public Material(DataElement value) {
        element = ProfileElementSupport.collection(value, "Material");
        ProfileElementSupport.requireUniqueKnownIds(element, ORDER.toArray(String[]::new));
    }
    public DataElementCollection element() { return element; }
    public String getName() { return ProfileElementSupport.string(element, "name"); }
    public Boolean getMandatory() { return optionalBoolean("mandatory"); }
    public Double getPortion() { return ProfileElementSupport.decimal(element, "portion"); }
    public URI getReference() { String value = ProfileElementSupport.string(element, "reference"); return value == null ? null : URI.create(value); }
    public boolean sameValues(Material other) { return other != null && java.util.Objects.equals(getName(), other.getName())
            && java.util.Objects.equals(getMandatory(), other.getMandatory()) && java.util.Objects.equals(getPortion(), other.getPortion())
            && java.util.Objects.equals(getReference(), other.getReference()); }
    public Builder toBuilder() { return new Builder(element); }
    public static Builder builder() { return new Builder(); }
    @Override public boolean equals(Object other) {
        return this == other || other instanceof Material that
                && element.toBuilder().elementId(ProfileElementSupport.ITEM_ID).build()
                .equals(that.element.toBuilder().elementId(ProfileElementSupport.ITEM_ID).build());
    }
    @Override public int hashCode() { return element.toBuilder().elementId(ProfileElementSupport.ITEM_ID).build().hashCode(); }
    @Override public String toString() { return "Material{" + element + '}'; }
    public static final class Builder {
        private DataElementCollection element;
        public Builder() {
            this(ProfileElementSupport.empty(ProfileElementSupport.ITEM_ID));
        }
        private Builder(DataElementCollection value) { element = value; }
        public Builder name(String value) { set("name", value); return this; }
        public Builder mandatory(Boolean value) { set("mandatory", value); return this; }
        public Builder portion(Double value) {
            if (value != null && !Double.isFinite(value)) throw new IllegalArgumentException("portion must be finite");
            set("portion", value); return this;
        }
        public Builder reference(URI value) { set("reference", value == null ? null : value.toString()); return this; }
        public Material build() { return new Material(element); }
        private void set(String id, Object value) { element = ProfileElementSupport.patchScalar(element, id, value, ORDER); }
    }

    private Boolean optionalBoolean(String field) {
        DataElement value = ProfileElementSupport.find(element, field);
        return value == null ? null : ProfileElementSupport.bool(element, field);
    }
}
