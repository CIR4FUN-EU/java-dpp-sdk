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

    private PostmanSeedData() {
    }

    static Dpp4Fun createSeedDpp() {
        DppCore coreDpp = new DppCore.Builder()
                .passportMetadata(new PassportMetadata.Builder()
                        .uniqueProductIdentifier(UUID.fromString(DPP_ID))
                        .addPassportUpdateDate(LocalDate.of(2026, 4, 24))
                        .qrCodeOrDigitalTag("https://demo.example/dpp/" + DPP_ID)
                        .externalDocumentationLink("https://demo.example/docs/furniture")
                        .build())
                .nameplate(new Nameplate.Builder()
                        .gtinCode(PRODUCT_ID)
                        .internalArticleNumber("C4F-DEMO-001")
                        .batchNumber("DEMO-2026-04")
                        .customsTariffNumber("940360")
                        .uriOfTheProduct("https://demo.example/products/c4f-demo-001")
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
                        .category("Beds")
                        .addTag("cir4fun")
                        .addTag("demo")
                        .build())
                .characteristics(new Characteristics.Builder()
                        .productName("Cir4Fun Platform Bed")
                        .description("Partner demo product passport")
                        .brand("Cir4Fun")
                        .productType("Bed")
                        .dimensions(new Dimensions.Builder()
                                .width(90.0)
                                .height(80.0)
                                .depth(120.0)
                                .unit("cm")
                                .build())
                        .weight(24.5)
                        .color("Warm oak")
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
