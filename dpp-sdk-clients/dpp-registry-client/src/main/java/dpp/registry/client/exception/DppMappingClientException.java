package dpp.registry.client.exception;

/**
 * Thrown when registry request or response content cannot be serialized or deserialized.
 */
public class DppMappingClientException extends DppClientException {
    public DppMappingClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
