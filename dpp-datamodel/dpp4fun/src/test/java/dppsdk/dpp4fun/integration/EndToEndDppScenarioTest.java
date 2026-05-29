package dppsdk.dpp4fun.integration;

import dppsdk.dpp4fun.mapper.Dpp4FunMapper;
import dppsdk.dpp4fun.model.BillOfMaterials;
import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.core.model.Documentation;
import dppsdk.core.model.DppCore;
import dppsdk.dpp4fun.model.Material;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.model.PassportMetadata;
import dppsdk.dpp4fun.model.ProductClassification;
import dppsdk.dpp4fun.payload.Dpp4FunPayload;
import dppsdk.core.validation.ValidationException;
import dppsdk.core.validation.DppCoreValidator;
import dppsdk.dpp4fun.validation.Dpp4FunValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EndToEndDppScenarioTest {

    @Test
    void completeDpp_buildsValidatesMapsAndExposesKeyValues() {
        PassportMetadata metadata = new PassportMetadata.Builder()
                .uniqueProductIdentifier(UUID.randomUUID())
                .addPassportUpdateDate(LocalDate.now())
                .qrCodeOrDigitalTag("QR-E2E-001")
                .externalDocumentationLink("https://example.com/docs")
                .build();
        Organization manufacturer = new Organization.Builder()
                .name("Example Manufacturer GmbH")
                .gln("123456789")
                .uri("https://manufacturer.example.com")
                .role(OrganizationRole.MANUFACTURER)
                .build();
        Nameplate nameplate = new Nameplate.Builder()
                .gtinCode("GTIN-E2E-001")
                .manufacturer(manufacturer)
                .build();
        Documentation documentation = new Documentation.Builder()
                .digitalInstructionsLink("https://example.com/instructions.pdf")
                .safetyInstructionsLink("https://example.com/safety.pdf")
                .downloadable(true)
                .availableForYears(10)
                .build();
        DppCore core = new DppCore.Builder()
                .passportMetadata(metadata)
                .nameplate(nameplate)
                .documentation(documentation)
                .build();
        ProductClassification classification = new ProductClassification.Builder()
                .sector("Furniture")
                .group("Bedroom")
                .category("Beds")
                .addTag("indoor")
                .build();
        Characteristics characteristics = new Characteristics.Builder()
                .productName("Example Bed")
                .productType("Bed")
                .brand("Dpp4Fun")
                .dimensions(new Dimensions.Builder()
                        .width(200.0)
                        .height(90.0)
                        .depth(50.0)
                        .unit("cm")
                        .build())
                .addFeature("Adjustable frame")
                .build();
        Material material = new Material.Builder()
                .name("Steel")
                .mandatory(true)
                .portion(2.5)
                .reference("MAT-001")
                .build();
        BillOfMaterials billOfMaterials = new BillOfMaterials.Builder()
                .addMaterial(material)
                .build();

        Dpp4Fun dpp = new Dpp4Fun.Builder()
                .coreDpp(core)
                .classification(classification)
                .characteristics(characteristics)
                .billOfMaterials(billOfMaterials)
                .build();

        assertDoesNotThrow(() -> new Dpp4FunValidator().validate(dpp));

        Dpp4FunPayload payload = new Dpp4FunMapper().toPayload(dpp);
        Dpp4Fun roundTripped = new Dpp4FunMapper().toDomain(payload);

        assertEquals(dpp, roundTripped);
        assertEquals("Dpp4Fun Furniture", dpp.getPassportType());
        assertEquals(metadata, dpp.getPassportMetadata());
        assertEquals(nameplate, dpp.getNameplate());
        assertEquals(documentation, dpp.getDocumentation());
        assertEquals(metadata.getUniqueProductIdentifier(), dpp.getUniqueProductIdentifier());
        assertEquals("GTIN-E2E-001", dpp.getGtinCode());
        assertEquals("Example Bed", dpp.getProductName());
        assertEquals("Beds", dpp.getCategory());
        assertEquals("Bed", dpp.getProductType());
        assertNotNull(dpp.getBillOfMaterials());
    }

    @Test
    void editAndDeleteByCopy_stillWorksInEndToEndObject() {
        Dpp4Fun original = new Dpp4Fun.Builder()
                .coreDpp(new DppCore.Builder()
                        .passportMetadata(new PassportMetadata.Builder()
                                .uniqueProductIdentifier(UUID.randomUUID())
                                .addPassportUpdateDate(LocalDate.now())
                                .build())
                        .nameplate(new Nameplate.Builder()
                                .gtinCode("GTIN-EDIT-001")
                                .manufacturer(new Organization.Builder()
                                        .name("Example Manufacturer GmbH")
                                        .role(OrganizationRole.MANUFACTURER)
                                        .build())
                                .build())
                        .documentation(new Documentation.Builder()
                                .digitalInstructionsLink("https://example.com/instructions.pdf")
                                .build())
                        .build())
                .classification(new ProductClassification.Builder()
                        .sector("Furniture")
                        .category("Beds")
                        .addTag("old-tag")
                        .build())
                .characteristics(new Characteristics.Builder()
                        .productName("Original Bed")
                        .productType("Bed")
                        .addFeature("Original feature")
                        .build())
                .billOfMaterials(new BillOfMaterials.Builder()
                        .addMaterial(new Material.Builder().name("Steel").portion(2.5).build())
                        .build())
                .build();

        Dpp4Fun edited = original.toBuilder()
                .coreDpp(original.getCoreDpp().toBuilder()
                        .documentation(null)
                        .build())
                .classification(original.getClassification().toBuilder()
                        .removeTag("old-tag")
                        .addTag("new-tag")
                        .build())
                .characteristics(original.getCharacteristics().toBuilder()
                        .productName("Edited Bed")
                        .removeFeature("Original feature")
                        .addFeature("Edited feature")
                        .build())
                .billOfMaterials(original.getBillOfMaterials().toBuilder()
                        .removeMaterial(original.getBillOfMaterials().getMaterials().get(0))
                        .build())
                .build();

        assertEquals("Original Bed", original.getProductName());
        assertEquals("Edited Bed", edited.getProductName());
        assertEquals(1, original.getTags().size());
        assertEquals(1, edited.getTags().size());
        assertEquals("new-tag", edited.getTags().get(0));
        assertEquals(1, original.getBillOfMaterials().getMaterials().size());
        assertEquals(0, edited.getBillOfMaterials().getMaterials().size());
        assertNotNull(original.getDocumentation());
        assertNull(edited.getDocumentation());
    }

    @Test
    void missingCoreDataValidationFailsClearly() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> new DppCoreValidator().validate(null));

        assertEquals("DppCore cannot be null", exception.getMessage());
    }
}

