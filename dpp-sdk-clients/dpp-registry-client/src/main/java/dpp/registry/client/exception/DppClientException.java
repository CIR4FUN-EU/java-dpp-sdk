package dpp.registry.client.exception;

/**
 * Base runtime exception for registry client failures.
 */
public class DppClientException extends RuntimeException {
    public DppClientException(String message) {
        super(message);
    }

    public DppClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
