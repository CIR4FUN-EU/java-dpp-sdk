package dppsdk.postgres.dpp4fun;

/**
 * Search filters for lightweight Dpp4Fun PostgreSQL projection queries.
 */
public record Dpp4FunSearchCriteria(
        String sector,
        String category,
        String brand,
        String productType,
        String materialName,
        String componentName,
        String partName,
        Integer limit,
        Integer offset
) {
    public int limitOrDefault() {
        return limit == null ? 50 : limit;
    }

    public int offsetOrDefault() {
        return offset == null ? 0 : offset;
    }
}
