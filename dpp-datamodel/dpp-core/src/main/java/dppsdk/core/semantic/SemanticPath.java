package dppsdk.core.semantic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable path to a DataElement, expressed as element identifiers and array positions. */
public record SemanticPath(List<String> segments) {
    public SemanticPath {
        Objects.requireNonNull(segments, "segments");
        if (segments.isEmpty()) throw new IllegalArgumentException("semantic path must not be empty");
        segments = List.copyOf(segments);
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                throw new IllegalArgumentException("semantic path segments must not be blank");
            }
        }
    }

    public static SemanticPath of(String first, String... remaining) {
        Objects.requireNonNull(first, "first");
        List<String> segments = new ArrayList<>(remaining.length + 1);
        segments.add(first);
        segments.addAll(List.of(remaining));
        return new SemanticPath(segments);
    }

    public SemanticPath child(String segment) {
        List<String> path = new ArrayList<>(segments);
        path.add(Objects.requireNonNull(segment, "segment"));
        return new SemanticPath(path);
    }
}
