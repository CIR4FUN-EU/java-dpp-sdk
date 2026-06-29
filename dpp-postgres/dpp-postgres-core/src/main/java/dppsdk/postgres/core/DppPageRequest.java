package dppsdk.postgres.core;

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
