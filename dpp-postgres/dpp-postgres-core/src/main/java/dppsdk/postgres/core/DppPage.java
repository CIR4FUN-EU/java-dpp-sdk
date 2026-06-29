package dppsdk.postgres.core;

import java.util.List;

/**
 * Lightweight cursor-style page result used by PostgreSQL query operations.
 */
public record DppPage<T>(
        List<T> items,
        String nextCursor
) {
    public DppPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
