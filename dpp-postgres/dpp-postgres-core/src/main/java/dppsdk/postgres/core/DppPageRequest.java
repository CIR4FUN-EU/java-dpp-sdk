package dppsdk.postgres.core;

/**
 * Lightweight cursor-style page request used by PostgreSQL query operations.
 */
public record DppPageRequest(
        String cursor,
        int limit
) {
    public DppPageRequest {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
    }
}
