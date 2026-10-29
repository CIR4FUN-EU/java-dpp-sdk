package dpp.repo.payloads;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

/**
 * Response payload returned when querying DPP identifiers by product identifiers.
 */
public class ReadDppIdsResponse {
    private DppStatusCode statusCode;
    private List<DppApiMessage> message;
    private List<String> dppIdentifiers;
    private String nextCursor;

    public ReadDppIdsResponse() {
    }

    @JsonIgnore
    public DppStatusCode getStatusCode() { return statusCode; }
    public void setStatusCode(DppStatusCode statusCode) { this.statusCode = statusCode; }
    @JsonIgnore
    public List<DppApiMessage> getMessage() { return message; }
    public void setMessage(List<DppApiMessage> message) { this.message = message; }

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
