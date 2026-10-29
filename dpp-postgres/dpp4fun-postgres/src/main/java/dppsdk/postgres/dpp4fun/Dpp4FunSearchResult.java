package dppsdk.postgres.dpp4fun;

/**
 * Lightweight projection returned by Dpp4Fun PostgreSQL search queries.
 */
public record Dpp4FunSearchResult(
        String dppId,
        String productId,
        long versionNo,
        String brand
) {
}
