package dppsdk.postgres.projection;

import dppsdk.core.model.DigitalProductPassport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Named extractor and declared query indexes for one rebuildable current-state projection. */
public final class DppProjectionDefinition {
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");
    private final String name;
    private final Function<DigitalProductPassport, DppProjectionDocument> extractor;
    private final List<DppProjectionIndex> indexes;
    private DppProjectionDefinition(String name, Function<DigitalProductPassport, DppProjectionDocument> extractor, List<DppProjectionIndex> indexes) {
        requireName(name, "projectionName"); this.name = name; this.extractor = extractor; this.indexes = List.copyOf(indexes);
    }
    static void requireName(String name, String label) {
        if (name == null || !SAFE_NAME.matcher(name).matches()) throw new IllegalArgumentException(label + " must be a safe SQL identifier (1-63 ASCII letters, digits, underscore; first character a letter or underscore)");
    }
    public String name() { return name; }
    public DppProjectionDocument project(DigitalProductPassport dpp) { return Objects.requireNonNull(extractor.apply(Objects.requireNonNull(dpp)), "extractor returned null"); }
    public List<DppProjectionIndex> indexes() { return indexes; }
    public static Builder builder(String name) { return new Builder(name); }
    public static final class Builder {
        private final String name;
        private Function<DigitalProductPassport, DppProjectionDocument> extractor;
        private final List<DppProjectionIndex> indexes = new ArrayList<>();
        private final Set<String> declarations = new HashSet<>();
        private Builder(String name) { requireName(name, "projectionName"); this.name = name; }
        public Builder extract(Function<DigitalProductPassport, DppProjectionDocument> value) { extractor = Objects.requireNonNull(value); return this; }
        public Builder btree(String field, DppProjectionValueType type) { add(new DppProjectionIndex(field, DppProjectionIndexType.BTREE, type)); return this; }
        public Builder gin(String field) { add(new DppProjectionIndex(field, DppProjectionIndexType.GIN, null)); return this; }
        private void add(DppProjectionIndex index) {
            String declaration = index.indexType() + ":" + index.fieldName();
            if (!declarations.add(declaration)) throw new IllegalArgumentException("Duplicate index definition: " + declaration);
            indexes.add(index);
        }
        public DppProjectionDefinition build() { if (extractor == null) throw new IllegalStateException("extractor is required"); return new DppProjectionDefinition(name, extractor, indexes); }
    }
}
