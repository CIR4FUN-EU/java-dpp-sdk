package dppsdk.core.model;

import java.util.Objects;

/** Common identity attributes of EN-oriented data elements. */
public abstract class DataElement {
    private final String elementId;
    private final String dictionaryReference;

    protected DataElement(String elementId, String dictionaryReference) {
        this.elementId = elementId;
        this.dictionaryReference = dictionaryReference;
    }

    public String getElementId() {
        return elementId;
    }

    public String getDictionaryReference() {
        return dictionaryReference;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        DataElement that = (DataElement) other;
        return Objects.equals(elementId, that.elementId)
                && Objects.equals(dictionaryReference, that.dictionaryReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementId, dictionaryReference);
    }
}
