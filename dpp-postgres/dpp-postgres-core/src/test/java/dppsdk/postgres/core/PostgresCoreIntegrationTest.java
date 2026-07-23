package dppsdk.postgres.core;

import dppsdk.core.model.Address;
import dppsdk.core.model.Contact;
import dppsdk.core.model.Documentation;
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Email;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.model.PassportMetadata;
import dppsdk.core.model.Telephone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgresCoreIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void coreMapperAndLifecycleRepositoryRoundTripNestedCoreData() throws Exception {
        DataSource dataSource = dataSource();
        PostgresDppVersionRepositorySupport versionSupport = new PostgresDppVersionRepositorySupport();
        PostgresDppCoreMapper coreMapper = new PostgresDppCoreMapper();
        PostgresLifecycleEventRepository lifecycleRepository = new PostgresLifecycleEventRepository();

        DppCore core = new DppCore.Builder()
                .passportMetadata(new PassportMetadata.Builder()
                        .uniqueProductIdentifier(UUID.nameUUIDFromBytes("core-roundtrip".getBytes()))
                        .addPassportUpdateDate(LocalDate.of(2026, 1, 1))
                        .qrCodeOrDigitalTag("CORE-QR-001")
                        .externalDocumentationLink("https://manufacturer.example.com/core-docs")
                        .build())
                .nameplate(new Nameplate.Builder()
                        .gtinCode("CORE-GTIN-001")
                        .internalArticleNumber("CORE-ART-001")
                        .batchNumber("CORE-BATCH-001")
                        .manufacturer(new Organization.Builder()
                                .name("Core Manufacturer")
                                .gln("100200300")
                                .uri("https://manufacturer.example.com/core")
                                .role(OrganizationRole.MANUFACTURER)
                                .contact(new Contact.Builder()
                                        .organization("Support Desk")
                                        .address(new Address.Builder()
                                                .country("DE")
                                                .town("Berlin")
                                                .zipCode("10115")
                                                .street("Alexanderplatz 1")
                                                .build())
                                        .email(new Email.Builder()
                                                .emailAddress("support@example.com")
                                                .typeOfEmail("support")
                                                .build())
                                        .telephone(new Telephone.Builder()
                                                .telephoneNumber("+49-30-123456")
                                                .typeOfTelephone("office")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .documentation(new Documentation.Builder()
                        .digitalInstructionsLink("https://manufacturer.example.com/instructions.pdf")
                        .safetyInstructionsLink("https://manufacturer.example.com/safety.pdf")
                        .downloadable(true)
                        .availableForYears(10)
                        .paperCopyAvailableOnRequest(true)
                        .build())
                .build();

        CoreTestDpp dpp = new CoreTestDpp(core);
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            versionSupport.initializeSchema(connection);
            PostgresDppVersionRepositorySupport.VersionRecord version = versionSupport.create(
                    connection,
                    dpp,
                    new PostgresDppOperationContext("op-core-create", createdAt)
            );
            coreMapper.insertVersionData(connection, version.versionId(), core);
            lifecycleRepository.append(connection, dpp.getDppId(), DppLifecycleEventType.DPP_CREATED, createdAt, Map.of("productId", dpp.getProductId()));
            connection.commit();
        }

        try (Connection connection = dataSource.getConnection()) {
            DppCore reloaded = coreMapper.readVersionData(connection, 1L);
            List<DppLifecycleEventRecord> events = lifecycleRepository.findByDppId(connection, dpp.getDppId());
            PostgresDppVersionRepositorySupport.VersionRecord historical =
                    versionSupport.findByDppIdAt(connection, dpp.getDppId(), createdAt.plusSeconds(60)).orElseThrow();
            assertEquals(core, reloaded);
            assertEquals(1, events.size());
            assertEquals(DppLifecycleEventType.DPP_CREATED, events.get(0).eventType());
            assertEquals(Map.of("productId", dpp.getProductId()), events.get(0).data());
            assertEquals(dpp.getDppId(), historical.dppId());
            assertEquals(List.of(dpp.getDppId()), versionSupport.findAllActiveDppIds(connection));
        }
    }

    private DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }

    private static final class CoreTestDpp extends dppsdk.core.model.Dpp {
        private final DppCore core;

        private CoreTestDpp(DppCore core) {
            this.core = core;
        }

        @Override
        public DppCore getCoreDpp() {
            return core;
        }

        @Override
        public String getPassportType() {
            return "Core Test";
        }
    }
}
