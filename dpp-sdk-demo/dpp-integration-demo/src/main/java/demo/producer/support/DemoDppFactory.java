package demo.producer.support;

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
import java.util.function.Function;

public final class DemoDppFactory {

    public static final String BED_DPP_ID = "9f2c5f7e-77a1-4f8a-a7fb-95f30e9a6c01";
    public static final String CHAIR_DPP_ID = "2e0dbac3-4d3f-4a9a-9dc8-7de3db4b14f2";
    public static final String BED_PRODUCT_ID = "04012345678911";
    public static final String CHAIR_PRODUCT_ID = "04012345678912";

    public Dpp4Fun createValidBedDpp() {
        return createDpp("Cir4Fun Platform Bed", "Beds", "Bed", "Warm oak", BED_DPP_ID, BED_PRODUCT_ID, "C4F-DEMO-011");
    }

    public Dpp4Fun createValidChairDpp() {
        return createDpp("Cir4Fun Office Chair", "Chairs", "Chair", "Graphite", CHAIR_DPP_ID, CHAIR_PRODUCT_ID, "C4F-DEMO-012");
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

    public Dpp4Fun withFreshDppId(Dpp4Fun dpp) {
        String dppId = UUID.randomUUID().toString();
        PassportMetadata updatedMetadata = dpp.getPassportMetadata().toBuilder()
                .uniqueProductIdentifier(UUID.fromString(dppId))
                .qrCodeOrDigitalTag("https://demo.example/dpp/" + dppId)
                .build();
        DppCore updatedCoreDpp = dpp.getCoreDpp().toBuilder()
                .passportMetadata(updatedMetadata)
                .build();
        return dpp.toBuilder()
                .coreDpp(updatedCoreDpp)
                .build();
    }

    public String createMalformedJson() {
        return "{\n  \"passportMetadata\": {\n    \"uniqueProductIdentifier\": \"not-closed\"\n";
    }

    public static String idOf(Dpp4Fun dpp) {
        return dpp.getPassportMetadata().getUniqueProductIdentifier().toString();
    }

    public static Function<Dpp4Fun, String> idExtractor() {
        return DemoDppFactory::idOf;
    }

    private Dpp4Fun createDpp(
            String productName,
            String category,
            String productType,
            String color,
            String id,
            String gtinCode,
            String internalArticleNumber
    ) {
        DppCore coreDpp = new DppCore.Builder()
                .passportMetadata(metadata(id))
                .nameplate(nameplate(gtinCode, internalArticleNumber))
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

    private Nameplate nameplate(String gtinCode, String internalArticleNumber) {
        return new Nameplate.Builder()
                .gtinCode(gtinCode)
                .internalArticleNumber(internalArticleNumber)
                .batchNumber("DEMO-2026-04")
                .customsTariffNumber("940360")
                .uriOfTheProduct("https://demo.example/products/" + internalArticleNumber.toLowerCase())
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
