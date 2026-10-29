package demo.producer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import demo.producer.support.DemoDppFactory;
import demo.producer.support.RegistryRecordPayload;
import dpp.registry.client.DppRegistryClient;
import demo.registry.MockEuRegistryApplication;
import demo.repo.DemoRepoApplication;
import dpp.registry.client.HttpDppRegistryClient;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.repo.client.HttpDppRepoClient;
import dpp.repo.client.exception.DppApiClientException;
import dpp.repo.payloads.DppRepresentation;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

class RealServicesSmokeTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final Map<String, String> ORIGINAL_DATASOURCE_PROPERTIES = new LinkedHashMap<>();

    @BeforeAll
    static void startPostgresWhenRequested() {
        if (!"postgres".equalsIgnoreCase(System.getProperty("dpp.repo.backend", "memory"))) return;
        POSTGRES.start();
        setDatasourceProperty("spring.datasource.url", POSTGRES.getJdbcUrl());
        setDatasourceProperty("spring.datasource.username", POSTGRES.getUsername());
        setDatasourceProperty("spring.datasource.password", POSTGRES.getPassword());
    }

    @AfterAll
    static void stopPostgresWhenStarted() {
        if (POSTGRES.isRunning()) POSTGRES.stop();
        ORIGINAL_DATASOURCE_PROPERTIES.forEach((key, value) -> {
            if (value == null) System.clearProperty(key);
            else System.setProperty(key, value);
        });
    }

    private final DemoDppFactory factory = new DemoDppFactory();

    private ConfigurableApplicationContext repoContext;
    private ConfigurableApplicationContext registryContext;

    @AfterEach
    void closeContexts() {
        if (registryContext != null) {
            registryContext.close();
            registryContext = null;
        }
        if (repoContext != null) {
            repoContext.close();
            repoContext = null;
        }
    }

    @Test
    @DisplayName("Real repo and registry services support creating a DPP and registering its metadata")
    void realRepoAndRegistrySupportCreateThenRegisterFlow() {
        repoContext = startContext(DemoRepoApplication.class);
        registryContext = startContext(MockEuRegistryApplication.class);
        clearInMemoryStore(repoContext, "inMemoryDppStore");
        clearInMemoryStore(registryContext, "inMemoryRegistryStore");

        String repoUrl = "http://localhost:" + localPort(repoContext);
        String registryUrl = "http://localhost:" + localPort(registryContext);

        HttpDppRepoClient repoClient = new HttpDppRepoClient(repoUrl);
        DppRegistryClient registryClient = new HttpDppRegistryClient(registryUrl);
        MockRegistryLookupClient mockRegistryLookupClient = new MockRegistryLookupClient(
                registryUrl,
                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
        );

        Dpp4Fun dpp = factory.withFreshDppId(factory.createValidBedDpp());
        String dppId = DemoDppFactory.idOf(dpp);
        String productId = dpp.getUniqueProductIdentifier();

        deleteExistingByProductId(repoClient, productId);
        assertTrue(new Dpp4FunValidationService().validate(dpp).isValid());
        assertEquals(dppId, repoClient.createDpp(toJson(dpp)).getDigitalProductPassportId());
        assertEquals("Cir4Fun", repoClient.readDataElement(dppId,
                DemoDppFactory.BRAND_PATH).getPayload().asText());
        assertEquals("FSC certified wood", repoClient.readDataElement(dppId,
                DemoDppFactory.FIRST_MATERIAL_NAME_PATH).getPayload().asText());
        com.fasterxml.jackson.databind.node.ObjectNode changedName = (com.fasterxml.jackson.databind.node.ObjectNode)
                repoClient.readDataElement(dppId, DemoDppFactory.BRAND_PATH, DppRepresentation.FULL).getPayload();
        changedName.put("value", "Fine-granular smoke update");
        assertEquals("Fine-granular smoke update", repoClient.updateDataElement(dppId,
                DemoDppFactory.BRAND_PATH, changedName, DppRepresentation.FULL).getPayload().path("value").asText());
        assertEquals("Fine-granular smoke update", repoClient.readDataElement(dppId,
                DemoDppFactory.BRAND_PATH).getPayload().asText());

        DppApiClientException malformedPath = assertThrows(
                DppApiClientException.class,
                () -> repoClient.readDataElement(dppId, "elements[3].elements[0].value")
        );
        assertEquals(400, malformedPath.httpStatus());
        DppApiClientException noMatch = assertThrows(
                DppApiClientException.class,
                () -> repoClient.readDataElement(dppId, "$.elements[3].elements[99]")
        );
        assertEquals(404, noMatch.httpStatus());
        dpp.repo.client.exception.DppHttpClientException unsupportedWildcard = assertThrows(
                dpp.repo.client.exception.DppHttpClientException.class,
                () -> repoClient.readDataElement(dppId, "$.elements[4].elements[0].value[*]")
        );
        assertEquals(501, unsupportedWildcard.statusCode());

        String registryIdentifier = registryClient.postNewDppToRegistry(
                new RegisterDppRequest(productId, dppId, "operator-123", repoUrl)
        ).getRegistrationId();
        assertFalse(registryIdentifier.isBlank());

        RegistryRecordPayload registryRecord = mockRegistryLookupClient.readByDppId(dppId).orElseThrow();
        assertEquals(registryIdentifier, registryRecord.registryIdentifier());
        assertEquals(dppId, registryRecord.dppIdentifier());
        assertEquals(productId, registryRecord.productIdentifier());
        assertEquals("operator-123", registryRecord.operatorIdentifier());
        assertEquals(repoUrl, registryRecord.repoUrl());
        assertTrue(mockRegistryLookupClient.readByRegistryId(registryIdentifier).isPresent());

        String unreachableEndpoint = "http://127.0.0.1:1/v1";
        String registrationForMissingDpp = registryClient.postNewDppToRegistry(
                new RegisterDppRequest(productId, "missing-dpp-id", "operator-123", unreachableEndpoint)
        ).getRegistrationId();
        assertFalse(registrationForMissingDpp.isBlank());
        RegistryRecordPayload missingDppMetadata = mockRegistryLookupClient.readByDppId("missing-dpp-id").orElseThrow();
        assertEquals(unreachableEndpoint, missingDppMetadata.repoUrl());
    }

    @Test
    @DisplayName("HTTP demo rerun succeeds when a previous aborted run left the demo DPP in the repo")
    void httpDemoRerunSucceedsWhenPreviousRunLeftDemoDppInRepo() {
        repoContext = startContext(DemoRepoApplication.class);
        registryContext = startContext(MockEuRegistryApplication.class);
        clearInMemoryStore(repoContext, "inMemoryDppStore");
        clearInMemoryStore(registryContext, "inMemoryRegistryStore");

        String repoUrl = "http://localhost:" + localPort(repoContext);
        String registryUrl = "http://localhost:" + localPort(registryContext);
        HttpDppRepoClient repoClient = new HttpDppRepoClient(repoUrl);
        MockRegistryLookupClient mockRegistryLookupClient = new MockRegistryLookupClient(
                registryUrl,
                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
        );

        Dpp4Fun dpp = factory.withFreshDppId(factory.createValidBedDpp());
        String dppId = DemoDppFactory.idOf(dpp);
        String productId = dpp.getUniqueProductIdentifier();
        deleteExistingByProductId(repoClient, productId);
        assertTrue(new Dpp4FunValidationService().validate(dpp).isValid());
        repoClient.createDpp(toJson(dpp));

        String demoOutput = captureStandardOut(() ->
                assertDoesNotThrow(() -> new HttpServiceDemoRunner().run(registryUrl, repoUrl)));

        assertTrue(demoOutput.contains("-- Compare compressed and full DPP representations --"));
        assertTrue(demoOutput.contains("COMPRESSED DPP (EN 18223 elementId-keyed JSON):"));
        assertTrue(demoOutput.contains("FULL DPP (expanded JSON):"));
        assertTrue(demoOutput.contains("Nameplate collection @ $.elements[1]:"));
        assertTrue(demoOutput.contains("Brand DataElement (FULL) @ " + DemoDppFactory.BRAND_PATH + ":"));
        assertTrue(demoOutput.contains(DemoDppFactory.BRAND_PATH));
        assertTrue(demoOutput.contains(DemoDppFactory.FIRST_MATERIAL_NAME_PATH));

        DppApiClientException missingOldDpp = assertThrows(
                DppApiClientException.class,
                () -> repoClient.readDppById(dppId)
        );
        assertEquals(404, missingOldDpp.httpStatus());

        DppApiClientException missingByProductAfterRun = assertThrows(
                DppApiClientException.class,
                () -> repoClient.readDppByProductId(productId)
        );
        assertEquals(404, missingByProductAfterRun.httpStatus());
        assertTrue(mockRegistryLookupClient.readByDppId(dppId).isEmpty());
    }

    @Test
    @DisplayName("HTTP demo first run succeeds when the repository has no matching DPP")
    void httpDemoFirstRunSucceedsWhenRepositoryHasNoMatchingDpp() {
        repoContext = startContext(DemoRepoApplication.class);
        registryContext = startContext(MockEuRegistryApplication.class);
        clearInMemoryStore(repoContext, "inMemoryDppStore");
        clearInMemoryStore(registryContext, "inMemoryRegistryStore");

        String repoUrl = "http://localhost:" + localPort(repoContext);
        String registryUrl = "http://localhost:" + localPort(registryContext);
        String demoProductId = factory.createValidBedDpp().getUniqueProductIdentifier();
        HttpDppRepoClient repoClient = new HttpDppRepoClient(repoUrl);

        assertDoesNotThrow(() -> new HttpServiceDemoRunner().run(registryUrl, repoUrl));
        assertEquals(404, assertThrows(DppApiClientException.class,
                () -> repoClient.readDppByProductId(demoProductId)).httpStatus());
    }

    private ConfigurableApplicationContext startContext(Class<?> applicationClass) {
        SpringApplication application = new SpringApplication(applicationClass);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        application.setApplicationContextFactory(
                ApplicationContextFactory.ofContextClass(AnnotationConfigServletWebServerApplicationContext.class)
        );
        return application.run(
                "--server.port=0",
                "--debug=false",
                "--logging.level.root=WARN",
                "--logging.level.org.springframework=WARN"
        );
    }

    private static void setDatasourceProperty(String key, String value) {
        ORIGINAL_DATASOURCE_PROPERTIES.putIfAbsent(key, System.getProperty(key));
        System.setProperty(key, value);
    }

    private void deleteExistingByProductId(HttpDppRepoClient repoClient, String productId) {
        try {
            String existingId = repoClient.readDppByProductId(productId).getPayload()
                    .path("digitalProductPassportId").asText();
            repoClient.deleteDppById(existingId);
        } catch (DppApiClientException exception) {
            if (exception.httpStatus() != 404) throw exception;
        }
    }

    private static com.fasterxml.jackson.databind.JsonNode toJson(Dpp4Fun dpp) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                    new dppsdk.core.transport.json.DppJsonCodec().toCompressedJson(dpp.asDigitalProductPassport()));
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Could not encode Dpp4Fun JSON", exception);
        }
    }

    private int localPort(ConfigurableApplicationContext context) {
        return ((ServletWebServerApplicationContext) context).getWebServer().getPort();
    }

    private void clearInMemoryStore(ConfigurableApplicationContext context, String beanName) {
        try {
            Object bean = context.getBean(beanName);
            java.lang.reflect.Method clear = bean.getClass().getDeclaredMethod("clear");
            clear.setAccessible(true);
            clear.invoke(bean);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to reset seeded in-memory store " + beanName, exception);
        }
    }

    private String captureStandardOut(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream replacement = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(replacement);
            action.run();
        } finally {
            System.setOut(original);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }
}
