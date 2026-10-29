package demo.repo;

import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.dpp4fun.model.aspect.BillOfMaterials;
import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.unit.LengthUnit;
import dppsdk.dpp4fun.model.unit.MassUnit;
import dppsdk.dpp4fun.model.composite.Material;
import dppsdk.dpp4fun.model.aspect.Nameplate;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

/** Typed source-aligned Dpp4Fun data used by the Postman walkthroughs. */
final class PostmanSeedData {

    static final String DPP_ID = "49192c87-20c8-4b6f-88de-48b56ca4c211";
    static final String PRODUCT_ID = "04012345678901";
    static final String BRAND_PATH = "$.elements[1].elements[7]";
    static final String DELETE_EXAMPLE_DPP_ID = "33333333-3333-3333-3333-333333333333";
    static final String DELETE_EXAMPLE_PRODUCT_ID = "04012345678998";
    static final String LIFECYCLE_DEFAULT_DPP_ID = "44444444-4444-4444-4444-444444444444";
    static final String LIFECYCLE_DEFAULT_PRODUCT_ID = "04012345678997";
    static final String LIFECYCLE_DELETE_DPP_ID = "44444444-4444-4444-4444-555555555555";
    static final String LIFECYCLE_DELETE_PRODUCT_ID = "04012345678994";
    static final String FINE_GRAINED_DEFAULT_DPP_ID = "55555555-5555-5555-5555-555555555555";
    static final String FINE_GRAINED_DEFAULT_PRODUCT_ID = "04012345678996";
    static final String FINE_GRAINED_DELETE_DPP_ID = "55555555-5555-5555-5555-666666666666";
    static final String FINE_GRAINED_DELETE_PRODUCT_ID = "04012345678993";
    static final String REGISTRY_DELETE_DPP_ID = "66666666-6666-6666-6666-666666666666";
    static final String REGISTRY_DELETE_PRODUCT_ID = "04012345678995";
    static final String HISTORICAL_SWAGGER_DPP_ID = "77777777-7777-7777-7777-777777777777";
    static final String HISTORICAL_SWAGGER_PRODUCT_ID = "04012345678992";
    static final Instant HISTORICAL_SWAGGER_VALID_FROM = Instant.parse("2026-06-08T10:00:00Z");
    static final String HISTORICAL_SWAGGER_QUERY_AT = "2026-06-08T10:15:30Z";

    private PostmanSeedData() { }

    static Dpp4Fun createSeedDpp() {
        return createSeedDpp(DPP_ID, PRODUCT_ID, "Cir4Fun Platform Bed", "Beds", "Warm oak", "C4F-DEMO-001");
    }
    static Dpp4Fun createDeleteExampleDpp() {
        return createSeedDpp(DELETE_EXAMPLE_DPP_ID, DELETE_EXAMPLE_PRODUCT_ID,
                "Cir4Fun Delete Example Stool", "Stools", "Natural ash", "C4F-DELETE-001");
    }
    static Dpp4Fun createLifecycleDefaultDpp() {
        return createSeedDpp(LIFECYCLE_DEFAULT_DPP_ID, LIFECYCLE_DEFAULT_PRODUCT_ID,
                "Cir4Fun Lifecycle Default Bed", "Beds", "Smoked oak", "C4F-LIFECYCLE-001");
    }
    static Dpp4Fun createLifecycleDeleteDpp() {
        return createSeedDpp(LIFECYCLE_DELETE_DPP_ID, LIFECYCLE_DELETE_PRODUCT_ID,
                "Cir4Fun Lifecycle Delete Stool", "Stools", "Stone grey", "C4F-LIFECYCLE-DEL-001");
    }
    static Dpp4Fun createFineGrainedDefaultDpp() {
        return createSeedDpp(FINE_GRAINED_DEFAULT_DPP_ID, FINE_GRAINED_DEFAULT_PRODUCT_ID,
                "Cir4Fun Fine Granular Chair", "Chairs", "Graphite", "C4F-FINE-001");
    }
    static Dpp4Fun createFineGrainedDeleteDpp() {
        return createSeedDpp(FINE_GRAINED_DELETE_DPP_ID, FINE_GRAINED_DELETE_PRODUCT_ID,
                "Cir4Fun Fine Granular Delete Bench", "Benches", "Walnut", "C4F-FINE-DEL-001");
    }
    static Dpp4Fun createRegistryDeleteDpp() {
        return createSeedDpp(REGISTRY_DELETE_DPP_ID, REGISTRY_DELETE_PRODUCT_ID,
                "Cir4Fun Registry Delete Cabinet", "Cabinets", "Sand", "C4F-REG-DEL-001");
    }
    static Dpp4Fun createHistoricalSwaggerDpp() {
        return createSeedDpp(HISTORICAL_SWAGGER_DPP_ID, HISTORICAL_SWAGGER_PRODUCT_ID,
                "Cir4Fun Historical Swagger Desk", "Desks", "Natural oak", "C4F-SWAGGER-HISTORY-001");
    }

    private static Dpp4Fun createSeedDpp(String dppId, String productId, String productName,
            String productType, String color, String articleNumber) {
        DppHeader header = new DppHeader.Builder()
                .digitalProductPassportId(dppId).uniqueProductIdentifier(productId)
                .granularity(Granularity.MODEL).dppSchemaVersion(Dpp4Fun.SCHEMA_VERSION)
                .dppStatus("active").lastUpdated(HISTORICAL_SWAGGER_VALID_FROM)
                .economicOperatorId("urn:example:operator:cir4fun").build();
        Nameplate nameplate = Nameplate.builder().gtinCode(productId)
                .internalArticleNumber(articleNumber).batchNumber("DEMO-2026-04")
                .manufacturerName("Cir4Fun Furniture GmbH")
                .manufacturerProductDesignation(Map.of("en-GB", productName))
                .uriOfManufacturer(URI.create("https://demo.example/organizations/cir4fun-furniture-gmbh"))
                .customsTariffNumber("940360").brand("Cir4Fun")
                .productType(Map.of("en-GB", productType))
                .uriOfTheProduct(URI.create("https://demo.example/products/" + articleNumber.toLowerCase()))
                .nameOfSupplier("Partner Supplier GmbH")
                .uriOfSupplier(URI.create("https://demo.example/organizations/partner-supplier-gmbh"))
                .build();
        BillOfMaterials bom = BillOfMaterials.builder()
                .addMaterial(Material.builder().name("FSC certified wood").mandatory(true).portion(72.0)
                        .reference(URI.create("urn:cir4fun:material:MAT-WOOD-001")).build())
                .addMaterial(Material.builder().name("Recycled steel fittings").mandatory(true).portion(8.0)
                        .reference(URI.create("urn:cir4fun:material:MAT-STEEL-002")).build())
                .build();
        ChairCharacteristics chair = ChairCharacteristics.builder().length(120, LengthUnit.CENTIMETER)
                .width(90, LengthUnit.CENTIMETER).height(80, LengthUnit.CENTIMETER)
                .weight(24.5, MassUnit.KILOGRAM).color(color).build();
        return Dpp4Fun.builder(header).nameplate(nameplate).billOfMaterials(bom)
                .chairCharacteristics(chair).build();
    }
}
