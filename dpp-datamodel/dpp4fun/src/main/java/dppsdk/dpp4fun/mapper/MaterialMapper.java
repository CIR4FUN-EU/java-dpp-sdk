package dppsdk.dpp4fun.mapper;

import dppsdk.core.mapper.Mapper;
import dppsdk.core.mapper.MappingException;
import dppsdk.dpp4fun.model.Material;
import dppsdk.dpp4fun.payload.MaterialPayload;

/**
 * Maps between {@link Material} and {@link MaterialPayload}.
 */
public class MaterialMapper implements Mapper<Material, MaterialPayload> {

    @Override
    public MaterialPayload toPayload(Material domain) {
        if (domain == null) return null;

        MaterialPayload p = new MaterialPayload();
        p.setName(domain.getName());
        p.setMandatory(domain.isMandatory());
        p.setPortion(domain.getPortion());
        p.setReference(domain.getReference());
        return p;
    }

    @Override
    public Material toDomain(MaterialPayload payload) {
        if (payload == null) return null;

        try {
            return new Material.Builder()
                    .name(payload.getName())
                    .mandatory(payload.isMandatory())
                    .portion(payload.getPortion())
                    .reference(payload.getReference())
                    .build();
        } catch (IllegalArgumentException e) {
            throw new MappingException("Failed to map MaterialPayload to Material: " + e.getMessage(), e);
        }
    }
}


