package dppsdk.core.mapper;

import dppsdk.core.model.Address;
import dppsdk.core.payload.AddressPayload;

/**
 * Maps between {@link Address} and {@link AddressPayload}.
 */
public class AddressMapper implements Mapper<Address, AddressPayload> {

    @Override
    public AddressPayload toPayload(Address domain) {
        if (domain == null) return null;

        AddressPayload p = new AddressPayload();
        p.setCountry(domain.getCountry());
        p.setZipCode(domain.getZipCode());
        p.setRegion(domain.getRegion());
        p.setTown(domain.getTown());
        p.setStreet(domain.getStreet());
        return p;
    }

    @Override
    public Address toDomain(AddressPayload payload) {
        if (payload == null) return null;

        Address.Builder builder = new Address.Builder();
        if (payload.getCountry() != null) {
            builder.country(payload.getCountry());
        }
        if (payload.getZipCode() != null) {
            builder.zipCode(payload.getZipCode());
        }
        if (payload.getRegion() != null) {
            builder.region(payload.getRegion());
        }
        if (payload.getTown() != null) {
            builder.town(payload.getTown());
        }
        if (payload.getStreet() != null) {
            builder.street(payload.getStreet());
        }

        try {
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new MappingException("Failed to map AddressPayload to Address: " + e.getMessage(), e);
        }
    }
}

