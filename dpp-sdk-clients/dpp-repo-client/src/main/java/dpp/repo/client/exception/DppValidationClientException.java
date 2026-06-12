package dpp.repo.client.exception;

/**
 * Thrown when repository client-side validation fails before an HTTP request is sent.
 */
public class DppValidationClientException extends DppClientException {
    public DppValidationClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
