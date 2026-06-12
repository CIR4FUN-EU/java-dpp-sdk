package dpp.repo.payloads;

import java.util.List;

/**
 * Standard response envelope used by repository endpoints.
 *
 * @param <T> payload type carried by the wrapper
 */
public class DppApiResponse<T> {
    private DppStatusCode statusCode;
    private T payload;
    private List<DppApiMessage> messages;

    public DppApiResponse() {
    }

    public DppStatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(DppStatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public List<DppApiMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<DppApiMessage> messages) {
        this.messages = messages;
    }
}
