package dppsdk.core.mapper;

import dppsdk.core.model.Nameplate;
import dppsdk.core.payload.NameplatePayload;

/**
 * Maps between {@link Nameplate} and {@link NameplatePayload}.
 * Delegates to {@link OrganizationMapper} for manufacturer and supplier.
 */
public class NameplateMapper implements Mapper<Nameplate, NameplatePayload> {

    private final OrganizationMapper organizationMapper = new OrganizationMapper();

    @Override
    public NameplatePayload toPayload(Nameplate domain) {
        if (domain == null) return null;

        NameplatePayload p = new NameplatePayload();
        p.setGtinCode(domain.getGtinCode());
        p.setInternalArticleNumber(domain.getInternalArticleNumber());
        p.setBatchNumber(domain.getBatchNumber());
        p.setCustomsTariffNumber(domain.getCustomsTariffNumber());
        p.setUriOfTheProduct(domain.getUriOfTheProduct());
        p.setManufacturer(organizationMapper.toPayload(domain.getManufacturer()));
        p.setSupplier(organizationMapper.toPayload(domain.getSupplier()));
        return p;
    }

    @Override
    public Nameplate toDomain(NameplatePayload payload) {
        if (payload == null) return null;

        try {
            Nameplate.Builder builder = new Nameplate.Builder();
            if (payload.getGtinCode() != null) {
                builder.gtinCode(payload.getGtinCode());
            }
            if (payload.getInternalArticleNumber() != null) {
                builder.internalArticleNumber(payload.getInternalArticleNumber());
            }
            if (payload.getBatchNumber() != null) {
                builder.batchNumber(payload.getBatchNumber());
            }
            if (payload.getCustomsTariffNumber() != null) {
                builder.customsTariffNumber(payload.getCustomsTariffNumber());
            }
            if (payload.getUriOfTheProduct() != null) {
                builder.uriOfTheProduct(payload.getUriOfTheProduct());
            }
            if (payload.getManufacturer() != null) {
                builder.manufacturer(organizationMapper.toDomain(payload.getManufacturer()));
            }
            if (payload.getSupplier() != null) {
                builder.supplier(organizationMapper.toDomain(payload.getSupplier()));
            }
            return builder.build();
        } catch (MappingException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new MappingException(
                    "Failed to map NameplatePayload to Nameplate: " + e.getMessage(), e);
        }
    }
}

