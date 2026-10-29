package dppsdk.dpp4fun.model.internal;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import java.net.URI;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import dppsdk.core.model.DataElement;
import dppsdk.core.model.MultiValuedDataElement;

/** Shared immutable mechanics for public, source-specific aspect views. */
public abstract class CatalogAspectModel {
    private final TypedAspectAccess access;
    private final ProfiledAspectState aspect;

    protected CatalogAspectModel(ProfiledAspectState aspect) {
        this.aspect = Objects.requireNonNull(aspect, "aspect");
        access = new TypedAspectAccess(aspect);
    }

    protected final ProfiledAspectState profileState() { return aspect; }
    public final DataElement toDataElement() { return aspect.asDataElement(); }
    @Override public boolean equals(Object other) {
        return this == other || other != null && getClass() == other.getClass()
                && aspect.asDataElement().equals(((CatalogAspectModel) other).aspect.asDataElement());
    }
    @Override public int hashCode() { return aspect.asDataElement().hashCode(); }
    @Override public String toString() { return getClass().getSimpleName() + '{' + aspect.asDataElement() + '}'; }
    protected final String text(String field) { return access.optionalString(field); }
    protected final Boolean bool(String field) { return access.bool(field); }
    protected final Double decimal(String field) { return access.decimal(field); }
    protected final Integer integer(String field) { return access.integer(field); }
    protected final URI uri(String field) { return access.uri(field); }
    protected final <T> List<T> entries(String field, Function<DataElement, T> factory) {
        DataElement value = access.dataElement(field);
        if (value == null) return List.of();
        if (!(value instanceof MultiValuedDataElement multi))
            throw new IllegalArgumentException(field + " must be a repeated D4.1 element");
        List<T> result = new ArrayList<>(multi.getDataElements().size());
        multi.getDataElements().forEach(element -> result.add(factory.apply(element)));
        return List.copyOf(result);
    }

    public static final class Editor {
        private TypedAspectAccess access;
        public Editor(ProfiledAspectState aspect) { access = new TypedAspectAccess(aspect); }
        public Editor text(String field, String value) { access = access.withString(field, value); return this; }
        public Editor bool(String field, Boolean value) { access = access.withBoolean(field, value); return this; }
        public Editor decimal(String field, Double value) { access = access.withDecimal(field, value); return this; }
        public Editor integer(String field, Integer value) { access = access.withInteger(field, value); return this; }
        public Editor uri(String field, URI value) { access = access.withUri(field, value); return this; }
        public Editor remove(String field) { access = access.without(field); return this; }
        public Editor entries(String field, List<? extends TypedCompositeEntry> values) {
            Objects.requireNonNull(values, "values");
            if (values.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException(field + " must not contain null");
            ProfiledAspectState.Builder builder = access.asAspect().toBuilder().replaceList(field);
            for (int index = 0; index < values.size(); index++)
                builder.importElement(field + "[" + index + "]", values.get(index).element());
            access = new TypedAspectAccess(builder.build());
            return this;
        }
        public Editor entry(String field, TypedCompositeEntry value) {
            Objects.requireNonNull(value, "value");
            ProfiledAspectState.Builder builder = access.asAspect().toBuilder();
            builder.importElement(field, value.element());
            access = new TypedAspectAccess(builder.build());
            return this;
        }
        public ProfiledAspectState aspect() { return access.asAspect(); }
    }
}
