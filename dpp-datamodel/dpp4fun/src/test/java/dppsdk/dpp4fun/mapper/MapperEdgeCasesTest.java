package dppsdk.dpp4fun.mapper;

import dppsdk.core.mapper.MappingException;
import dppsdk.core.mapper.NameplateMapper;
import dppsdk.core.mapper.OrganizationMapper;
import dppsdk.core.mapper.PassportMetadataMapper;
import dppsdk.core.model.Contact;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.core.model.Email;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.model.PassportMetadata;
import dppsdk.core.model.Telephone;
import dppsdk.core.payload.OrganizationPayload;
import dppsdk.core.payload.PassportMetadataPayload;
import dppsdk.core.payload.NameplatePayload;
import dppsdk.dpp4fun.model.BillOfMaterials;
import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.ProductClassification;
import dppsdk.dpp4fun.payload.BillOfMaterialsPayload;
import dppsdk.dpp4fun.payload.Dpp4FunPayload;
import dppsdk.dpp4fun.payload.ProductClassificationPayload;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperEdgeCasesTest {

    @Test
    void passportMetadataMapperPreservesUuidDatesAndNullOptionals() {
        PassportMetadata domain = TestDataFactory.validPassportMetadata().toBuilder()
                .qrCodeOrDigitalTag(null)
                .externalDocumentationLink(null)
                .build();

        PassportMetadataMapper mapper = new PassportMetadataMapper();
        PassportMetadataPayload payload = mapper.toPayload(domain);

        assertNull(mapper.toPayload(null));
        assertNull(mapper.toDomain(null));
        assertEquals(domain.getUniqueProductIdentifier().toString(), payload.getUniqueProductIdentifier());
        assertEquals(domain.getPassportUpdateDates().get(0).toString(), payload.getPassportUpdateDates().get(0));
        assertNull(payload.getQrCodeOrDigitalTag());
        assertNull(payload.getExternalDocumentationLink());
        assertEquals(domain, mapper.toDomain(payload));
    }

    @Test
    void productClassificationMapperPreservesCollectionsAndNullHandling() {
        ProductClassification minimal = new ProductClassification.Builder()
                .sector("Furniture")
                .category("Beds")
                .build();

        ProductClassificationMapper mapper = new ProductClassificationMapper();
        ProductClassificationPayload payload = mapper.toPayload(minimal);

        assertNull(mapper.toPayload(null));
        assertNull(mapper.toDomain(null));
        assertEquals("Furniture", payload.getSector());
        assertTrue(payload.getTags().isEmpty());
        assertEquals(minimal, mapper.toDomain(payload));
    }

    @Test
    void organizationMapperPreservesNestedContactAndRoleConversion() {
        Contact contact = new Contact.Builder()
                .organization("Support Desk")
                .address(new dppsdk.core.model.Address.Builder()
                        .country("DE")
                        .town("Berlin")
                        .street("Main St 1")
                        .build())
                .email(new Email.Builder().emailAddress("support@example.com").build())
                .telephone(new Telephone.Builder().telephoneNumber("+49-30-123456").build())
                .build();

        Organization domain = new Organization.Builder()
                .name("Demo Org")
                .uri("https://example.com")
                .contact(contact)
                .role(OrganizationRole.SUPPLIER)
                .build();

        OrganizationMapper mapper = new OrganizationMapper();
        OrganizationPayload payload = mapper.toPayload(domain);

        assertNull(mapper.toPayload(null));
        assertNull(mapper.toDomain(null));
        assertEquals("SUPPLIER", payload.getRole());
        assertEquals("Support Desk", payload.getContact().getOrganization());
        assertEquals(domain, mapper.toDomain(payload));
    }

    @Test
    void nameplateMapperPreservesOptionalPartiesAndNestedOrganizations() {
        Nameplate minimal = new Nameplate.Builder()
                .gtinCode("GTIN-MIN-001")
                .build();

        Nameplate full = new Nameplate.Builder()
                .gtinCode("GTIN-FULL-001")
                .manufacturer(TestDataFactory.validManufacturer())
                .supplier(TestDataFactory.validSupplier())
                .build();

        NameplateMapper mapper = new NameplateMapper();

        NameplatePayload minimalPayload = mapper.toPayload(minimal);
        NameplatePayload fullPayload = mapper.toPayload(full);

        assertNull(minimalPayload.getManufacturer());
        assertNull(minimalPayload.getSupplier());
        assertEquals(minimal, mapper.toDomain(minimalPayload));

        assertEquals("MANUFACTURER", fullPayload.getManufacturer().getRole());
        assertEquals("SUPPLIER", fullPayload.getSupplier().getRole());
        assertEquals(full, mapper.toDomain(fullPayload));
    }

    @Test
    void billOfMaterialsMapperPreservesCollectionsAndEmptyLists() {
        BillOfMaterials empty = new BillOfMaterials.Builder().build();
        BillOfMaterials full = TestDataFactory.validBillOfMaterials();

        BillOfMaterialsMapper mapper = new BillOfMaterialsMapper();
        BillOfMaterialsPayload emptyPayload = mapper.toPayload(empty);
        BillOfMaterialsPayload fullPayload = mapper.toPayload(full);

        assertEquals(0, emptyPayload.getMaterials().size());
        assertEquals(0, emptyPayload.getComponents().size());
        assertEquals(0, emptyPayload.getParts().size());
        assertEquals(empty, mapper.toDomain(emptyPayload));
        assertEquals(full, mapper.toDomain(fullPayload));
    }

    @Test
    void dpp4FunFurnitureDppMapperPreservesTopLevelGraph() {
        Dpp4Fun domain = TestDataFactory.validDpp();
        Dpp4FunMapper mapper = new Dpp4FunMapper();

        Dpp4FunPayload payload = mapper.toPayload(domain);

        assertEquals(domain.getUniqueProductIdentifier().toString(),
                payload.getCoreDpp().getPassportMetadata().getUniqueProductIdentifier());
        assertEquals(domain.getUniqueProductIdentifier().toString(),
                payload.getPassportMetadata().getUniqueProductIdentifier());
        assertEquals(domain.getGtinCode(), payload.getNameplate().getGtinCode());
        assertEquals(domain, mapper.toDomain(payload));
    }

    @Test
    void passportMetadataMapperRejectsMalformedUuidAndDate() {
        PassportMetadataPayload badUuid = new PassportMetadataPayload();
        badUuid.setUniqueProductIdentifier("not-a-uuid");
        badUuid.setPassportUpdateDates(List.of("2026-04-23"));

        PassportMetadataPayload badDate = new PassportMetadataPayload();
        badDate.setUniqueProductIdentifier("1f4d3b6c-8743-47f3-9f1d-2ab3d415dd0f");
        badDate.setPassportUpdateDates(List.of("2026-13-01"));

        PassportMetadataMapper mapper = new PassportMetadataMapper();

        assertThrows(MappingException.class, () -> mapper.toDomain(badUuid));
        assertThrows(MappingException.class, () -> mapper.toDomain(badDate));
    }

    @Test
    void dpp4FunFurnitureDppMapperRejectsMissingRequiredTopLevelData() {
        Dpp4FunPayload payload = new Dpp4FunPayload();
        payload.setPassportMetadata(new PassportMetadataMapper().toPayload(TestDataFactory.validPassportMetadata()));
        payload.setClassification(new ProductClassificationMapper().toPayload(TestDataFactory.validProductClassification()));
        payload.setCharacteristics(new CharacteristicsMapper().toPayload(TestDataFactory.validCharacteristics()));

        MappingException exception = assertThrows(MappingException.class,
                () -> new Dpp4FunMapper().toDomain(payload));

        assertTrue(exception.getMessage().contains("nameplate"));
    }
}

