package dpp.repo.payloads;

import java.util.List;

/**
 * Response payload returned when querying DPP identifiers by product identifiers.
 */
public class ReadDppIdsResponse {
    private List<String> dppIdentifiers;
    private String nextCursor;

    public ReadDppIdsResponse() {
    }

    public List<String> getDppIdentifiers() {
        return dppIdentifiers;
    }

    public void setDppIdentifiers(List<String> dppIdentifiers) {
        this.dppIdentifiers = dppIdentifiers;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }
}
