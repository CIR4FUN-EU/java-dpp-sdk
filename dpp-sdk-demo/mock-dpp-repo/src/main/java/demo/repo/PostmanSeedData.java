package demo.repo;

import dppsdk.core.model.Contact;
import dppsdk.core.model.Documentation;
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Email;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.model.PassportMetadata;
import dppsdk.dpp4fun.model.BillOfMaterials;
import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.Material;
import dppsdk.dpp4fun.model.ProductClassification;
import java.time.LocalDate;
import java.util.UUID;

final class PostmanSeedData {

    static final String DPP_ID = "49192c87-20c8-4b6f-88de-48b56ca4c211";
    static final String PRODUCT_ID = "04012345678901";
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

    private PostmanSeedData() {
    }

    static Dpp4Fun createSeedDpp() {
        return createSeedDpp(DPP_ID, PRODUCT_ID, "Cir4Fun Platform Bed", "Beds", "Bed", "Warm oak", "C4F-DEMO-001");
    }

    static Dpp4Fun createDeleteExampleDpp() {
        return createSeedDpp(DELETE_EXAMPLE_DPP_ID, DELETE_EXAMPLE_PRODUCT_ID,
                "Cir4Fun Delete Example Stool", "Stools", "Stool", "Natural ash", "C4F-DELETE-001");
    }

    static Dpp4Fun createLifecycleDefaultDpp() {
        return createSeedDpp(LIFECYCLE_DEFAULT_DPP_ID, LIFECYCLE_DEFAULT_PRODUCT_ID,
                "Cir4Fun Lifecycle Default Bed", "Beds", "Bed", "Smoked oak", "C4F-LIFECYCLE-001");
    }

    static Dpp4Fun createLifecycleDeleteDpp() {
        return createSeedDpp(LIFECYCLE_DELETE_DPP_ID, LIFECYCLE_DELETE_PRODUCT_ID,
                "Cir4Fun Lifecycle Delete Stool", "Stools", "Stool", "Stone grey", "C4F-LIFECYCLE-DEL-001");
    }

    static Dpp4Fun createFineGrainedDefaultDpp() {
        return createSeedDpp(FINE_GRAINED_DEFAULT_DPP_ID, FINE_GRAINED_DEFAULT_PRODUCT_ID,
                "Cir4Fun Fine Granular Chair", "Chairs", "Chair", "Graphite", "C4F-FINE-001");
    }

    static Dpp4Fun createFineGrainedDeleteDpp() {
        return createSeedDpp(FINE_GRAINED_DELETE_DPP_ID, FINE_GRAINED_DELETE_PRODUCT_ID,
                "Cir4Fun Fine Granular Delete Bench", "Benches", "Bench", "Walnut", "C4F-FINE-DEL-001");
    }

    static Dpp4Fun createRegistryDeleteDpp() {
        return createSeedDpp(REGISTRY_DELETE_DPP_ID, REGISTRY_DELETE_PRODUCT_ID,
                "Cir4Fun Registry Delete Cabinet", "Cabinets", "Cabinet", "Sand", "C4F-REG-DEL-001");
    }

    private static Dpp4Fun createSeedDpp(String dppId, String productId, String productName, String category,
                                         String productType, String color, String articleNumber) {
        DppCore coreDpp = new DppCore.Builder()
                .passportMetadata(new PassportMetadata.Builder()
                        .uniqueProductIdentifier(UUID.fromString(dppId))
                        .addPassportUpdateDate(LocalDate.of(2026, 4, 24))
                        .qrCodeOrDigitalTag("https://demo.example/dpp/" + dppId)
                        .externalDocumentationLink("https://demo.example/docs/furniture")
                        .build())
                .nameplate(new Nameplate.Builder()
                        .gtinCode(productId)
                        .internalArticleNumber(articleNumber)
                        .batchNumber("DEMO-2026-04")
                        .customsTariffNumber("940360")
                        .uriOfTheProduct("https://demo.example/products/" + articleNumber.toLowerCase())
                        .manufacturer(organization("Cir4Fun Furniture GmbH", OrganizationRole.MANUFACTURER))
                        .supplier(organization("Partner Supplier GmbH", OrganizationRole.SUPPLIER))
                        .build())
                .documentation(new Documentation.Builder()
                        .digitalInstructionsLink("https://demo.example/docs/assembly")
                        .safetyInstructionsLink("https://demo.example/docs/safety")
                        .downloadable(true)
                        .availableForYears(10)
                        .paperCopyAvailableOnRequest(true)
                        .build())
                .build();

        return new Dpp4Fun.Builder()
                .coreDpp(coreDpp)
                .classification(new ProductClassification.Builder()
                        .sector("Furniture")
                        .group("Home and office furniture")
                        .category(category)
                        .addTag("cir4fun")
                        .addTag("demo")
                        .build())
                .characteristics(new Characteristics.Builder()
                        .productName(productName)
                        .description("Partner demo product passport")
                        .brand("Cir4Fun")
                        .productType(productType)
                        .dimensions(new Dimensions.Builder()
                                .width(90.0)
                                .height(80.0)
                                .depth(120.0)
                                .unit("cm")
                                .build())
                        .weight(24.5)
                        .color(color)
                        .addFeature("repairable")
                        .addFeature("recyclable")
                        .build())
                .billOfMaterials(new BillOfMaterials.Builder()
                        .addMaterial(new Material.Builder()
                                .name("FSC certified wood")
                                .mandatory(true)
                                .portion(72.0)
                                .reference("MAT-WOOD-001")
                                .build())
                        .addMaterial(new Material.Builder()
                                .name("Recycled steel fittings")
                                .mandatory(true)
                                .portion(8.0)
                                .reference("MAT-STEEL-002")
                                .build())
                        .build())
                .build();
    }

    private static Organization organization(String name, OrganizationRole role) {
        return new Organization.Builder()
                .name(name)
                .gln("4000001000005")
                .uri("https://demo.example/organizations/" + name.toLowerCase().replace(" ", "-"))
                .contact(new Contact.Builder()
                        .organization(name)
                        .email(new Email.Builder()
                                .emailAddress("contact@demo.example")
                                .typeOfEmail("business")
                                .build())
                        .build())
                .role(role)
                .build();
    }
}
