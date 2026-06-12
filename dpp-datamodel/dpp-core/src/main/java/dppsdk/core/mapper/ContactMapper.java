package dppsdk.core.mapper;

import dppsdk.core.model.Contact;
import dppsdk.core.payload.ContactPayload;

/**
 * Maps between {@link Contact} and {@link ContactPayload}.
 * Delegates to {@link AddressMapper}, {@link EmailMapper}, {@link TelephoneMapper}.
 */
public class ContactMapper implements Mapper<Contact, ContactPayload> {

    private final AddressMapper addressMapper = new AddressMapper();
    private final EmailMapper emailMapper = new EmailMapper();
    private final TelephoneMapper telephoneMapper = new TelephoneMapper();

    @Override
    public ContactPayload toPayload(Contact domain) {
        if (domain == null) return null;

        ContactPayload p = new ContactPayload();
        p.setOrganization(domain.getOrganization());
        p.setAddress(addressMapper.toPayload(domain.getAddress()));
        p.setEmail(emailMapper.toPayload(domain.getEmail()));
        p.setTelephone(telephoneMapper.toPayload(domain.getTelephone()));
        return p;
    }

    @Override
    public Contact toDomain(ContactPayload payload) {
        if (payload == null) return null;

        try {
            Contact.Builder builder = new Contact.Builder();
            if (payload.getOrganization() != null) {
                builder.organization(payload.getOrganization());
            }
            if (payload.getAddress() != null) {
                builder.address(addressMapper.toDomain(payload.getAddress()));
            }
            if (payload.getEmail() != null) {
                builder.email(emailMapper.toDomain(payload.getEmail()));
            }
            if (payload.getTelephone() != null) {
                builder.telephone(telephoneMapper.toDomain(payload.getTelephone()));
            }
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new MappingException("Failed to map ContactPayload to Contact: " + e.getMessage(), e);
        }
    }
}

