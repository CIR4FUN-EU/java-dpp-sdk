package dppsdk.core.model;

import java.util.List;
import java.util.Objects;

/** Ordered language-specific values. */
public final class MultiLanguageDataElement extends DataElement {
    private final List<MultiLanguageValue> values;

    private MultiLanguageDataElement(Builder builder) {
        super(builder.elementId, builder.dictionaryReference);
        this.values = List.copyOf(builder.values);
    }

    public List<MultiLanguageValue> getValues() { return values; }

    public Builder toBuilder() {
        return new Builder().elementId(getElementId())
                .dictionaryReference(getDictionaryReference()).values(values);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && Objects.equals(values, ((MultiLanguageDataElement) other).values);
    }

    @Override
    public int hashCode() { return Objects.hash(super.hashCode(), values); }

    @Override
    public String toString() {
        return "MultiLanguageDataElement{elementId='" + getElementId()
                + "', values=" + values + '}';
    }

    public static final class Builder {
        private String elementId;
        private String dictionaryReference;
        private List<MultiLanguageValue> values = List.of();

        public Builder elementId(String value) { this.elementId = value; return this; }
        public Builder dictionaryReference(String value) { this.dictionaryReference = value; return this; }
        public Builder values(List<MultiLanguageValue> value) {
            this.values = List.copyOf(value);
            return this;
        }
        public MultiLanguageDataElement build() { return new MultiLanguageDataElement(this); }
    }
}
