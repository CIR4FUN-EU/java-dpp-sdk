package dpp.repo.client.core;

/**
 * Validates a caller-owned DPP type before full create requests are sent.
 */
public interface DppValidator<T> {
    void validate(T dpp);
}
