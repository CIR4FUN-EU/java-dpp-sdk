package dpp.repo.client.exception;

import dpp.repo.payloads.DppApiMessage;
import dpp.repo.payloads.DppStatusCode;

import java.util.List;

/**
 * Thrown when an HTTP response contains a structured repository API error.
 */
public class DppApiClientException extends DppClientException {
    private final int httpStatus;
    private final DppStatusCode statusCode;
    private final List<DppApiMessage> messages;
    private final String rawResponseBody;

    public DppApiClientException(
            String message,
            DppStatusCode statusCode,
            List<DppApiMessage> messages,
            String rawResponseBody
    ) {
        this(message, -1, statusCode, messages, rawResponseBody, null);
    }

    public DppApiClientException(
            String message,
            int httpStatus,
            DppStatusCode statusCode,
            List<DppApiMessage> messages,
            String rawResponseBody,
            Throwable cause
    ) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.statusCode = statusCode;
        this.messages = messages;
        this.rawResponseBody = rawResponseBody;
    }

    public int httpStatus() { return httpStatus; }

    public DppStatusCode statusCode() {
        return statusCode;
    }

    public List<DppApiMessage> message() {
        return messages;
    }

    public String rawResponseBody() {
        return rawResponseBody;
    }
}
