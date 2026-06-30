package dppsdk.postgres.core;

/**
 * Signals that the requested DPP cannot be resolved in PostgreSQL storage.
 */
public final class PostgresDppNotFoundException extends IllegalStateException {

    public PostgresDppNotFoundException(String dppId) {
        super("No active DPP found for " + dppId);
    }
}
