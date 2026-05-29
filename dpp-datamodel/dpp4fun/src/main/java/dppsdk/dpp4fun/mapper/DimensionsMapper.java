package dppsdk.dpp4fun.mapper;

import dppsdk.core.mapper.Mapper;
import dppsdk.core.mapper.MappingException;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.dpp4fun.payload.DimensionsPayload;

/**
 * Maps between {@link Dimensions} and {@link DimensionsPayload}.
 */
public class DimensionsMapper implements Mapper<Dimensions, DimensionsPayload> {

    @Override
    public DimensionsPayload toPayload(Dimensions domain) {
        if (domain == null) return null;

        DimensionsPayload p = new DimensionsPayload();
        p.setWidth(domain.getWidth());
        p.setHeight(domain.getHeight());
        p.setDepth(domain.getDepth());
        p.setUnit(domain.getUnit());
        return p;
    }

    @Override
    public Dimensions toDomain(DimensionsPayload payload) {
        if (payload == null) return null;

        try {
            Dimensions.Builder builder = new Dimensions.Builder();
            if (payload.getWidth() != null) {
                builder.width(payload.getWidth());
            }
            if (payload.getHeight() != null) {
                builder.height(payload.getHeight());
            }
            if (payload.getDepth() != null) {
                builder.depth(payload.getDepth());
            }
            if (payload.getUnit() != null) {
                builder.unit(payload.getUnit());
            }
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new MappingException("Failed to map DimensionsPayload to Dimensions: " + e.getMessage(), e);
        }
    }
}


