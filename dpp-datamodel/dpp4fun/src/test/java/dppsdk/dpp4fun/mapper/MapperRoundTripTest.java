package dppsdk.dpp4fun.mapper;

import dppsdk.core.mapper.AddressMapper;
import dppsdk.core.mapper.ContactMapper;
import dppsdk.core.mapper.DocumentationMapper;
import dppsdk.core.mapper.DppCoreMapper;
import dppsdk.core.mapper.EmailMapper;
import dppsdk.core.mapper.MappingException;
import dppsdk.core.mapper.NameplateMapper;
import dppsdk.core.mapper.OrganizationMapper;
import dppsdk.core.mapper.PassportMetadataMapper;
import dppsdk.core.mapper.TelephoneMapper;
import dppsdk.core.model.Address;
import dppsdk.core.model.Contact;
import dppsdk.core.model.Documentation;
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Email;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.PassportMetadata;
import dppsdk.core.model.Telephone;
import dppsdk.core.payload.DppCorePayload;
import dppsdk.core.payload.OrganizationPayload;
import dppsdk.core.payload.PassportMetadataPayload;
import dppsdk.dpp4fun.model.BillOfMaterials;
import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.Component;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.dpp4fun.model.Material;
import dppsdk.dpp4fun.model.Part;
import dppsdk.dpp4fun.model.ProductClassification;
import dppsdk.dpp4fun.payload.Dpp4FunPayload;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapperRoundTripTest {

    @Test
    void addressRoundTripPreservesFields() {
        Address domain = new Address.Builder()
                .country("DE")
                .zipCode("10115")
                .region("Berlin")
                .town("Berlin")
                .street("Alexanderplatz 1")
                .build();

        assertEquals(domain, new AddressMapper().toDomain(new AddressMapper().toPayload(domain)));
    }

    @Test
    void emailRoundTripPreservesFields() {
        Email domain = new Email.Builder()
                .emailAddress("team@example.com")
                .typeOfEmail("support")
                .build();

        assertEquals(domain, new EmailMapper().toDomain(new EmailMapper().toPayload(domain)));
    }

    @Test
    void telephoneRoundTripPreservesFields() {
        Telephone domain = new Telephone.Builder()
                .telephoneNumber("+49-30-123456")
                .typeOfTelephone("office")
                .build();

        assertEquals(domain, new TelephoneMapper().toDomain(new TelephoneMapper().toPayload(domain)));
    }

    @Test
    void contactRoundTripPreservesFields() {
        Contact domain = new Contact.Builder()
                .organization("Support Desk")
                .address(new Address.Builder().country("DE").town("Berlin").street("Main St 1").build())
                .email(new Email.Builder().emailAddress("team@example.com").build())
                .telephone(new Telephone.Builder().telephoneNumber("+49-30-123456").build())
                .build();

        assertEquals(domain, new ContactMapper().toDomain(new ContactMapper().toPayload(domain)));
    }

    @Test
    void organizationRoundTripPreservesFields() {
        Organization domain = TestDataFactory.validManufacturer();

        assertEquals(domain, new OrganizationMapper().toDomain(new OrganizationMapper().toPayload(domain)));
    }

    @Test
    void organizationMapperRejectsUnknownRole() {
        OrganizationPayload payload = new OrganizationPayload();
        payload.setName("Bad Role Co");
        payload.setRole("NOT_A_REAL_ROLE");

        assertThrows(MappingException.class, () -> new OrganizationMapper().toDomain(payload));
    }

    @Test
    void dimensionsRoundTripPreservesFields() {
        Dimensions domain = TestDataFactory.validDimensions();

        assertEquals(domain, new DimensionsMapper().toDomain(new DimensionsMapper().toPayload(domain)));
    }

    @Test
    void characteristicsRoundTripPreservesFields() {
        Characteristics domain = TestDataFactory.validCharacteristics();

        assertEquals(domain, new CharacteristicsMapper().toDomain(new CharacteristicsMapper().toPayload(domain)));
    }

    @Test
    void productClassificationRoundTripPreservesFields() {
        ProductClassification domain = TestDataFactory.validProductClassification();

        assertEquals(domain, new ProductClassificationMapper()
                .toDomain(new ProductClassificationMapper().toPayload(domain)));
    }

    @Test
    void passportMetadataRoundTripPreservesConvertedFields() {
        PassportMetadata domain = TestDataFactory.validPassportMetadata();

        PassportMetadataPayload payload = new PassportMetadataMapper().toPayload(domain);

        assertEquals(domain.getUniqueProductIdentifier().toString(), payload.getUniqueProductIdentifier());
        assertEquals(domain.getPassportUpdateDates().get(0).toString(), payload.getPassportUpdateDates().get(0));
        assertEquals(domain, new PassportMetadataMapper().toDomain(payload));
    }

    @Test
    void passportMetadataMapperRejectsInvalidDate() {
        PassportMetadataPayload payload = new PassportMetadataPayload();
        payload.setUniqueProductIdentifier("1f4d3b6c-8743-47f3-9f1d-2ab3d415dd0f");
        payload.setPassportUpdateDates(java.util.List.of("2026-13-01"));

        assertThrows(MappingException.class, () -> new PassportMetadataMapper().toDomain(payload));
    }

    @Test
    void nameplateRoundTripPreservesFields() {
        Nameplate domain = TestDataFactory.validNameplate();

        assertEquals(domain, new NameplateMapper().toDomain(new NameplateMapper().toPayload(domain)));
    }

    @Test
    void materialRoundTripPreservesFields() {
        Material domain = new Material.Builder()
                .name("Steel")
                .mandatory(true)
                .portion(2.5)
                .reference("MAT-001")
                .build();

        assertEquals(domain, new MaterialMapper().toDomain(new MaterialMapper().toPayload(domain)));
    }

    @Test
    void componentRoundTripPreservesFields() {
        Component domain = new Component.Builder()
                .name("Frame")
                .reference("COMP-001")
                .build();

        assertEquals(domain, new ComponentMapper().toDomain(new ComponentMapper().toPayload(domain)));
    }

    @Test
    void partRoundTripPreservesFields() {
        Part domain = new Part.Builder()
                .name("Leg")
                .mandatory(true)
                .reference("PART-001")
                .build();

        assertEquals(domain, new PartMapper().toDomain(new PartMapper().toPayload(domain)));
    }

    @Test
    void billOfMaterialsRoundTripPreservesFields() {
        BillOfMaterials domain = TestDataFactory.validBillOfMaterials();

        assertEquals(domain, new BillOfMaterialsMapper()
                .toDomain(new BillOfMaterialsMapper().toPayload(domain)));
    }

    @Test
    void documentationRoundTripPreservesFields() {
        Documentation domain = TestDataFactory.validDocumentation();

        assertEquals(domain, new DocumentationMapper().toDomain(new DocumentationMapper().toPayload(domain)));
    }

    @Test
    void dppCoreRoundTripThroughCorePayloadPreservesFields() {
        DppCore domain = TestDataFactory.validDppCore();
        DppCorePayload payload = new DppCoreMapper().toPayload(domain);

        assertEquals(domain.getUniqueProductIdentifier().toString(),
                payload.getPassportMetadata().getUniqueProductIdentifier());
        assertEquals(domain, new DppCoreMapper().toDomain(payload));
    }

    @Test
    void topLevelDppRoundTripPreservesObjectGraph() {
        Dpp4Fun domain = TestDataFactory.validDpp().toBuilder()
                .characteristics(TestDataFactory.validCharacteristics().toBuilder()
                        .productType("Beds")
                        .build())
                .build();

        Dpp4FunMapper mapper = new Dpp4FunMapper();
        Dpp4FunPayload payload = mapper.toPayload(domain);

        assertEquals(domain.getUniqueProductIdentifier().toString(),
                payload.getCoreDpp().getPassportMetadata().getUniqueProductIdentifier());
        assertEquals(domain.getUniqueProductIdentifier().toString(),
                payload.getPassportMetadata().getUniqueProductIdentifier());
        assertEquals(domain.getGtinCode(), payload.getNameplate().getGtinCode());
        assertEquals(domain, mapper.toDomain(payload));
    }
}

