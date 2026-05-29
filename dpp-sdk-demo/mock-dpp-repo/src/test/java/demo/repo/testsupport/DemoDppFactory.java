package demo.repo.testsupport;

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

public final class DemoDppFactory {

    public static final String BED_DPP_ID = "49192c87-20c8-4b6f-88de-48b56ca4c211";
    public static final String CHAIR_DPP_ID = "b2368516-3312-420a-9f71-55a51240c229";

    public Dpp4Fun createValidBedDpp() {
        return createDpp("Cir4Fun Platform Bed", "Beds", "Bed", "Warm oak", BED_DPP_ID);
    }

    public Dpp4Fun createValidChairDpp() {
        return createDpp("Cir4Fun Office Chair", "Chairs", "Chair", "Graphite", CHAIR_DPP_ID);
    }

    public Dpp4Fun createUpdatedBedDpp() {
        return withProductName(createValidBedDpp(), "Cir4Fun Platform Bed - Updated Demo");
    }

    public Dpp4Fun createDppWithWrongSupplierRole() {
        Dpp4Fun valid = createValidChairDpp();
        Nameplate nameplate = valid.getNameplate().toBuilder()
                .supplier(organization("Partner Distribution GmbH", OrganizationRole.DISTRIBUTOR))
                .build();
        DppCore coreDpp = valid.getCoreDpp().toBuilder()
                .nameplate(nameplate)
                .build();
        return valid.toBuilder().coreDpp(coreDpp).build();
    }

    public Dpp4Fun createDppWithInvalidDocumentation() {
        Dpp4Fun valid = createValidBedDpp();
        Documentation invalidDocumentation = new Documentation.Builder()
                .downloadable(true)
                .availableForYears(5)
                .build();
        DppCore coreDpp = valid.getCoreDpp().toBuilder()
                .documentation(invalidDocumentation)
                .build();
        return valid.toBuilder()
                .coreDpp(coreDpp)
                .build();
    }

    public Dpp4Fun withProductName(Dpp4Fun dpp, String productName) {
        Characteristics updatedCharacteristics = dpp.getCharacteristics().toBuilder()
                .productName(productName)
                .build();
        return dpp.toBuilder()
                .characteristics(updatedCharacteristics)
                .build();
    }

    public String createMalformedJson() {
        return "{\n  \"passportMetadata\": {\n    \"uniqueProductIdentifier\": \"not-closed\"\n";
    }

    private Dpp4Fun createDpp(String productName, String category, String productType, String color, String id) {
        DppCore coreDpp = new DppCore.Builder()
                .passportMetadata(metadata(id))
                .nameplate(nameplate())
                .documentation(documentation())
                .build();

        return new Dpp4Fun.Builder()
                .coreDpp(coreDpp)
                .classification(classification(category))
                .characteristics(characteristics(productName, productType, color))
                .billOfMaterials(billOfMaterials())
                .build();
    }

    private PassportMetadata metadata(String id) {
        return new PassportMetadata.Builder()
                .uniqueProductIdentifier(UUID.fromString(id))
                .addPassportUpdateDate(LocalDate.of(2026, 4, 24))
                .qrCodeOrDigitalTag("https://demo.example/dpp/" + id)
                .externalDocumentationLink("https://demo.example/docs/furniture")
                .build();
    }

    private ProductClassification classification(String category) {
        return new ProductClassification.Builder()
                .sector("Furniture")
                .group("Home and office furniture")
                .category(category)
                .addTag("cir4fun")
                .addTag("demo")
                .build();
    }

    private Characteristics characteristics(String productName, String productType, String color) {
        return new Characteristics.Builder()
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
                .build();
    }

    private Nameplate nameplate() {
        return new Nameplate.Builder()
                .gtinCode("04012345678901")
                .internalArticleNumber("C4F-DEMO-001")
                .batchNumber("DEMO-2026-04")
                .customsTariffNumber("940360")
                .uriOfTheProduct("https://demo.example/products/c4f-demo-001")
                .manufacturer(organization("Cir4Fun Furniture GmbH", OrganizationRole.MANUFACTURER))
                .supplier(organization("Partner Supplier GmbH", OrganizationRole.SUPPLIER))
                .build();
    }

    private Organization organization(String name, OrganizationRole role) {
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

    private BillOfMaterials billOfMaterials() {
        return new BillOfMaterials.Builder()
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
                .build();
    }

    private Documentation documentation() {
        return new Documentation.Builder()
                .digitalInstructionsLink("https://demo.example/docs/assembly")
                .safetyInstructionsLink("https://demo.example/docs/safety")
                .downloadable(true)
                .availableForYears(10)
                .paperCopyAvailableOnRequest(true)
                .build();
    }
}
