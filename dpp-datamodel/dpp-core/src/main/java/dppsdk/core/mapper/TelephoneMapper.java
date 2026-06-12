package dppsdk.core.mapper;

import dppsdk.core.model.Telephone;
import dppsdk.core.payload.TelephonePayload;

/**
 * Maps between {@link Telephone} and {@link TelephonePayload}.
 */
public class TelephoneMapper implements Mapper<Telephone, TelephonePayload> {

    @Override
    public TelephonePayload toPayload(Telephone domain) {
        if (domain == null) return null;

        TelephonePayload p = new TelephonePayload();
        p.setTelephoneNumber(domain.getTelephoneNumber());
        p.setTypeOfTelephone(domain.getTypeOfTelephone());
        return p;
    }

    @Override
    public Telephone toDomain(TelephonePayload payload) {
        if (payload == null) return null;

        try {
            Telephone.Builder builder = new Telephone.Builder();
            if (payload.getTelephoneNumber() != null) {
                builder.telephoneNumber(payload.getTelephoneNumber());
            }
            if (payload.getTypeOfTelephone() != null) {
                builder.typeOfTelephone(payload.getTypeOfTelephone());
            }
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new MappingException("Failed to map TelephonePayload to Telephone: " + e.getMessage(), e);
        }
    }
}

