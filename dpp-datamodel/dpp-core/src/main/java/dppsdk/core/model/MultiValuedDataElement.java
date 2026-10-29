package dppsdk.core.model;

import java.util.List;
import java.util.Objects;

/** Ordered values expressed as data elements of the declared value data type. */
public final class MultiValuedDataElement extends DataElement {
    private final String valueDataType;
    private final List<DataElement> dataElements;

    private MultiValuedDataElement(Builder builder) {
        super(builder.elementId, builder.dictionaryReference);
        this.valueDataType = builder.valueDataType;
        this.dataElements = List.copyOf(builder.dataElements);
    }

    public String getValueDataType() { return valueDataType; }
    public List<DataElement> getDataElements() { return dataElements; }

    public Builder toBuilder() {
        return new Builder().elementId(getElementId())
                .dictionaryReference(getDictionaryReference())
                .valueDataType(valueDataType).dataElements(dataElements);
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) return false;
        MultiValuedDataElement that = (MultiValuedDataElement) other;
        return Objects.equals(valueDataType, that.valueDataType)
                && Objects.equals(dataElements, that.dataElements);
    }

    @Override
    public int hashCode() { return Objects.hash(super.hashCode(), valueDataType, dataElements); }

    @Override
    public String toString() {
        return "MultiValuedDataElement{elementId='" + getElementId()
                + "', valueDataType='" + valueDataType
                + "', dataElements=" + dataElements + '}';
    }

    public static final class Builder {
        private String elementId;
        private String dictionaryReference;
        private String valueDataType;
        private List<DataElement> dataElements = List.of();

        public Builder elementId(String value) { this.elementId = value; return this; }
        public Builder dictionaryReference(String value) { this.dictionaryReference = value; return this; }
        public Builder valueDataType(String value) { this.valueDataType = value; return this; }
        public Builder dataElements(List<DataElement> value) {
            this.dataElements = List.copyOf(value);
            return this;
        }
        public MultiValuedDataElement build() { return new MultiValuedDataElement(this); }
    }
}
