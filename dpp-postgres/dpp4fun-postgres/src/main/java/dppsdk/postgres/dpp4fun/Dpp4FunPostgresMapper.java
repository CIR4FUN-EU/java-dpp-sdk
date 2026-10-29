package dppsdk.postgres.dpp4fun;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.postgres.projection.DppProjectionDefinition;
import dppsdk.postgres.projection.DppProjectionDocument;
import dppsdk.postgres.projection.DppProjectionValueType;
import java.util.List;

/** Maps the typed Dpp4Fun facade to a rebuildable generic projection document. */
public final class Dpp4FunPostgresMapper {
    public DppProjectionDefinition projectionDefinition() {
        return DppProjectionDefinition.builder("dpp4fun")
                .extract(this::project)
                .btree("brand", DppProjectionValueType.TEXT)
                .gin("materials")
                .gin("components")
                .gin("parts")
                .build();
    }

    public DppProjectionDocument project(DigitalProductPassport passport) { return project(Dpp4Fun.from(passport)); }

    public DppProjectionDocument project(Dpp4Fun dpp) {
        List<String> materials = dpp.getBillOfMaterials() == null ? List.of()
                : dpp.getBillOfMaterials().getMaterials().stream().map(value -> value.getName()).filter(java.util.Objects::nonNull).toList();
        List<String> components = dpp.getBillOfMaterials() == null ? List.of()
                : dpp.getBillOfMaterials().getComponents().stream().map(value -> value.getName()).filter(java.util.Objects::nonNull).toList();
        List<String> parts = dpp.getBillOfMaterials() == null ? List.of()
                : dpp.getBillOfMaterials().getParts().stream().map(value -> value.getName()).filter(java.util.Objects::nonNull).toList();
        var nameplate = dpp.getNameplate();
        return DppProjectionDocument.builder()
                .text("dppId", dpp.getDigitalProductPassportId())
                .text("productId", dpp.getUniqueProductIdentifier())
                .text("brand", nameplate == null ? null : nameplate.getBrand())
                .strings("materials", materials)
                .strings("components", components)
                .strings("parts", parts)
                .build();
    }
}
