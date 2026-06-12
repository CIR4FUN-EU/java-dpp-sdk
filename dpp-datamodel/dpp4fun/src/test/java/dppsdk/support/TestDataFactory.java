package dppsdk.support;

import dppsdk.core.model.DppCore;
import dppsdk.core.model.Documentation;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.model.PassportMetadata;
import dppsdk.core.payload.OrganizationPayload;
import dppsdk.core.payload.PassportMetadataPayload;
import dppsdk.dpp4fun.mapper.Dpp4FunMapper;
import dppsdk.dpp4fun.model.BillOfMaterials;
import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.Component;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.dpp4fun.model.Material;
import dppsdk.dpp4fun.model.Part;
import dppsdk.dpp4fun.model.ProductClassification;
import dppsdk.dpp4fun.payload.Dpp4FunPayload;
import dppsdk.dpp4fun.payload.ProductClassificationPayload;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Shared factory for creating valid domain objects in tests.
 *
 * Returns fully-valid, ready-to-use objects.
 * Tests only override the field(s) they care about via toBuilder().
 *
 * Usage:
 *   Characteristics c = TestDataFactory.validCharacteristics()
 *       .toBuilder().weight(-1.0).build(); // override one field for test
 */
public final class TestDataFactory {

    private TestDataFactory() {}

    // Ã¢â€â‚¬Ã¢â€â‚¬ PassportMetadata Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    public static PassportMetadata validPassportMetadata() {
        return new PassportMetadata.Builder()
                .uniqueProductIdentifier(UUID.randomUUID())
                .addPassportUpdateDate(LocalDate.now())
                .qrCodeOrDigitalTag("QR-DEMO-001")
                .externalDocumentationLink("https://example.com/doc")
                .build();
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ ProductClassification Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    public static ProductClassification validProductClassification() {
        return new ProductClassification.Builder()
                .sector("Furniture")
                .group("Seating")
                .category("Beds")
                .addTag("demo")
                .build();
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Dimensions Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    public static Dimensions validDimensions() {
        return new Dimensions.Builder()
                .width(200.0)
                .height(90.0)
                .depth(50.0)
                .unit("cm")
                .build();
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Characteristics Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    public static Characteristics validCharacteristics() {
        return new Characteristics.Builder()
                .productName("Demo Bed")
                .description("A demo product for testing")
                .brand("Dpp4Fun")
                .productType("Bed")
                .dimensions(validDimensions())
                .weight(75.0)
                .addFeature("Feature A")
                .addFeature("Feature B")
                .build();
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Organizations Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    public static Organization validManufacturer() {
        return new Organization.Builder()
                .name("Demo Manufacturer GmbH")
                .gln("123456789")
                .uri("https://manufacturer.example.com")
                .role(OrganizationRole.MANUFACTURER)
                .build();
    }

    public static Organization validSupplier() {
        return new Organization.Builder()
                .name("Demo Supplier Ltd")
                .gln("987654321")
                .uri("https://supplier.example.com")
                .role(OrganizationRole.SUPPLIER)
                .build();
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Nameplate Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    public static Nameplate validNameplate() {
        return new Nameplate.Builder()
                .gtinCode("GTIN-DEMO-001")
                .internalArticleNumber("ART-001")
                .batchNumber("BATCH-2026")
                .manufacturer(validManufacturer())
                .build();
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Documentation Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    public static Documentation validDocumentation() {
        return new Documentation.Builder()
                .digitalInstructionsLink("https://example.com/instructions.pdf")
                .safetyInstructionsLink("https://example.com/safety.pdf")
                .downloadable(true)
                .availableForYears(10)
                .build();
    }

    public static DppCore validDppCore() {
        return new DppCore.Builder()
                .passportMetadata(validPassportMetadata())
                .nameplate(validNameplate())
                .documentation(validDocumentation())
                .build();
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ BillOfMaterials Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    public static BillOfMaterials validBillOfMaterials() {
        return new BillOfMaterials.Builder()
                .addMaterial(new Material.Builder()
                        .name("Steel").mandatory(true).portion(2.5).reference("MAT-001").build())
                .addMaterial(new Material.Builder()
                        .name("Foam").mandatory(true).portion(5.0).reference("MAT-002").build())
                .addComponent(new Component.Builder()
                        .name("Frame Assembly").reference("COMP-001").build())
                .addPart(new Part.Builder()
                        .name("Mattress Support").mandatory(true).reference("PART-001").build())
                .build();
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬ Full DPP Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    public static Dpp4Fun validDpp() {
        return new Dpp4Fun.Builder()
                .coreDpp(validDppCore())
                .classification(validProductClassification())
                .characteristics(validCharacteristics())
                .billOfMaterials(validBillOfMaterials())
                .build();
    }

    public static Dpp4FunPayload validDppPayload() {
        return new Dpp4FunMapper().toPayload(validDpp());
    }

    public static String validDppJson() {
        return new Dpp4FunJsonCodec().toJson(validDpp());
    }

    // ---------------------------------------------------------------------
    // Demo-friendly aliases
    // ---------------------------------------------------------------------

    public static PassportMetadata createValidPassportMetadata() {
        return validPassportMetadata();
    }

    public static ProductClassification createValidClassification() {
        return validProductClassification();
    }

    public static Characteristics createValidCharacteristics() {
        return validCharacteristics();
    }

    public static Nameplate createValidNameplate() {
        return validNameplate();
    }

    public static Organization createValidManufacturer() {
        return validManufacturer();
    }

    public static Organization createValidSupplier() {
        return validSupplier();
    }

    public static Documentation createValidDocumentation() {
        return validDocumentation();
    }

    public static DppCore createValidDppCore() {
        return validDppCore();
    }

    public static BillOfMaterials createValidBillOfMaterials() {
        return validBillOfMaterials();
    }

    public static Dpp4Fun createValidDpp() {
        return validDpp();
    }

    public static Dpp4Fun createValidBedDpp() {
        return validDpp();
    }

    public static Dpp4FunPayload createValidDppPayload() {
        return validDppPayload();
    }

    public static String createValidJson() {
        return validDppJson();
    }

    public static Dpp4Fun createDppWithWrongSupplierRole() {
        return createValidDpp().toBuilder()
                .coreDpp(createValidDpp().getCoreDpp().toBuilder()
                        .nameplate(new Nameplate.Builder()
                                .gtinCode("GTIN-DEMO-WRONG-SUPPLIER")
                                .supplier(new Organization.Builder()
                                        .name("Demo Supplier Ltd")
                                        .gln("987654321")
                                        .uri("https://supplier.example.com")
                                        .role(OrganizationRole.MANUFACTURER)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    public static Dpp4Fun createDppWithoutManufacturer() {
        return createValidDpp().toBuilder()
                .coreDpp(createValidDpp().getCoreDpp().toBuilder()
                        .nameplate(new Nameplate.Builder()
                                .gtinCode("GTIN-DEMO-NO-MANUFACTURER")
                                .supplier(createValidSupplier())
                                .build())
                        .build())
                .build();
    }

    public static Dpp4Fun createDppWithInvalidDocumentation() {
        return createValidDpp().toBuilder()
                .coreDpp(createValidDpp().getCoreDpp().toBuilder()
                        .documentation(new Documentation.Builder()
                                .downloadable(true)
                                .build())
                        .build())
                .build();
    }

    public static Dpp4Fun createDppWithInvalidDimensions() {
        return createValidDpp().toBuilder()
                .characteristics(createValidCharacteristics().toBuilder()
                        .dimensions(new Dimensions.Builder()
                                .width(200.0)
                                .height(90.0)
                                .depth(50.0)
                                // Builder allows a missing unit, but the semantic validator rejects it.
                                .build())
                        .build())
                .build();
    }

    public static Dpp4Fun createDppWithDuplicateTags() {
        return createValidDpp().toBuilder()
                .classification(new ProductClassification.Builder()
                        .sector("Furniture")
                        .group("Seating")
                        .category("Beds")
                        .addTag("demo")
                        .addTag("demo")
                        .build())
                .build();
    }

    public static PassportMetadataPayload createPayloadWithInvalidUuid() {
        PassportMetadataPayload payload = new PassportMetadataPayload();
        payload.setUniqueProductIdentifier("not-a-uuid");
        payload.setPassportUpdateDates(List.of(LocalDate.now().toString()));
        payload.setQrCodeOrDigitalTag("QR-DEMO-001");
        payload.setExternalDocumentationLink("https://example.com/doc");
        return payload;
    }

    public static PassportMetadataPayload createPayloadWithInvalidDate() {
        PassportMetadataPayload payload = new PassportMetadataPayload();
        payload.setUniqueProductIdentifier(UUID.randomUUID().toString());
        payload.setPassportUpdateDates(List.of("2026-13-01"));
        payload.setQrCodeOrDigitalTag("QR-DEMO-001");
        payload.setExternalDocumentationLink("https://example.com/doc");
        return payload;
    }

    public static OrganizationPayload createPayloadWithInvalidRole() {
        OrganizationPayload payload = new OrganizationPayload();
        payload.setName("Demo Manufacturer GmbH");
        payload.setGln("123456789");
        payload.setUri("https://manufacturer.example.com");
        payload.setRole("BROKER");
        return payload;
    }

    public static Dpp4FunPayload createPayloadMissingRequiredFields() {
        Dpp4FunPayload payload = createValidDppPayload();
        payload.setNameplate(null);
        return payload;
    }

    public static String createMalformedJson() {
        return "{not-json";
    }

    public static String createJsonWithInvalidRole() {
        return createValidJson().replace("\"MANUFACTURER\"", "\"BROKER\"");
    }

    public static String createJsonWithInvalidUuid() {
        Dpp4Fun valid = createValidDpp();
        String json = new Dpp4FunJsonCodec().toJson(valid);
        return json.replace(valid.getUniqueProductIdentifier().toString(), "not-a-uuid");
    }

    public static String createJsonWithInvalidDate() {
        Dpp4Fun valid = createValidDpp();
        String json = new Dpp4FunJsonCodec().toJson(valid);
        String validDate = valid.getPassportUpdateDates().get(0).toString();
        return json.replace(validDate, "2026-13-01");
    }
}

