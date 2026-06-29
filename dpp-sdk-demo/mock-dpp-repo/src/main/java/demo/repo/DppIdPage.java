package demo.repo;

import java.util.List;

record DppIdPage(
        List<String> dppIds,
        String nextCursor
) {
}
