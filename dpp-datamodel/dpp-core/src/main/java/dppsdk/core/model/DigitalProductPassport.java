package dppsdk.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import dppsdk.core.semantic.SemanticPath;

/** EN-oriented passport root. Its header is an SDK grouping, not a separate EN semantic class. */
public final class DigitalProductPassport {
    private final DppHeader dppHeader;
    private final List<DataElement> dataElements;

    private DigitalProductPassport(Builder builder) {
        this.dppHeader = Objects.requireNonNull(builder.dppHeader, "dppHeader is required");
        this.dataElements = List.copyOf(builder.dataElements);
    }

    public DppHeader getDppHeader() { return dppHeader; }
    public List<DataElement> getDataElements() { return dataElements; }

    /**
     * Finds a nested element by element identifiers and/or child-list indexes.
     * This is a canonical-tree lookup, not an RFC 9535 JSONPath evaluator.
     */
    public Optional<DataElement> findDataElement(SemanticPath path) {
        Objects.requireNonNull(path, "path");
        DataElement current = dataElements.stream()
                .filter(element -> path.segments().get(0).equals(element.getElementId()))
                .findFirst().orElse(null);
        for (int i = 1; current != null && i < path.segments().size(); i++) {
            List<DataElement> children;
            if (current instanceof DataElementCollection collection) {
                children = collection.getDataElements();
            } else if (current instanceof MultiValuedDataElement multiValued) {
                children = multiValued.getDataElements();
            } else {
                return Optional.empty();
            }
            String segment = path.segments().get(i);
            Integer index = childIndex(segment);
            if (index != null) {
                current = index < children.size() ? children.get(index) : null;
            } else {
                current = children.stream().filter(element -> segment.equals(element.getElementId()))
                        .findFirst().orElse(null);
            }
        }
        return Optional.ofNullable(current);
    }

    private static Integer childIndex(String segment) {
        if (segment.isEmpty() || !segment.chars().allMatch(Character::isDigit)) return null;
        try {
            return Integer.valueOf(segment);
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    public Builder toBuilder() {
        return new Builder().dppHeader(dppHeader).dataElements(dataElements);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        DigitalProductPassport that = (DigitalProductPassport) other;
        return Objects.equals(dppHeader, that.dppHeader)
                && Objects.equals(dataElements, that.dataElements);
    }

    @Override
    public int hashCode() { return Objects.hash(dppHeader, dataElements); }

    @Override
    public String toString() {
        return "DigitalProductPassport{dppHeader=" + dppHeader
                + ", dataElements=" + dataElements + '}';
    }

    public static final class Builder {
        private DppHeader dppHeader;
        private List<DataElement> dataElements = List.of();

        public Builder dppHeader(DppHeader value) { this.dppHeader = value; return this; }
        public Builder dataElements(List<DataElement> value) {
            this.dataElements = List.copyOf(value);
            return this;
        }
        public DigitalProductPassport build() { return new DigitalProductPassport(this); }
    }
}
