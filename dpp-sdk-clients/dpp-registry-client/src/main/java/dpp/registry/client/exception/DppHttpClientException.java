package dpp.registry.client.exception;

/**
 * Thrown when a registry endpoint returns a non-2xx HTTP status.
 */
public class DppHttpClientException extends DppClientException {
    private final int statusCode;
    private final String responseBody;

    public DppHttpClientException(String message, int statusCode, String responseBody) {
        this(message, statusCode, responseBody, null);
    }

    public DppHttpClientException(String message, int statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }
}
