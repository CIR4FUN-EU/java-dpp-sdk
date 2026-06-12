package dppsdk.core.mapper;

/**
 * Runtime exception indicating a failure during domain ↔ payload mapping.
 *
 * <p>Distinguishes mapping failures (e.g. invalid UUID string, unknown enum value)
 * from Jackson parsing failures and validation failures.</p>
 */
public class MappingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MappingException(String message) {
        super(message);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}

