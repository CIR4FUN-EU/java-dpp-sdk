package dppsdk.dpp4fun.mapper;

import dppsdk.core.mapper.Mapper;
import dppsdk.core.mapper.MappingException;
import dppsdk.dpp4fun.model.BillOfMaterials;
import dppsdk.dpp4fun.model.Component;
import dppsdk.dpp4fun.model.Material;
import dppsdk.dpp4fun.model.Part;
import dppsdk.dpp4fun.payload.BillOfMaterialsPayload;
import dppsdk.dpp4fun.payload.ComponentPayload;
import dppsdk.dpp4fun.payload.MaterialPayload;
import dppsdk.dpp4fun.payload.PartPayload;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps between {@link BillOfMaterials} and {@link BillOfMaterialsPayload}.
 * Delegates to {@link MaterialMapper}, {@link ComponentMapper}, {@link PartMapper}.
 */
public class BillOfMaterialsMapper implements Mapper<BillOfMaterials, BillOfMaterialsPayload> {

    private final MaterialMapper materialMapper = new MaterialMapper();
    private final ComponentMapper componentMapper = new ComponentMapper();
    private final PartMapper partMapper = new PartMapper();

    @Override
    public BillOfMaterialsPayload toPayload(BillOfMaterials domain) {
        if (domain == null) return null;

        BillOfMaterialsPayload p = new BillOfMaterialsPayload();
        p.setMaterials(mapList(domain.getMaterials(), materialMapper::toPayload));
        p.setComponents(mapList(domain.getComponents(), componentMapper::toPayload));
        p.setParts(mapList(domain.getParts(), partMapper::toPayload));
        return p;
    }

    @Override
    public BillOfMaterials toDomain(BillOfMaterialsPayload payload) {
        if (payload == null) return null;

        try {
            BillOfMaterials.Builder builder = new BillOfMaterials.Builder();

            if (payload.getMaterials() != null) {
                for (MaterialPayload mp : payload.getMaterials()) {
                    builder.addMaterial(materialMapper.toDomain(mp));
                }
            }
            if (payload.getComponents() != null) {
                for (ComponentPayload cp : payload.getComponents()) {
                    builder.addComponent(componentMapper.toDomain(cp));
                }
            }
            if (payload.getParts() != null) {
                for (PartPayload pp : payload.getParts()) {
                    builder.addPart(partMapper.toDomain(pp));
                }
            }

            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new MappingException("Failed to map BillOfMaterialsPayload to BillOfMaterials: " + e.getMessage(), e);
        }
    }

    private <S, T> List<T> mapList(List<S> source, java.util.function.Function<S, T> mapFn) {
        if (source == null) return null;
        return source.stream().map(mapFn).collect(Collectors.toList());
    }
}


