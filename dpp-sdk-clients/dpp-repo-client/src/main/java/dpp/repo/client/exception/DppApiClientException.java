package dpp.repo.client.exception;

import dpp.repo.payloads.DppApiMessage;
import dpp.repo.payloads.DppStatusCode;

import java.util.List;

/**
 * Thrown when a 2xx HTTP response contains an error status in the repository API wrapper.
 */
public class DppApiClientException extends DppClientException {
    private final DppStatusCode statusCode;
    private final List<DppApiMessage> messages;
    private final String rawResponseBody;

    public DppApiClientException(
            String message,
            DppStatusCode statusCode,
            List<DppApiMessage> messages,
            String rawResponseBody
    ) {
        super(message);
        this.statusCode = statusCode;
        this.messages = messages;
        this.rawResponseBody = rawResponseBody;
    }

    public DppStatusCode statusCode() {
        return statusCode;
    }

    public List<DppApiMessage> messages() {
        return messages;
    }

    public String rawResponseBody() {
        return rawResponseBody;
    }
}
