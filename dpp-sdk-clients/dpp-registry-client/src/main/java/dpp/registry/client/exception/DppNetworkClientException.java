package dpp.registry.client.exception;

/**
 * Thrown when a registry request cannot complete due to connection, timeout, I/O, or interruption failure.
 */
public class DppNetworkClientException extends DppClientException {
    public DppNetworkClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
