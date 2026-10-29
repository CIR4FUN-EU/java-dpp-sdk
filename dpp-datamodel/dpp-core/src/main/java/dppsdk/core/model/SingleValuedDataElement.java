package dppsdk.core.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A single JSON-compatible value without coupling the domain to a JSON library. */
public final class SingleValuedDataElement extends DataElement {
    private final Object value;
    private final String valueDataType;

    private SingleValuedDataElement(Builder builder) {
        super(builder.elementId, builder.dictionaryReference);
        this.value = freezeValue(builder.value);
        this.valueDataType = builder.valueDataType;
    }

    public Object getValue() { return value; }
    public String getValueDataType() { return valueDataType; }

    public Builder toBuilder() {
        return new Builder().elementId(getElementId())
                .dictionaryReference(getDictionaryReference())
                .value(value).valueDataType(valueDataType);
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) return false;
        SingleValuedDataElement that = (SingleValuedDataElement) other;
        return valuesEqual(value, that.value)
                && Objects.equals(valueDataType, that.valueDataType);
    }

    @Override
    public int hashCode() { return Objects.hash(super.hashCode(), valueHash(value), valueDataType); }

    @Override
    public String toString() {
        return "SingleValuedDataElement{elementId='" + getElementId()
                + "', value=" + value + ", valueDataType='" + valueDataType + "'}";
    }

    private static Object freezeValue(Object source) {
        return freezeValue(source, new IdentityHashMap<>());
    }

    private static Object freezeValue(Object source, IdentityHashMap<Object, Boolean> visiting) {
        if (source == null || source instanceof String || source instanceof Boolean
                || source instanceof Byte || source instanceof Short
                || source instanceof Integer || source instanceof Long
                || source instanceof BigInteger || source instanceof BigDecimal) {
            return source;
        }
        if (source instanceof Float number) {
            if (!Float.isFinite(number)) throw new IllegalArgumentException("value must be JSON-compatible");
            return number;
        }
        if (source instanceof Double number) {
            if (!Double.isFinite(number)) throw new IllegalArgumentException("value must be JSON-compatible");
            return number;
        }
        if (source instanceof List<?> list) {
            if (visiting.put(source, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("value must not contain cycles");
            }
            try {
                List<Object> copy = new ArrayList<>(list.size());
                for (Object item : list) copy.add(freezeValue(item, visiting));
                return Collections.unmodifiableList(copy);
            } finally {
                visiting.remove(source);
            }
        }
        if (source instanceof Map<?, ?> map) {
            if (visiting.put(source, Boolean.TRUE) != null) {
                throw new IllegalArgumentException("value must not contain cycles");
            }
            try {
                Map<String, Object> copy = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        throw new IllegalArgumentException("value object keys must be strings");
                    }
                    copy.put(key, freezeValue(entry.getValue(), visiting));
                }
                return Collections.unmodifiableMap(copy);
            } finally {
                visiting.remove(source);
            }
        }
        throw new IllegalArgumentException("value must be JSON-compatible");
    }

    private static boolean valuesEqual(Object left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
            return decimal(leftNumber).compareTo(decimal(rightNumber)) == 0;
        }
        if (left instanceof List<?> leftList && right instanceof List<?> rightList) {
            if (leftList.size() != rightList.size()) return false;
            for (int i = 0; i < leftList.size(); i++) {
                if (!valuesEqual(leftList.get(i), rightList.get(i))) return false;
            }
            return true;
        }
        if (left instanceof Map<?, ?> leftMap && right instanceof Map<?, ?> rightMap) {
            if (!leftMap.keySet().equals(rightMap.keySet())) return false;
            for (Object key : leftMap.keySet()) {
                if (!valuesEqual(leftMap.get(key), rightMap.get(key))) return false;
            }
            return true;
        }
        return Objects.equals(left, right);
    }

    private static int valueHash(Object value) {
        if (value == null) return 0;
        if (value instanceof Number number) return decimal(number).stripTrailingZeros().hashCode();
        if (value instanceof List<?> list) {
            int result = 1;
            for (Object item : list) result = 31 * result + valueHash(item);
            return result;
        }
        if (value instanceof Map<?, ?> map) {
            int result = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result += Objects.hashCode(entry.getKey()) ^ valueHash(entry.getValue());
            }
            return result;
        }
        return value.hashCode();
    }

    private static BigDecimal decimal(Number number) {
        if (number instanceof BigDecimal decimal) return decimal;
        if (number instanceof BigInteger integer) return new BigDecimal(integer);
        return new BigDecimal(number.toString());
    }

    public static final class Builder {
        private String elementId;
        private String dictionaryReference;
        private Object value;
        private String valueDataType;

        public Builder elementId(String value) { this.elementId = value; return this; }
        public Builder dictionaryReference(String value) { this.dictionaryReference = value; return this; }
        public Builder value(Object value) { this.value = value; return this; }
        public Builder valueDataType(String value) { this.valueDataType = value; return this; }
        public SingleValuedDataElement build() { return new SingleValuedDataElement(this); }
    }
}
