package dpp.repo.payloads;

import java.util.List;

/**
 * Wrapper-derived result returned after a successful delete request.
 */
public class DeleteDppResponse {
    private DppStatusCode statusCode;
    private List<DppApiMessage> message;

    public DeleteDppResponse() {
    }

    public DeleteDppResponse(DppStatusCode statusCode, List<DppApiMessage> messages) {
        this.statusCode = statusCode;
        this.message = messages;
    }

    public DppStatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(DppStatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public List<DppApiMessage> getMessage() {
        return message;
    }

    public void setMessage(List<DppApiMessage> message) {
        this.message = message;
    }

}
