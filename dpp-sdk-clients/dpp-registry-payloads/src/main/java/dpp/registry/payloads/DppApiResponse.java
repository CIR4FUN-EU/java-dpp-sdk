package dpp.registry.payloads;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Standard response envelope used by registry endpoints.
 *
 * @param <T> payload type carried by the wrapper
 */
public class DppApiResponse<T> {
    private DppStatusCode statusCode;
    private T payload;
    private List<DppApiMessage> message;

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

    @JsonProperty("message")
    public List<DppApiMessage> getMessage() {
        return message;
    }

    @JsonProperty("message")
    @JsonAlias("messages")
    public void setMessage(List<DppApiMessage> message) {
        if (message != null) {
            message.forEach(item -> Objects.requireNonNull(item, "message entries must not be null"));
        }
        this.message = message;
    }

    /** @deprecated Use {@link #getMessage()}; retained for source compatibility. */
    @Deprecated
    @JsonIgnore
    public List<DppApiMessage> getMessages() {
        return message;
    }

    /** @deprecated Use {@link #setMessage(List)}; retained for source compatibility. */
    @Deprecated
    @JsonIgnore
    public void setMessages(List<DppApiMessage> messages) {
        setMessage(messages);
    }
}
