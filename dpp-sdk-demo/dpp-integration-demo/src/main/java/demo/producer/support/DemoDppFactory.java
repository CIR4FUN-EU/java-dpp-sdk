package demo.producer.support;

import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.dpp4fun.model.aspect.BillOfMaterials;
import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.unit.LengthUnit;
import dppsdk.dpp4fun.model.composite.Material;
import dppsdk.dpp4fun.model.aspect.Nameplate;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Builds demo passports through the public typed D4.1 API. */
public final class DemoDppFactory {

    public static final String BED_DPP_ID = "9f2c5f7e-77a1-4f8a-a7fb-95f30e9a6c01";
    public static final String CHAIR_DPP_ID = "2e0dbac3-4d3f-4a9a-9dc8-7de3db4b14f2";
    public static final String BED_PRODUCT_ID = "04012345678911";
    public static final String CHAIR_PRODUCT_ID = "04012345678912";
    public static final String BRAND_PATH = "$.elements[1].elements[5]";
    public static final String FIRST_MATERIAL_NAME_PATH = "$.elements[0].elements[0].value[0].elements[0]";

    public Dpp4Fun createValidBedDpp() {
        return createDpp("Cir4Fun Platform Bed", BED_DPP_ID, BED_PRODUCT_ID, "C4F-DEMO-011");
    }

    public Dpp4Fun createValidChairDpp() {
        return createDpp("Cir4Fun Office Chair", CHAIR_DPP_ID, CHAIR_PRODUCT_ID, "C4F-DEMO-012");
    }

    public Dpp4Fun createUpdatedBedDpp() {
        return withBrand(createValidBedDpp(), "Cir4Fun Platform Bed - Updated Demo");
    }

    /** An incomplete typed passport used to demonstrate profile validation errors. */
    public Dpp4Fun createIncompleteDpp() {
        return Dpp4Fun.builder().build();
    }

    public Dpp4Fun withBrand(Dpp4Fun dpp, String productName) {
        Nameplate nameplate = dpp.getNameplate().toBuilder()
                .brand(productName)
                .build();
        return dpp.toBuilder().nameplate(nameplate).build();
    }

    public Dpp4Fun withFreshDppId(Dpp4Fun dpp) {
        DppHeader updated = dpp.getHeader().toBuilder()
                .digitalProductPassportId(UUID.randomUUID().toString())
                .lastUpdated(Instant.now())
                .build();
        return dpp.toBuilder().header(updated).build();
    }

    public String createMalformedJson() {
        return "{\n  \"digitalProductPassportId\": \"not-closed\"\n";
    }

    public static String idOf(Dpp4Fun dpp) {
        return dpp.getDigitalProductPassportId();
    }

    public static Function<Dpp4Fun, String> idExtractor() {
        return DemoDppFactory::idOf;
    }

    private Dpp4Fun createDpp(String productName, String dppId, String productId, String articleNumber) {
        DppHeader header = new DppHeader.Builder()
                .digitalProductPassportId(dppId)
                .uniqueProductIdentifier(productId)
                .granularity(Granularity.MODEL)
                .dppSchemaVersion(Dpp4Fun.SCHEMA_VERSION)
                .dppStatus("active")
                .lastUpdated(Instant.parse("2026-04-24T00:00:00Z"))
                .economicOperatorId("urn:example:operator:cir4fun")
                .build();
        Nameplate nameplate = Nameplate.builder()
                .gtinCode(productId)
                .internalArticleNumber(articleNumber)
                .manufacturerName("Cir4Fun Furniture GmbH")
                .manufacturerProductDesignation(Map.of("en-GB", productName, "de-DE", productName))
                .uriOfManufacturer(URI.create("https://demo.example/organizations/cir4fun-furniture-gmbh"))
                .brand("Cir4Fun")
                .uriOfTheProduct(URI.create("https://demo.example/products/" + articleNumber.toLowerCase()))
                .build();
        BillOfMaterials billOfMaterials = BillOfMaterials.builder()
                .addMaterial(new Material.Builder().name("FSC certified wood").mandatory(true).portion(72.0)
                        .reference(URI.create("urn:cir4fun:material:MAT-WOOD-001")).build())
                .addMaterial(new Material.Builder().name("Recycled steel fittings").mandatory(true).portion(8.0)
                        .reference(URI.create("urn:cir4fun:material:MAT-STEEL-002")).build())
                .build();
        ChairCharacteristics chair = ChairCharacteristics.builder()
                .length(120, LengthUnit.CENTIMETER)
                .width(90, LengthUnit.CENTIMETER)
                .height(80, LengthUnit.CENTIMETER)
                .weight(24.5, dppsdk.dpp4fun.model.unit.MassUnit.KILOGRAM)
                .color("Graphite")
                .seatHeight(46, LengthUnit.CENTIMETER)
                .stackable(false)
                .build();
        return Dpp4Fun.builder(header)
                .nameplate(nameplate)
                .billOfMaterials(billOfMaterials)
                .chairCharacteristics(chair)
                .build();
    }
}
