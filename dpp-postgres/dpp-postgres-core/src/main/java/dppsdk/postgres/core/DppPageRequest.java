package dppsdk.postgres.core;

/**
 * Lightweight cursor-style page request used by PostgreSQL query operations.
 */
public record DppPageRequest(
        String cursor,
        int limit
) {
    public DppPageRequest {
        if (cursor != null) {
            cursor = cursor.isBlank() ? null : cursor;
            if (cursor != null) {
                try {
                    if (Integer.parseInt(cursor) < 0) {
                        throw new IllegalArgumentException("cursor must be a non-negative integer");
                    }
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("cursor must be a non-negative integer", exception);
                }
            }
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
    }
}
