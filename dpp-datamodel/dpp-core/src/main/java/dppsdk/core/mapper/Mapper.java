package dppsdk.core.mapper;

/**
 * Generic bidirectional mapper interface for converting between
 * domain objects and transport payload objects.
 *
 * <p>ALL field-by-field mapping logic in this SDK lives exclusively
 * in implementations of this interface within the {@code dppsdk.core.mapper} package.</p>
 *
 * <p>Supports both directions:</p>
 * <ul>
 *   <li>OUTBOUND: {@link #toPayload(Object)} — domain → payload</li>
 *   <li>INBOUND: {@link #toDomain(Object)} — payload → domain</li>
 * </ul>
 *
 * @param <D> the domain type
 * @param <P> the payload type
 */
public interface Mapper<D, P> {

    /**
     * Convert a domain object to its transport payload representation.
     *
     * @param domain the domain object (may be null)
     * @return the payload object, or null if domain is null
     * @throws MappingException if mapping fails
     */
    P toPayload(D domain);

    /**
     * Convert a transport payload back to its domain object representation.
     *
     * @param payload the payload object (may be null)
     * @return the domain object, or null if payload is null
     * @throws MappingException if mapping fails (e.g. invalid enum value, unparseable UUID)
     */
    D toDomain(P payload);
}


