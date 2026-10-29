package dppsdk.postgres.projection;

import java.util.Objects;

public record DppProjectionIndex(String fieldName, DppProjectionIndexType indexType, DppProjectionValueType valueType) {
    public DppProjectionIndex {
        DppProjectionDefinition.requireName(fieldName, "fieldName");
        Objects.requireNonNull(indexType, "indexType must not be null");
        if (indexType == DppProjectionIndexType.BTREE) Objects.requireNonNull(valueType, "B-tree valueType must not be null");
        if (indexType == DppProjectionIndexType.GIN && valueType != null) throw new IllegalArgumentException("GIN indexes do not have a scalar value type");
    }
}
