package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.dpp4fun.model.composite.Address;
import dppsdk.dpp4fun.model.composite.Contact;
import dppsdk.dpp4fun.model.composite.Email;
import dppsdk.dpp4fun.model.composite.Telephone;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

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
        ChairCharacteristics characteristics = ChairCharacteristics.builder()
                .length(100.0, LengthUnit.CENTIMETER)
                .width(50.0, LengthUnit.CENTIMETER)
                .height(30.0, LengthUnit.CENTIMETER)
                .build();
        Material material = new Material.Builder()
                .name("Steel")
                .mandatory(true)
                .portion(2.0)
                .reference(java.net.URI.create("urn:material:MAT-001"))
                .build();
        Component component = new Component.Builder()
                .name("Frame")
                .reference(java.net.URI.create("urn:component:COMP-001"))
                .build();
        Part part = new Part.Builder()
                .name("Leg")
                .mandatory(true)
                .reference(java.net.URI.create("urn:part:PART-001"))
                .build();

        assertEquals(address, address.toBuilder().build());
        assertEquals(email, email.toBuilder().build());
        assertEquals(telephone, telephone.toBuilder().build());
        assertEquals(contact, contact.toBuilder().build());
        assertEquals(characteristics, characteristics.toBuilder().build());
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

        Policies policies = Policies.builder()
                .permissions(java.util.List.of("share", "repair"))
                .prohibitions(java.util.List.of("resell"))
                .build();
        Policies policiesWithoutPermission = policies.toBuilder()
                .permissions(java.util.List.of("repair"))
                .build();

        assertEquals(java.util.List.of("share", "repair"), policies.getPermissions());
        assertEquals(java.util.List.of("repair"), policiesWithoutPermission.getPermissions());
    }

    @Test
    void billOfMaterialsRemoveMethods_createCopiesWithoutChangingOriginal() {
        Material steel = new Material.Builder().name("Steel").portion(2.0).build();
        Material foam = new Material.Builder().name("Foam").portion(1.0).build();
        Component frame = new Component.Builder().name("Frame").build();
        Component cover = new Component.Builder().name("Cover").build();
        Part leg = new Part.Builder().name("Leg").build();
        Part screw = new Part.Builder().name("Screw").build();

        BillOfMaterials original = BillOfMaterials.builder()
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
    void aggregateToBuilder_editsNestedTypedDataWithoutChangingOriginal() {
        Dpp4Fun original = TestDataFactory.validDpp();

        Dpp4Fun updated = original.toBuilder()
                .characteristics(original.getChairCharacteristics().toBuilder()
                        .color("green")
                        .build())
                .build();

        assertEquals("navy", original.getChairCharacteristics().getColor());
        assertEquals("green", updated.getChairCharacteristics().getColor());
    }
}

