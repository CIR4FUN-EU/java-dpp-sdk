package demo.repo.testsupport;

import dppsdk.core.model.DppHeader;
import dppsdk.dpp4fun.model.characteristics.CribCharacteristics;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.unit.LengthUnit;
import dppsdk.dpp4fun.model.aspect.Nameplate;
import java.util.UUID;

public final class DemoDppFactory {
    public static final String BED_DPP_ID = "49192c87-20c8-4b6f-88de-48b56ca4c211";
    public static final String CHAIR_DPP_ID = "b2368516-3312-420a-9f71-55a51240c229";
    public static final String BRAND_PATH = "$.elements[5].elements[7]";
    public static final String GTIN_PATH = "$.elements[5].elements[0]";
    public static final String BILL_OF_MATERIALS_PATH = "$.elements[0]";
    public static final String MATERIALS_PATH = "$.elements[0].elements[0]";
    public static final String FIRST_MATERIAL_NAME_PATH = "$.elements[0].elements[0].value[0].elements[0]";

    public Dpp4Fun createValidBedDpp() {
        Dpp4Fun comprehensive = TypedDppFixtures.comprehensiveChair(BED_DPP_ID, "04012345678901");
        Nameplate nameplate = comprehensive.getNameplate().toBuilder().internalArticleNumber("C4F-DEMO-BED-001")
                .brand("Cir4Fun Platform Bed").build();
        return comprehensive.toBuilder().nameplate(nameplate).chairCharacteristics(null)
                .cribCharacteristics(CribCharacteristics.builder().length(120, LengthUnit.CENTIMETER)
                        .width(70, LengthUnit.CENTIMETER).mattressIncluded(true).color("warm oak").build())
                .build();
    }

    public Dpp4Fun createValidChairDpp() {
        return TypedDppFixtures.comprehensiveChair(CHAIR_DPP_ID, "04012345678902");
    }

    public Dpp4Fun createUpdatedBedDpp() {
        return withBrand(createValidBedDpp(), "Cir4Fun Bed Collection");
    }

    public Dpp4Fun withBrand(Dpp4Fun dpp, String brand) {
        return dpp.toBuilder().nameplate(dpp.getNameplate().toBuilder().brand(brand).build()).build();
    }

    public String createMalformedJson() {
        return "{\n  \"digitalProductPassportId\": \"not-closed\"\n";
    }

    public Dpp4Fun withFreshDppId(Dpp4Fun dpp) {
        DppHeader header = dpp.getHeader().toBuilder()
                .digitalProductPassportId("urn:uuid:" + UUID.randomUUID()).build();
        return dpp.toBuilder().header(header).build();
    }
}
