package dppsdk.postgres.core;

import java.util.List;

public record DppPage<T>(
        List<T> items,
        String nextCursor
) {
    public DppPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
