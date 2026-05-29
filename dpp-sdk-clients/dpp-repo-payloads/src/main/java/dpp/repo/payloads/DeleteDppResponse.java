package dpp.repo.payloads;

import java.util.List;

/**
 * Wrapper-derived result returned after a successful delete request.
 */
public class DeleteDppResponse {
    private DppStatusCode statusCode;
    private List<DppApiMessage> messages;

    public DeleteDppResponse() {
    }

    public DeleteDppResponse(DppStatusCode statusCode, List<DppApiMessage> messages) {
        this.statusCode = statusCode;
        this.messages = messages;
    }

    public DppStatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(DppStatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public List<DppApiMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<DppApiMessage> messages) {
        this.messages = messages;
    }
}
