package dppsdk.core.mapper;

import dppsdk.core.model.DppCore;
import dppsdk.core.payload.DppCorePayload;

/**
 * Maps the reusable DPP core fields to and from the canonical nested core payload.
 */
public class DppCoreMapper implements Mapper<DppCore, DppCorePayload> {

    private final PassportMetadataMapper passportMetadataMapper = new PassportMetadataMapper();
    private final NameplateMapper nameplateMapper = new NameplateMapper();
    private final DocumentationMapper documentationMapper = new DocumentationMapper();

    @Override
    public DppCorePayload toPayload(DppCore domain) {
        if (domain == null) return null;

        DppCorePayload payload = new DppCorePayload();
        payload.setPassportMetadata(passportMetadataMapper.toPayload(domain.getPassportMetadata()));
        payload.setNameplate(nameplateMapper.toPayload(domain.getNameplate()));
        payload.setDocumentation(documentationMapper.toPayload(domain.getDocumentation()));
        return payload;
    }

    @Override
    public DppCore toDomain(DppCorePayload payload) {
        if (payload == null) return null;

        try {
            DppCore.Builder builder = new DppCore.Builder();
            if (payload.getPassportMetadata() != null) {
                builder.passportMetadata(passportMetadataMapper.toDomain(payload.getPassportMetadata()));
            }
            if (payload.getNameplate() != null) {
                builder.nameplate(nameplateMapper.toDomain(payload.getNameplate()));
            }
            if (payload.getDocumentation() != null) {
                builder.documentation(documentationMapper.toDomain(payload.getDocumentation()));
            }
            return builder.build();
        } catch (MappingException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new MappingException("Failed to map DppCorePayload to DppCore: " + e.getMessage(), e);
        }
    }
}
