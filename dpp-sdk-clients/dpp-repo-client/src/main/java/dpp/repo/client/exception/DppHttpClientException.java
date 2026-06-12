package dpp.repo.client.exception;

/**
 * Thrown when a repository endpoint returns a non-2xx HTTP status.
 */
public class DppHttpClientException extends DppClientException {
    private final int statusCode;
    private final String responseBody;

    public DppHttpClientException(String message, int statusCode, String responseBody) {
        super(message);
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
