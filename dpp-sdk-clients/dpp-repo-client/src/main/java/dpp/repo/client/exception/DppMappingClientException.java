package dpp.repo.client.exception;

/**
 * Thrown when repository request or response content cannot be serialized or deserialized.
 */
public class DppMappingClientException extends DppClientException {
    public DppMappingClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
