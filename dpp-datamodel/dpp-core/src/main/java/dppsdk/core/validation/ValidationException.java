package dppsdk.core.validation;

/**
 * Custom exception for validation failures in the DPP SDK.
 * Thrown by validators when business rules, nested validation, or cross-object consistency checks fail.
 * Provides clear, field-oriented error messages.
 */
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Construct a validation exception with a clear message.
     *
     * @param message The error message describing the validation failure
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Construct a validation exception with a message and cause.
     *
     * @param message The error message describing the validation failure
     * @param cause The underlying exception (if any)
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

