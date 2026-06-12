package dppsdk.dpp4fun.mapper;

import dppsdk.core.mapper.DppCoreMapper;
import dppsdk.core.mapper.Mapper;
import dppsdk.core.mapper.MappingException;
import dppsdk.core.payload.DppCorePayload;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.payload.Dpp4FunPayload;

/**
 * Top-level mapper for {@link Dpp4Fun} and {@link Dpp4FunPayload}.
 *
 * <p>Delegates all field-by-field mapping to the appropriate sub-mappers.
 * This is the single entry point for full DPP mapping in both directions.</p>
 *
 * <p>Supports both:</p>
 * <ul>
 *   <li>OUTBOUND: domain to payload (for sending)</li>
 *   <li>INBOUND: payload to domain (for receiving)</li>
 * </ul>
 */
public class Dpp4FunMapper implements Mapper<Dpp4Fun, Dpp4FunPayload> {

    private final DppCoreMapper coreDppMapper = new DppCoreMapper();
    private final ProductClassificationMapper classificationMapper = new ProductClassificationMapper();
    private final CharacteristicsMapper characteristicsMapper = new CharacteristicsMapper();
    private final BillOfMaterialsMapper billOfMaterialsMapper = new BillOfMaterialsMapper();

    @Override
    public Dpp4FunPayload toPayload(Dpp4Fun domain) {
        if (domain == null) return null;

        Dpp4FunPayload p = new Dpp4FunPayload();
        p.setCoreDpp(coreDppMapper.toPayload(domain.getCoreDpp()));
        p.setClassification(classificationMapper.toPayload(domain.getClassification()));
        p.setCharacteristics(characteristicsMapper.toPayload(domain.getCharacteristics()));
        p.setBillOfMaterials(billOfMaterialsMapper.toPayload(domain.getBillOfMaterials()));
        return p;
    }

    @Override
    public Dpp4Fun toDomain(Dpp4FunPayload payload) {
        if (payload == null) return null;

        try {
            Dpp4Fun.Builder builder = new Dpp4Fun.Builder();
            DppCorePayload corePayload = payload.getCoreDpp();
            if (corePayload == null
                    && (payload.getPassportMetadata() != null
                    || payload.getNameplate() != null
                    || payload.getDocumentation() != null)) {
                corePayload = new DppCorePayload();
                corePayload.setPassportMetadata(payload.getPassportMetadata());
                corePayload.setNameplate(payload.getNameplate());
                corePayload.setDocumentation(payload.getDocumentation());
            }
            builder.coreDpp(coreDppMapper.toDomain(corePayload));
            if (payload.getClassification() != null) {
                builder.classification(classificationMapper.toDomain(payload.getClassification()));
            }
            if (payload.getCharacteristics() != null) {
                builder.characteristics(characteristicsMapper.toDomain(payload.getCharacteristics()));
            }
            if (payload.getBillOfMaterials() != null) {
                builder.billOfMaterials(billOfMaterialsMapper.toDomain(payload.getBillOfMaterials()));
            }

            return builder.build();
        } catch (MappingException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new MappingException(
                    "Failed to map Dpp4FunPayload to Dpp4Fun: " + e.getMessage(), e);
        }
    }
}
