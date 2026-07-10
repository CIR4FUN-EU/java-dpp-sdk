package dppsdk.postgres.core;

/**
 * Lifecycle event types currently supported by the mock-compatible PostgreSQL persistence flow.
 */
public enum DppLifecycleEventType {
    DPP_CREATED,
    DPP_UPDATED,
    DATA_ELEMENT_UPDATED,
    DPP_DELETED
}
