package dppsdk.dpp4fun.model.composite;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.SingleValuedDataElement;

import java.util.List;

/** Immutable permission and prohibition lists from D4.1 RightOfEndUsers. */
public final class Policies extends TypedCompositeEntry {
    public static final List<String> ORDER = List.of("permissionSet", "prohibitionSet");

    public Policies(DataElement value) { super(value); }
    DataElementCollection dataElement() { return element(); }
    public List<String> getPermissions() { return strings("permissionSet", "permission_"); }
    public List<String> getProhibitions() { return strings("prohibitionSet", "prohibition_"); }
    public Builder toBuilder() { return new Builder(element()); }
    public static Builder builder() { return new Builder(ProfileElementSupport.empty("policies")); }

    public static final class Builder {
        private DataElementCollection element;
        private Builder(DataElementCollection element) { this.element = element; }
        public Builder permissions(List<String> values) { set("permissionSet", "permission", values); return this; }
        public Builder prohibitions(List<String> values) { set("prohibitionSet", "prohibition", values); return this; }
        public Policies build() { return new Policies(element); }
        private void set(String field, String stem, List<String> values) {
            List<String> copy = values == null ? List.of() : List.copyOf(values);
            var current = ProfileElementSupport.find(element, field);
            List<dppsdk.core.model.DataElement> members = current instanceof MultiValuedDataElement multi
                    ? multi.getDataElements().stream().filter(child -> !child.getElementId().startsWith(stem + "_")).toList()
                    : List.of();
            for (int index = 0; index < copy.size(); index++)
                members = append(members, new SingleValuedDataElement.Builder()
                        .elementId(String.format(java.util.Locale.ROOT, "%s_%02d", stem, index))
                        .value(copy.get(index)).build());
            MultiValuedDataElement replacement = members.isEmpty() ? null
                    : ProfileElementSupport.multi(field, members);
            element = ProfileElementSupport.patch(element, field, replacement, ORDER);
        }
        private static List<dppsdk.core.model.DataElement> append(List<dppsdk.core.model.DataElement> values,
                dppsdk.core.model.DataElement value) {
            java.util.ArrayList<dppsdk.core.model.DataElement> result = new java.util.ArrayList<>(values);
            result.add(value);
            return result;
        }
    }

    private List<String> strings(String field, String idPrefix) {
        var value = ProfileElementSupport.find(element(), field);
        if (value == null) return List.of();
        if (!(value instanceof MultiValuedDataElement multi)) throw new IllegalArgumentException(field + " must be a list");
        return multi.getDataElements().stream().filter(child -> child.getElementId().startsWith(idPrefix))
                .map(child -> {
                    if (!(child instanceof SingleValuedDataElement scalar) || !(scalar.getValue() instanceof String text))
                        throw new IllegalArgumentException(field + " must contain text values");
                    return text;
                }).toList();
    }
}
