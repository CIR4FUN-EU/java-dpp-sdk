package dppsdk.core.semantic;

import java.util.Objects;
import java.util.Optional;

/** Fixed semantic metadata that a resolver can recover for one path. */
public record SemanticDefinition(
        Optional<DataElementKind> kind,
        Optional<String> elementId,
        Optional<String> dictionaryReference,
        Optional<String> valueDataType,
        boolean dictionaryReferenceResolved,
        boolean valueDataTypeResolved) {
    public SemanticDefinition(Optional<DataElementKind> kind, Optional<String> elementId,
            Optional<String> dictionaryReference, Optional<String> valueDataType) {
        this(kind, elementId, dictionaryReference, valueDataType,
                dictionaryReference.isPresent(), valueDataType.isPresent());
    }

    public SemanticDefinition {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(elementId, "elementId");
        Objects.requireNonNull(dictionaryReference, "dictionaryReference");
        Objects.requireNonNull(valueDataType, "valueDataType");
        if (dictionaryReference.isPresent() && !dictionaryReferenceResolved
                || valueDataType.isPresent() && !valueDataTypeResolved) {
            throw new IllegalArgumentException("supplied semantic metadata must be marked resolved");
        }
        elementId.ifPresent(value -> requireNonBlank(value, "elementId"));
        dictionaryReference.ifPresent(value -> requireNonBlank(value, "dictionaryReference"));
        valueDataType.ifPresent(value -> requireNonBlank(value, "valueDataType"));
    }

    private static void requireNonBlank(String value, String name) {
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    }
}
