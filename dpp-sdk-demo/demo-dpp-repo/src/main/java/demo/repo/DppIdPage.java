package demo.repo;

import java.util.List;

/**
 * Demo-local page result for batch DPP id lookup.
 */
record DppIdPage(
        List<String> dppIds,
        String nextCursor
) {
}
