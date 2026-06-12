package dppsdk.core.mapper;

import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.payload.OrganizationPayload;

/**
 * Maps between {@link Organization} and {@link OrganizationPayload}.
 *
 * <p>{@link OrganizationRole} is mapped as a String in the payload
 * using {@code name()} / {@code valueOf()} for simplicity.
 * An unrecognized role string on inbound will throw {@link MappingException}.</p>
 */
public class OrganizationMapper implements Mapper<Organization, OrganizationPayload> {

    private final ContactMapper contactMapper = new ContactMapper();

    @Override
    public OrganizationPayload toPayload(Organization domain) {
        if (domain == null) return null;

        OrganizationPayload p = new OrganizationPayload();
        p.setName(domain.getName());
        p.setGln(domain.getGln());
        p.setProductDescription(domain.getProductDescription());
        p.setProductDesignation(domain.getProductDesignation());
        p.setProductFamily(domain.getProductFamily());
        p.setProductRoot(domain.getProductRoot());
        p.setProductOrderSuffix(domain.getProductOrderSuffix());
        p.setUri(domain.getUri());
        p.setContact(contactMapper.toPayload(domain.getContact()));
        p.setRole(domain.getRole() != null ? domain.getRole().name() : null);
        return p;
    }

    @Override
    public Organization toDomain(OrganizationPayload payload) {
        if (payload == null) return null;

        OrganizationRole role = null;
        if (payload.getRole() != null) {
            try {
                role = OrganizationRole.valueOf(payload.getRole());
            } catch (IllegalArgumentException e) {
                throw new MappingException(
                        "Unknown OrganizationRole: '" + payload.getRole() + "'", e);
            }
        }

        try {
            Organization.Builder builder = new Organization.Builder();
            if (payload.getName() != null) {
                builder.name(payload.getName());
            }
            if (payload.getGln() != null) {
                builder.gln(payload.getGln());
            }
            if (payload.getProductDescription() != null) {
                builder.productDescription(payload.getProductDescription());
            }
            if (payload.getProductDesignation() != null) {
                builder.productDesignation(payload.getProductDesignation());
            }
            if (payload.getProductFamily() != null) {
                builder.productFamily(payload.getProductFamily());
            }
            if (payload.getProductRoot() != null) {
                builder.productRoot(payload.getProductRoot());
            }
            if (payload.getProductOrderSuffix() != null) {
                builder.productOrderSuffix(payload.getProductOrderSuffix());
            }
            if (payload.getUri() != null) {
                builder.uri(payload.getUri());
            }
            if (payload.getContact() != null) {
                builder.contact(contactMapper.toDomain(payload.getContact()));
            }
            if (payload.getRole() != null) {
                builder.role(role);
            }
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new MappingException("Failed to map OrganizationPayload to Organization: " + e.getMessage(), e);
        }
    }
}

