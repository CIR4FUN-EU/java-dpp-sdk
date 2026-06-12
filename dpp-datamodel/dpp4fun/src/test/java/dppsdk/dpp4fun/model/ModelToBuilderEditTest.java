package dppsdk.dpp4fun.model;

import dppsdk.core.model.Address;
import dppsdk.core.model.Contact;
import dppsdk.core.model.Email;
import dppsdk.core.model.PassportMetadata;
import dppsdk.core.model.Telephone;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModelToBuilderEditTest {

    @Test
    void leafModelToBuilder_preservesFields() {
        Address address = new Address.Builder()
                .country("DE")
                .zipCode("10115")
                .region("Berlin")
                .town("Berlin")
                .street("Main St 1")
                .build();
        Email email = new Email.Builder()
                .emailAddress("support@example.com")
                .typeOfEmail("support")
                .build();
        Telephone telephone = new Telephone.Builder()
                .telephoneNumber("+49-30-123456")
                .typeOfTelephone("office")
                .build();
        Contact contact = new Contact.Builder()
                .organization("Support Desk")
                .address(address)
                .email(email)
                .telephone(telephone)
                .build();
        Dimensions dimensions = new Dimensions.Builder()
                .width(100.0)
                .height(50.0)
                .depth(30.0)
                .unit("cm")
                .build();
        Material material = new Material.Builder()
                .name("Steel")
                .mandatory(true)
                .portion(2.0)
                .reference("MAT-001")
                .build();
        Component component = new Component.Builder()
                .name("Frame")
                .reference("COMP-001")
                .build();
        Part part = new Part.Builder()
                .name("Leg")
                .mandatory(true)
                .reference("PART-001")
                .build();

        assertEquals(address, address.toBuilder().build());
        assertEquals(email, email.toBuilder().build());
        assertEquals(telephone, telephone.toBuilder().build());
        assertEquals(contact, contact.toBuilder().build());
        assertEquals(dimensions, dimensions.toBuilder().build());
        assertEquals(material, material.toBuilder().build());
        assertEquals(component, component.toBuilder().build());
        assertEquals(part, part.toBuilder().build());
    }

    @Test
    void toBuilderEdits_createModifiedCopiesWithoutChangingOriginal() {
        Address originalAddress = new Address.Builder()
                .country("DE")
                .town("Berlin")
                .street("Main St 1")
                .build();
        Address updatedAddress = originalAddress.toBuilder()
                .town("Hamburg")
                .build();

        Material originalMaterial = new Material.Builder()
                .name("Steel")
                .mandatory(true)
                .portion(2.0)
                .build();
        Material updatedMaterial = originalMaterial.toBuilder()
                .portion(3.0)
                .build();

        assertEquals("Berlin", originalAddress.getTown());
        assertEquals("Hamburg", updatedAddress.getTown());
        assertEquals(2.0, originalMaterial.getPortion());
        assertEquals(3.0, updatedMaterial.getPortion());
    }

    @Test
    void collectionRemoveMethods_createCopiesWithoutChangingOriginal() {
        LocalDate keptDate = LocalDate.now();
        LocalDate removedDate = keptDate.minusDays(1);
        PassportMetadata metadata = new PassportMetadata.Builder()
                .uniqueProductIdentifier(UUID.randomUUID())
                .addPassportUpdateDate(keptDate)
                .addPassportUpdateDate(removedDate)
                .build();
        PassportMetadata metadataWithoutDate = metadata.toBuilder()
                .removePassportUpdateDate(removedDate)
                .build();

        ProductClassification classification = new ProductClassification.Builder()
                .sector("Furniture")
                .category("Beds")
                .addTag("indoor")
                .addTag("premium")
                .build();
        ProductClassification classificationWithoutTag = classification.toBuilder()
                .removeTag("premium")
                .build();

        Characteristics characteristics = new Characteristics.Builder()
                .productName("Demo Bed")
                .addFeature("Feature A")
                .addFeature("Feature B")
                .build();
        Characteristics characteristicsWithoutFeature = characteristics.toBuilder()
                .removeFeature("Feature B")
                .build();

        assertEquals(2, metadata.getPassportUpdateDates().size());
        assertEquals(1, metadataWithoutDate.getPassportUpdateDates().size());
        assertEquals(2, classification.getTags().size());
        assertEquals(1, classificationWithoutTag.getTags().size());
        assertEquals(2, characteristics.getFeatures().size());
        assertEquals(1, characteristicsWithoutFeature.getFeatures().size());
    }

    @Test
    void billOfMaterialsRemoveMethods_createCopiesWithoutChangingOriginal() {
        Material steel = new Material.Builder().name("Steel").portion(2.0).build();
        Material foam = new Material.Builder().name("Foam").portion(1.0).build();
        Component frame = new Component.Builder().name("Frame").build();
        Component cover = new Component.Builder().name("Cover").build();
        Part leg = new Part.Builder().name("Leg").build();
        Part screw = new Part.Builder().name("Screw").build();

        BillOfMaterials original = new BillOfMaterials.Builder()
                .addMaterial(steel)
                .addMaterial(foam)
                .addComponent(frame)
                .addComponent(cover)
                .addPart(leg)
                .addPart(screw)
                .build();

        BillOfMaterials updated = original.toBuilder()
                .removeMaterial(foam)
                .removeComponent(cover)
                .removePart(screw)
                .build();

        assertEquals(2, original.getMaterials().size());
        assertEquals(2, original.getComponents().size());
        assertEquals(2, original.getParts().size());
        assertEquals(1, updated.getMaterials().size());
        assertEquals(1, updated.getComponents().size());
        assertEquals(1, updated.getParts().size());
    }

    @Test
    void aggregateToBuilder_canRemoveDocumentationAndEditNestedData() {
        Dpp4Fun original = TestDataFactory.validDpp();

        Dpp4Fun updated = original.toBuilder()
                .coreDpp(original.getCoreDpp().toBuilder()
                        .documentation(null)
                        .build())
                .characteristics(original.getCharacteristics().toBuilder()
                        .productName("Updated Bed")
                        .build())
                .build();

        assertEquals("Demo Bed", original.getProductName());
        assertEquals("Updated Bed", updated.getProductName());
        assertNotNull(original.getDocumentation());
        assertNull(updated.getDocumentation());
    }
}

