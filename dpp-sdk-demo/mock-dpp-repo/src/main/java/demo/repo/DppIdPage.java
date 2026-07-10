package demo.repo;

import java.util.List;

/**
 * Mock-local page result for batch DPP id lookup.
 */
record DppIdPage(
        List<String> dppIds,
        String nextCursor
) {
}
