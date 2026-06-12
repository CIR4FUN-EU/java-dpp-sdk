package dpp.repo.client.exception;

/**
 * Thrown when a repository request cannot complete due to connection, timeout, I/O, or interruption failure.
 */
public class DppNetworkClientException extends DppClientException {
    public DppNetworkClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
