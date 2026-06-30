package dppsdk.postgres.core;

/**
 * Signals optimistic-lock style version conflicts in PostgreSQL storage.
 */
public final class PostgresDppVersionConflictException extends IllegalStateException {

    public PostgresDppVersionConflictException(String dppId, long expectedCurrentVersion, long actualCurrentVersion) {
        super("Stale expected version " + expectedCurrentVersion + " for " + dppId + "; current version is " + actualCurrentVersion);
    }
}
