package dppsdk.core.model;

import java.util.List;
import java.util.Objects;

/** Ordered collection of nested data elements. */
public final class DataElementCollection extends DataElement {
    private final List<DataElement> dataElements;

    private DataElementCollection(Builder builder) {
        super(builder.elementId, builder.dictionaryReference);
        this.dataElements = List.copyOf(builder.dataElements);
    }

    public List<DataElement> getDataElements() { return dataElements; }

    public Builder toBuilder() {
        return new Builder().elementId(getElementId())
                .dictionaryReference(getDictionaryReference())
                .dataElements(dataElements);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && Objects.equals(dataElements, ((DataElementCollection) other).dataElements);
    }

    @Override
    public int hashCode() { return Objects.hash(super.hashCode(), dataElements); }

    @Override
    public String toString() {
        return "DataElementCollection{elementId='" + getElementId()
                + "', dataElements=" + dataElements + '}';
    }

    public static final class Builder {
        private String elementId;
        private String dictionaryReference;
        private List<DataElement> dataElements = List.of();

        public Builder elementId(String value) { this.elementId = value; return this; }
        public Builder dictionaryReference(String value) { this.dictionaryReference = value; return this; }
        public Builder dataElements(List<DataElement> value) {
            this.dataElements = List.copyOf(value);
            return this;
        }
        public DataElementCollection build() { return new DataElementCollection(this); }
    }
}
