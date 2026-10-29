package dppsdk.postgres.projection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Small immutable JSON-safe query document; it is never authoritative DPP state. */
public final class DppProjectionDocument {
    private final Map<String, Object> values;

    private DppProjectionDocument(Map<String, Object> values) { this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values)); }
    public Map<String, Object> values() { return values; }
    public Object get(String name) { return values.get(name); }
    public static Builder builder() { return new Builder(); }

    private static Object copyJsonValue(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean) return value;
        if (value instanceof Number number) {
            if (number instanceof Double d && !Double.isFinite(d) || number instanceof Float f && !Float.isFinite(f))
                throw new IllegalArgumentException("Projection numbers must be finite");
            return number instanceof BigDecimal ? number : new BigDecimal(number.toString());
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item instanceof List<?> || item instanceof Map<?, ?>) throw new IllegalArgumentException("Nested projection values are not supported");
                copy.add(copyJsonValue(item));
            }
            return Collections.unmodifiableList(copy);
        }
        throw new IllegalArgumentException("Unsupported projection value type: " + value.getClass().getName());
    }

    public static final class Builder {
        private final Map<String, Object> values = new LinkedHashMap<>();
        public Builder put(String name, Object value) {
            DppProjectionDefinition.requireName(name, "fieldName");
            if (values.containsKey(name)) throw new IllegalArgumentException("Duplicate projection field: " + name);
            values.put(name, copyJsonValue(value));
            return this;
        }
        public Builder text(String name, String value) { return put(name, value); }
        public Builder number(String name, Number value) { return put(name, value); }
        public Builder bool(String name, Boolean value) { return put(name, value); }
        public Builder strings(String name, List<String> value) { return put(name, Objects.requireNonNull(value, "value must not be null")); }
        public DppProjectionDocument build() { return new DppProjectionDocument(values); }
    }
}
