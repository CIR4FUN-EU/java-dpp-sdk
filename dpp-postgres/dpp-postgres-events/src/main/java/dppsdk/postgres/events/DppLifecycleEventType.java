package dppsdk.postgres.events;

/** SDK-defined event types available for recording DPP change and lifecycle audit metadata. */
public enum DppLifecycleEventType {
    DPP_CREATED,
    DPP_UPDATED,
    DATA_ELEMENT_UPDATED,
    DPP_DELETED
}
