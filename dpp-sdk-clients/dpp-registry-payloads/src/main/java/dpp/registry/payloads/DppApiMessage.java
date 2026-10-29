package dpp.registry.payloads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;

/**
 * One entry from the registry Result {@code message} array.
 */
public class DppApiMessage {
    private MessageType messageType;
    private String text;
    private String code;
    private String correlationId;
    private Instant timestamp;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DppApiMessage(
            @JsonProperty(value = "messageType", required = true) MessageType messageType,
            @JsonProperty(value = "text", required = true) String text
    ) {
        this.messageType = Objects.requireNonNull(messageType, "messageType must not be null");
        this.text = Objects.requireNonNull(text, "text must not be null");
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = Objects.requireNonNull(messageType, "messageType must not be null");
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = Objects.requireNonNull(text, "text must not be null");
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
