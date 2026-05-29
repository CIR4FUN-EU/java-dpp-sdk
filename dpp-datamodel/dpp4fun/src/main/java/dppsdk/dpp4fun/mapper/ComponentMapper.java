package dppsdk.dpp4fun.mapper;

import dppsdk.core.mapper.Mapper;
import dppsdk.core.mapper.MappingException;
import dppsdk.dpp4fun.model.Component;
import dppsdk.dpp4fun.payload.ComponentPayload;

/**
 * Maps between {@link Component} and {@link ComponentPayload}.
 */
public class ComponentMapper implements Mapper<Component, ComponentPayload> {

    @Override
    public ComponentPayload toPayload(Component domain) {
        if (domain == null) return null;

        ComponentPayload p = new ComponentPayload();
        p.setName(domain.getName());
        p.setReference(domain.getReference());
        return p;
    }

    @Override
    public Component toDomain(ComponentPayload payload) {
        if (payload == null) return null;

        try {
            return new Component.Builder()
                    .name(payload.getName())
                    .reference(payload.getReference())
                    .build();
        } catch (IllegalArgumentException e) {
            throw new MappingException("Failed to map ComponentPayload to Component: " + e.getMessage(), e);
        }
    }
}


