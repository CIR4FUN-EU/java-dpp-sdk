package dppsdk.postgres.dpp4fun;

public record Dpp4FunSearchResult(
        String dppId,
        String productId,
        long versionNo,
        String sector,
        String category,
        String brand,
        String productType,
        String productName
) {
}
