package dppsdk.core.validation;

/**
 * Generic validation interface for DPP model objects.
 * Implementations validate business logic, nested objects, and cross-object consistency.
 * Builders handle local object integrity; validators handle semantic rules.
 * 
 * Fail-fast: throws ValidationException on first error.
 *
 * @param <T> The type of object to validate
 */
public interface Validator<T> {
    /**
     * Validate the given object.
     * Throws ValidationException if validation fails.
     *
     * @param value The object to validate (typically non-null after builder construction)
     * @throws ValidationException if validation fails
     */
    void validate(T value) throws ValidationException;
}

