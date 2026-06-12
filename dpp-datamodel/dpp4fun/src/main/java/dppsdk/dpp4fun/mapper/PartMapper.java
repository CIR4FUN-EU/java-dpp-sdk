package dppsdk.dpp4fun.mapper;

import dppsdk.core.mapper.Mapper;
import dppsdk.core.mapper.MappingException;
import dppsdk.dpp4fun.model.Part;
import dppsdk.dpp4fun.payload.PartPayload;

/**
 * Maps between {@link Part} and {@link PartPayload}.
 */
public class PartMapper implements Mapper<Part, PartPayload> {

    @Override
    public PartPayload toPayload(Part domain) {
        if (domain == null) return null;

        PartPayload p = new PartPayload();
        p.setName(domain.getName());
        p.setMandatory(domain.isMandatory());
        p.setReference(domain.getReference());
        return p;
    }

    @Override
    public Part toDomain(PartPayload payload) {
        if (payload == null) return null;

        try {
            return new Part.Builder()
                    .name(payload.getName())
                    .mandatory(payload.isMandatory())
                    .reference(payload.getReference())
                    .build();
        } catch (IllegalArgumentException e) {
            throw new MappingException("Failed to map PartPayload to Part: " + e.getMessage(), e);
        }
    }
}


