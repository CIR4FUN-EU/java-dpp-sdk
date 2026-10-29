package dppsdk.core.transport.json;

/** Reports a malformed DPP JSON document or a value that cannot be represented by the codec. */
public final class DppJsonException extends RuntimeException {
    public DppJsonException(String message) {
        super(message);
    }

    public DppJsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
