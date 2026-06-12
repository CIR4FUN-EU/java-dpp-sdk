package dpp.repo.client.exception;

/**
 * Base runtime exception for repository client failures.
 */
public class DppClientException extends RuntimeException {
    public DppClientException(String message) {
        super(message);
    }

    public DppClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
