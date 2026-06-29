package dppsdk.postgres.dpp4fun;

import dppsdk.core.model.Address;
import dppsdk.core.model.Contact;
import dppsdk.core.model.Documentation;
import dppsdk.core.model.Email;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.model.PassportMetadata;
import dppsdk.core.model.Telephone;
import dppsdk.dpp4fun.model.BillOfMaterials;
import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Component;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.Material;
import dppsdk.dpp4fun.model.Part;
import dppsdk.dpp4fun.model.ProductClassification;
import dppsdk.postgres.core.DppLifecycleEventRecord;
import dppsdk.postgres.core.DppLifecycleEventType;
import dppsdk.postgres.core.DppPage;
import dppsdk.postgres.core.DppPageRequest;
import dppsdk.postgres.core.PostgresDppOperationContext;
import dppsdk.postgres.core.PostgresDppStatus;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Dpp4FunPostgresRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void createReadAndRoundTripSupportFullAndOptionalAggregates() {
        Dpp4FunPostgresRepository repository = new Dpp4FunPostgresRepository(dataSource());

        Dpp4Fun full = createDpp("full", true, true, true);
        Dpp4Fun minimal = createDpp("minimal", false, false, false);

        repository.create(full, new PostgresDppOperationContext("create-full", Instant.parse("2026-01-01T10:00:00Z")));
        repository.create(minimal, new PostgresDppOperationContext("create-minimal", Instant.parse("2026-01-01T11:00:00Z")));

        assertEquals(full, repository.findCurrentByDppId(full.getDppId()).orElseThrow());
        assertEquals(full, repository.findCurrentByProductId(full.getProductId()).orElseThrow());
        assertTrue(repository.existsActiveByDppId(full.getDppId()));
        assertEquals(minimal, repository.findCurrentByDppId(minimal.getDppId()).orElseThrow());
        assertEquals(Optional.empty(), Optional.ofNullable(repository.findCurrentByDppId("missing").orElse(null)));
    }

    @Test
    void appendVersionHistoricalLookupAndStaleVersionHandlingWork() {
        Dpp4FunPostgresRepository repository = new Dpp4FunPostgresRepository(dataSource());
        Dpp4Fun original = createDpp("history", true, true, false);
        Instant v1Time = Instant.parse("2026-02-01T09:00:00Z");
        Instant v2Time = Instant.parse("2026-02-10T09:00:00Z");

        repository.create(original, new PostgresDppOperationContext("create-history", v1Time));
        Dpp4Fun updated = original.toBuilder()
                .characteristics(original.getCharacteristics().toBuilder()
                        .productName("Updated history product")
                        .brand("History Brand")
                        .build())
                .build();

        repository.appendVersion(updated, 1L, new PostgresDppOperationContext("append-history", v2Time));

        assertEquals(updated, repository.findCurrentByDppId(original.getDppId()).orElseThrow());
        assertEquals(original, repository.findByProductIdAt(original.getProductId(), Instant.parse("2026-02-05T00:00:00Z")).orElseThrow());
        assertEquals(updated, repository.findByProductIdAt(original.getProductId(), Instant.parse("2026-02-15T00:00:00Z")).orElseThrow());
        assertThrows(IllegalStateException.class, () ->
                repository.appendVersion(updated, 1L, new PostgresDppOperationContext("stale-history", Instant.parse("2026-02-20T09:00:00Z"))));

        List<Dpp4FunVersionSummary> history = repository.findHistoryByDppId(original.getDppId());
        assertEquals(2, history.size());
        assertEquals(PostgresDppStatus.SUPERSEDED, history.get(0).status());
        assertEquals(PostgresDppStatus.ACTIVE, history.get(1).status());
    }

    @Test
    void softDeletePreservesHistoryAndLifecycleEvents() {
        Dpp4FunPostgresRepository repository = new Dpp4FunPostgresRepository(dataSource());
        Dpp4Fun dpp = createDpp("delete", true, true, false);
        Instant createdAt = Instant.parse("2026-03-01T08:00:00Z");
        Instant updatedAt = Instant.parse("2026-03-02T08:00:00Z");
        Instant deletedAt = Instant.parse("2026-03-03T08:00:00Z");

        repository.create(dpp, new PostgresDppOperationContext("create-delete", createdAt));
        Dpp4Fun updated = dpp.toBuilder()
                .characteristics(dpp.getCharacteristics().toBuilder().productName("Delete v2").build())
                .build();
        repository.appendVersion(updated, 1L, new PostgresDppOperationContext("append-delete", updatedAt));
        repository.recordLifecycleEvent(dpp.getDppId(), DppLifecycleEventType.DATA_ELEMENT_UPDATED, Instant.parse("2026-03-02T10:00:00Z"), Map.of("elementPath", "characteristics.productName"));
        repository.softDelete(dpp.getDppId(), 2L, deletedAt);

        assertFalse(repository.existsActiveByDppId(dpp.getDppId()));
        assertEquals(Optional.empty(), repository.findCurrentByDppId(dpp.getDppId()));
        assertEquals(Optional.empty(), repository.findCurrentByProductId(dpp.getProductId()));
        assertEquals(updated, repository.findByProductIdAt(dpp.getProductId(), Instant.parse("2026-03-02T12:00:00Z")).orElseThrow());
        assertEquals(Optional.empty(), repository.findByProductIdAt(dpp.getProductId(), Instant.parse("2026-03-04T00:00:00Z")));

        List<Dpp4FunVersionSummary> history = repository.findHistoryByDppId(dpp.getDppId());
        assertEquals(2, history.size());
        assertEquals(PostgresDppStatus.DELETED, history.get(1).status());

        List<DppLifecycleEventRecord> events = repository.findEventsByDppId(dpp.getDppId());
        assertEquals(4, events.size());
        assertEquals(DppLifecycleEventType.DPP_CREATED, events.get(0).eventType());
        assertEquals(DppLifecycleEventType.DPP_UPDATED, events.get(1).eventType());
        assertEquals(DppLifecycleEventType.DATA_ELEMENT_UPDATED, events.get(2).eventType());
        assertEquals(Map.of("elementPath", "characteristics.productName"), events.get(2).data());
        assertEquals(DppLifecycleEventType.DPP_DELETED, events.get(3).eventType());
    }

    @Test
    void batchLookupAndSearchReturnExpectedProjectionData() {
        Dpp4FunPostgresRepository repository = new Dpp4FunPostgresRepository(dataSource());
        Dpp4Fun chair = createSearchDpp("chair", "Seating", "Chair", "ChairBrand", "Chair", "Oak", "Leg Assembly", "Seat Panel");
        Dpp4Fun bed = createSearchDpp("bed", "Beds", "Bed", "SleepBrand", "Bed", "Steel", "Frame Assembly", "Mattress Support");
        Dpp4Fun sofa = createSearchDpp("sofa", "Sofas", "Sofa", "LoungeBrand", "Sofa", "Foam", "Arm Assembly", "Seat Cushion");

        repository.create(chair, new PostgresDppOperationContext("create-chair", Instant.parse("2026-04-01T10:00:00Z")));
        repository.create(bed, new PostgresDppOperationContext("create-bed", Instant.parse("2026-04-01T10:01:00Z")));
        repository.create(sofa, new PostgresDppOperationContext("create-sofa", Instant.parse("2026-04-01T10:02:00Z")));

        DppPage<String> firstPage = repository.findActiveDppIdsByProductIds(
                List.of(chair.getProductId(), "missing", bed.getProductId(), sofa.getProductId()),
                new DppPageRequest(null, 2)
        );
        DppPage<String> secondPage = repository.findActiveDppIdsByProductIds(
                List.of(chair.getProductId(), "missing", bed.getProductId(), sofa.getProductId()),
                new DppPageRequest(firstPage.nextCursor(), 2)
        );

        assertEquals(List.of(chair.getDppId(), bed.getDppId()), firstPage.items());
        assertNotNull(firstPage.nextCursor());
        assertEquals(List.of(sofa.getDppId()), secondPage.items());

        List<Dpp4FunSearchResult> byCategory = repository.search(new Dpp4FunSearchCriteria(null, "Beds", null, null, null, null, null, 10, 0));
        List<Dpp4FunSearchResult> byBrand = repository.search(new Dpp4FunSearchCriteria(null, null, "ChairBrand", null, null, null, null, 10, 0));
        List<Dpp4FunSearchResult> byProductType = repository.search(new Dpp4FunSearchCriteria(null, null, null, "Sofa", null, null, null, 10, 0));
        List<Dpp4FunSearchResult> byMaterial = repository.search(new Dpp4FunSearchCriteria(null, null, null, null, "Steel", null, null, 10, 0));
        List<Dpp4FunSearchResult> byComponent = repository.search(new Dpp4FunSearchCriteria(null, null, null, null, null, "Arm Assembly", null, 10, 0));
        List<Dpp4FunSearchResult> byPart = repository.search(new Dpp4FunSearchCriteria(null, null, null, null, null, null, "Seat Panel", 10, 0));

        assertEquals(bed.getDppId(), byCategory.get(0).dppId());
        assertEquals(chair.getDppId(), byBrand.get(0).dppId());
        assertEquals(sofa.getDppId(), byProductType.get(0).dppId());
        assertEquals(bed.getDppId(), byMaterial.get(0).dppId());
        assertEquals(sofa.getDppId(), byComponent.get(0).dppId());
        assertEquals(chair.getDppId(), byPart.get(0).dppId());
    }

    private DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }

    private Dpp4Fun createDpp(String suffix, boolean includeDocumentation, boolean includeBom, boolean includeContact) {
        Dpp4Fun base = TestDataFactory.validDpp();
        PassportMetadata passportMetadata = new PassportMetadata.Builder()
                .uniqueProductIdentifier(UUID.nameUUIDFromBytes(("dpp-" + suffix).getBytes()))
                .addPassportUpdateDate(LocalDate.of(2026, 1, 1))
                .qrCodeOrDigitalTag("QR-" + suffix)
                .externalDocumentationLink(includeDocumentation ? "https://example.com/" + suffix + "/external" : null)
                .build();

        Organization manufacturer = includeContact
                ? createOrganization("Manufacturer " + suffix, OrganizationRole.MANUFACTURER, suffix)
                : TestDataFactory.validManufacturer().toBuilder().name("Manufacturer " + suffix).build();
        Organization supplier = includeContact
                ? createOrganization("Supplier " + suffix, OrganizationRole.SUPPLIER, suffix + "-supplier")
                : null;

        Nameplate nameplate = base.getNameplate().toBuilder()
                .gtinCode("GTIN-" + suffix)
                .internalArticleNumber("ARTICLE-" + suffix)
                .batchNumber("BATCH-" + suffix)
                .manufacturer(manufacturer)
                .supplier(supplier)
                .build();

        Documentation documentation = includeDocumentation
                ? TestDataFactory.validDocumentation().toBuilder()
                .digitalInstructionsLink("https://example.com/" + suffix + "/instructions.pdf")
                .safetyInstructionsLink("https://example.com/" + suffix + "/safety.pdf")
                .paperCopyAvailableOnRequest(true)
                .build()
                : null;

        BillOfMaterials billOfMaterials = includeBom
                ? TestDataFactory.validBillOfMaterials().toBuilder().build()
                : null;

        return base.toBuilder()
                .coreDpp(base.getCoreDpp().toBuilder()
                        .passportMetadata(passportMetadata)
                        .nameplate(nameplate)
                        .documentation(documentation)
                        .build())
                .characteristics(base.getCharacteristics().toBuilder()
                        .productName("Product " + suffix)
                        .brand("Brand " + suffix)
                        .build())
                .billOfMaterials(billOfMaterials)
                .build();
    }

    private Dpp4Fun createSearchDpp(
            String suffix,
            String category,
            String productName,
            String brand,
            String productType,
            String materialName,
            String componentName,
            String partName
    ) {
        return createDpp(suffix, true, true, false).toBuilder()
                .classification(new ProductClassification.Builder()
                        .sector("Furniture")
                        .group("Home")
                        .category(category)
                        .addTag(suffix)
                        .build())
                .characteristics(new Characteristics.Builder()
                        .productName(productName)
                        .description(productName + " description")
                        .brand(brand)
                        .productType(productType)
                        .dimensions(new Dimensions.Builder().width(200.0).height(80.0).depth(50.0).unit("cm").build())
                        .weight(45.0)
                        .addFeature(productName + " feature")
                        .build())
                .billOfMaterials(new BillOfMaterials.Builder()
                        .addMaterial(new Material.Builder().name(materialName).mandatory(true).portion(5.0).reference("MAT-" + suffix).build())
                        .addComponent(new Component.Builder().name(componentName).reference("COMP-" + suffix).build())
                        .addPart(new Part.Builder().name(partName).mandatory(true).reference("PART-" + suffix).build())
                        .build())
                .build();
    }

    private Organization createOrganization(String name, OrganizationRole role, String suffix) {
        return new Organization.Builder()
                .name(name)
                .gln("GLN-" + suffix)
                .uri("https://example.com/" + suffix)
                .role(role)
                .contact(new Contact.Builder()
                        .organization(name + " Contact")
                        .address(new Address.Builder()
                                .country("DE")
                                .town("Berlin")
                                .zipCode("10115")
                                .street("Street " + suffix)
                                .build())
                        .email(new Email.Builder()
                                .emailAddress(suffix + "@example.com")
                                .typeOfEmail("support")
                                .build())
                        .telephone(new Telephone.Builder()
                                .telephoneNumber("+49-30-" + Math.abs(suffix.hashCode()))
                                .typeOfTelephone("office")
                                .build())
                        .build())
                .build();
    }
}
