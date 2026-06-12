package dppsdk.core.mapper;

import dppsdk.core.model.Email;
import dppsdk.core.payload.EmailPayload;

/**
 * Maps between {@link Email} and {@link EmailPayload}.
 */
public class EmailMapper implements Mapper<Email, EmailPayload> {

    @Override
    public EmailPayload toPayload(Email domain) {
        if (domain == null) return null;

        EmailPayload p = new EmailPayload();
        p.setEmailAddress(domain.getEmailAddress());
        p.setTypeOfEmail(domain.getTypeOfEmail());
        return p;
    }

    @Override
    public Email toDomain(EmailPayload payload) {
        if (payload == null) return null;

        try {
            Email.Builder builder = new Email.Builder();
            if (payload.getEmailAddress() != null) {
                builder.emailAddress(payload.getEmailAddress());
            }
            if (payload.getTypeOfEmail() != null) {
                builder.typeOfEmail(payload.getTypeOfEmail());
            }
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new MappingException("Failed to map EmailPayload to Email: " + e.getMessage(), e);
        }
    }
}

