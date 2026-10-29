package dppsdk.dpp4fun.model.internal;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;

import java.net.URI;
import java.util.List;

/** Immutable source-backed repeated entry; parent binders assign its serialization ID. */
public abstract class TypedCompositeEntry {
    private final DataElementCollection element;

    public TypedCompositeEntry(DataElement value) {
        element = ProfileElementSupport.collection(value, "repeated D4.1 item");
    }

    public final DataElementCollection element() { return element; }
    protected final String text(String field) { return ProfileElementSupport.string(element, field); }
    protected final Boolean bool(String field) {
        DataElement value = ProfileElementSupport.find(element, field);
        if (value == null) return null;
        return ProfileElementSupport.bool(element, field);
    }
    protected final Double decimal(String field) { return ProfileElementSupport.decimal(element, field); }
    protected final Integer integer(String field) { return ProfileElementSupport.integer(element, field); }
    protected final URI uri(String field) {
        String value = text(field);
        return value == null ? null : URI.create(value);
    }

    @Override public final boolean equals(Object other) {
        return this == other || other != null && getClass() == other.getClass()
                && element.getDataElements().equals(((TypedCompositeEntry) other).element.getDataElements());
    }
    @Override public final int hashCode() { return element.getDataElements().hashCode(); }
    @Override public final String toString() { return getClass().getSimpleName() + element.getDataElements(); }

    public static final class Editor {
        private DataElementCollection element;
        private final List<String> order;
        public Editor(DataElementCollection value, List<String> order) { element = value; this.order = order; }
        public Editor set(String field, Object value) {
            if (value instanceof URI uri) value = uri.toString();
            element = ProfileElementSupport.patchScalar(element, field, value, order);
            return this;
        }
        public Editor strings(String field, List<String> values) {
            List<String> copy = values == null ? List.of() : List.copyOf(values);
            element = ProfileElementSupport.patchStringList(element, field, copy, order);
            return this;
        }
        public Editor editList(String field, List<?> values, String name) {
            if (values == null || values.stream().anyMatch(java.util.Objects::isNull))
                throw new IllegalArgumentException(name + " must not contain null");
            return this;
        }
        public DataElementCollection element() { return element; }
    }
}
