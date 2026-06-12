/**
 * Transport-friendly payload POJOs for the DPP SDK.
 *
 * <p>These classes provide the transport-facing DTO layer for the SDK. Their
 * canonical Java structure mirrors the domain model, including nested
 * structures such as {@code coreDpp}, while compatibility accessors may exist
 * to support older calling code or legacy wire shapes.</p>
 *
 * <p>They are simple data carriers with no business logic, no validation, and
 * no mapping logic.</p>
 *
 * <p>Key design rules:</p>
 * <ul>
 *   <li>Plain Java POJOs with getters/setters</li>
 *   <li>Transport-friendly types: UUID → String, LocalDate → String (ISO-8601), enum → String</li>
 *   <li>No Jackson annotations on domain model — only payload classes may carry them if needed</li>
 *   <li>All field-by-field mapping between domain and payload lives in {@code dppsdk.core.mapper}</li>
 * </ul>
 *
 * <p>Supports both sending (outbound) and receiving (inbound) DPPs as JSON,
 * including compatibility handling where the transport codec preserves a legacy
 * flat JSON shape.</p>
 */
package dppsdk.core.payload;




