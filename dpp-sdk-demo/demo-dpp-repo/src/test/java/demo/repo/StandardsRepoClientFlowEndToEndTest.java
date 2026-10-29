package demo.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import demo.repo.testsupport.DemoDppFactory;
import demo.repo.testsupport.TypedDppFixtures;
import dpp.registry.client.HttpDppRegistryClient;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.repo.client.HttpDppRepoClient;
import dpp.repo.client.exception.DppApiClientException;
import dpp.repo.client.exception.DppHttpClientException;
import dpp.repo.payloads.DppApiResponse;
import dpp.repo.payloads.DppStatusCode;
import dpp.repo.payloads.DppRepresentation;
import dppsdk.core.transport.json.DppJsonCodec;
import dppsdk.core.model.DataElement;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.validation.format.DataFormatValidator;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.org.springframework=WARN"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class StandardsRepoClientFlowEndToEndTest {

    private static final AtomicInteger PRODUCT_SEQUENCE = new AtomicInteger(1);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    private int repoPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryDppStore store;

    @Autowired
    private DppRepoBackend backend;

    @Value("${dpp.repo.backend:memory}")
    private String backendId;

    @Value("${dpp.repo.events.enabled:true}")
    private boolean eventsEnabled;

    @Value("${dpp.repo.profile:dpp4fun}")
    private String profileId;

    private HttpServer registryServer;
    private final Map<String, JsonNode> registryRecordsByRegistryId = new HashMap<>();
    private final Map<String, String> registryIdsByDppId = new HashMap<>();

    private final DemoDppFactory factory = new DemoDppFactory();

    @Test
    @DisplayName("Typed comprehensive D4.1 fixture and all six Characteristics variants round-trip")
    void typedFixturesCoverAllAspectsAndVariants() throws Exception {
        Dpp4Fun comprehensive = TypedDppFixtures.comprehensiveChair(
                "urn:example:dpp:comprehensive", "urn:example:product:comprehensive");
        assertEquals(19, java.util.stream.Stream.of(comprehensive.getBillOfMaterials(),
                comprehensive.getCarbonFootprint(), comprehensive.getDurability(),
                comprehensive.getEnvironmentalImpact(), comprehensive.getHealthImpact(),
                comprehensive.getNameplate(), comprehensive.getProductRelationship(),
                comprehensive.getRecyclability(), comprehensive.getRefurbishment(),
                comprehensive.getReliability(), comprehensive.getRemanufacturing(),
                comprehensive.getRepairability(), comprehensive.getReusability(),
                comprehensive.getRightOfEndUsers(), comprehensive.getSafety(),
                comprehensive.getTraceability(), comprehensive.getUpgradability(),
                comprehensive.getUsage(), comprehensive.getWasteManagement())
                .filter(java.util.Objects::nonNull).count());
        var issues = new Dpp4FunValidationService().validate(comprehensive);
        assertTrue(issues.isValid(), () -> issues.issues().toString());

        Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();
        String fullJson = codec.toJson(comprehensive);
        Dpp4Fun fullRoundTrip = codec.fromExpandedJson(fullJson);
        assertEquals(comprehensive.asDigitalProductPassport(), fullRoundTrip.asDigitalProductPassport());
        Dpp4Fun compressedRoundTrip = codec.fromCompressedJson(codec.toCompressedJson(comprehensive));
        assertEquals(comprehensive.asDigitalProductPassport(), compressedRoundTrip.asDigitalProductPassport());
        assertEquals(Map.of("en-GB", "Office chair", "de-DE", "Bürostuhl"),
                fullRoundTrip.getNameplate().getManufacturerProductDesignation());
        assertEquals("cm", fullRoundTrip.getChairCharacteristics().getHeightUnitSymbol());

        for (Dpp4FunProductVariant variant : Dpp4FunProductVariant.values()) {
            String suffix = variant.id();
            Dpp4Fun small = TypedDppFixtures.variant("urn:example:dpp:" + suffix,
                    "urn:example:product:" + suffix, variant);
            assertEquals(variant, small.getCharacteristics().variant());
            assertEquals(small.asDigitalProductPassport(), codec.fromExpandedJson(codec.toJson(small))
                    .asDigitalProductPassport());
            assertTrue(new Dpp4FunValidationService().validate(small).isValid(), variant.id());
        }
    }

    @BeforeEach
    void clearRepoStore() {
        if ("postgres".equalsIgnoreCase(backendId)) {
            assertTrue(backend instanceof CorePostgresDppRepoBackend
                    || backend instanceof PostgresDppRepoBackend);
        } else {
            assertTrue(backend instanceof InMemoryDppRepoBackend);
        }
        store.clear();
    }

    @AfterEach
    void stopRegistry() {
        if (registryServer != null) {
            registryServer.stop(0);
        }
        registryRecordsByRegistryId.clear();
        registryIdsByDppId.clear();
    }

    @Test
    @DisplayName("Real repo service plus registry stub exercises the standard client flow end to end")
    void clientCanExerciseStandardsRepoFlowWithRegistryStub() throws Exception {
        registryServer = HttpServer.create(new InetSocketAddress(0), 0);
        registryServer.createContext("/v1/registerDPP", this::handleRegisterDpp);
        registryServer.createContext("/internal/dpps", this::handleRegistryRead);
        registryServer.start();
        int registryPort = registryServer.getAddress().getPort();

        String repoUrl = "http://localhost:" + repoPort;
        String registryUrl = "http://localhost:" + registryPort;

        HttpDppRepoClient repoClient = new HttpDppRepoClient(repoUrl);
        HttpDppRegistryClient registryClient = new HttpDppRegistryClient(registryUrl);
        HttpClient rawHttpClient = HttpClient.newHttpClient();

        Dpp4Fun dpp = factory.createValidChairDpp();
        String dppId = dpp.getDigitalProductPassportId();
        String productId = dpp.getUniqueProductIdentifier();

        assertEquals(dppId, repoClient.createDpp(objectMapper.readTree(
                new DppJsonCodec().toCompressedJson(dpp.asDigitalProductPassport()))).getDigitalProductPassportId());
        JsonNode compressedPassportPayload = repoClient.readDppById(dppId).getPayload();
        assertTrue(compressedPassportPayload.path("characteristics").isObject());
        DigitalProductPassport expandedCompressedPassport = new DppJsonCodec()
                .expandCompressedJson(compressedPassportPayload.toString()).passport();
        assertEquals(dppId, expandedCompressedPassport.getDppHeader().getDigitalProductPassportId());
        assertEquals(dppId, readFull(repoClient, dppId).getDigitalProductPassportId());
        assertEquals(productId, readFullByProductId(repoClient, productId).getUniqueProductIdentifier());

        Dpp4Fun updatedDpp = factory.withBrand(dpp, "Updated End To End Name");
        JsonNode updatedCanonical = objectMapper.readTree(new Dpp4FunJsonCodec().toJson(updatedDpp));
        com.fasterxml.jackson.databind.node.ObjectNode patch = objectMapper.createObjectNode();
        patch.set("elements", updatedCanonical.get("elements"));
        DppApiResponse<JsonNode> updateResponse = repoClient.updateDppById(dppId, patch, DppRepresentation.FULL);
        DigitalProductPassport updatedResponsePassport = new DppJsonCodec()
                .fromExpandedJson(updateResponse.getPayload().toString());
        DigitalProductPassport expectedUpdatedPassport = new DppJsonCodec()
                .fromExpandedJson(updatedCanonical.toString());
        assertEquals(expectedUpdatedPassport.getDataElements(), updatedResponsePassport.getDataElements());
        assertEquals("Updated End To End Name", readFull(repoClient, dppId).getNameplate().getBrand());

        assertEquals("Updated End To End Name",
                repoClient.readDataElement(dppId, DemoDppFactory.BRAND_PATH).getPayload().asText());
        SingleValuedDataElement expandedCompressedElement = (SingleValuedDataElement) new DppJsonCodec()
                .expandCompressedElementJson("brand",
                        repoClient.readDataElement(dppId, DemoDppFactory.BRAND_PATH)
                                .getPayload().toString(), null);
        assertEquals("Updated End To End Name", expandedCompressedElement.getValue());
        JsonNode brandElement = repoClient.readDataElement(
                dppId, DemoDppFactory.BRAND_PATH, DppRepresentation.FULL).getPayload();
        assertEquals("Updated End To End Name", brandElement.path("value").asText());
        SingleValuedDataElement expandedFullElement = (SingleValuedDataElement) new DppJsonCodec()
                .fromExpandedElementJson(brandElement.toString());
        assertEquals("Updated End To End Name", expandedFullElement.getValue());
        ((com.fasterxml.jackson.databind.node.ObjectNode) brandElement).put("value", "Granular E2E Name");
        JsonNode updatedElementPayload = repoClient.updateDataElement(dppId, DemoDppFactory.BRAND_PATH,
                brandElement, DppRepresentation.FULL).getPayload();
        assertEquals("Granular E2E Name", updatedElementPayload.path("value").asText());
        assertEquals("Granular E2E Name", ((SingleValuedDataElement) new DppJsonCodec()
                .fromExpandedElementJson(updatedElementPayload.toString())).getValue());

        assertEquals("Granular E2E Name",
                readFullAt(repoClient, dppId, Instant.now()).getNameplate().getBrand());

        assertEquals(List.of(dppId), repoClient.readDppIdsByProductIds(List.of(productId), 10, "0").getDppIdentifiers());

        RegisterDppRequest request = new RegisterDppRequest(productId, dppId, "operator-123", repoUrl);
        String registryIdentifier = registryClient.postNewDppToRegistry(request).getRegistrationId();
        assertFalse(registryIdentifier.isBlank());
        assertNotNull(readRegistryRecord(rawHttpClient, registryUrl, "/internal/dpps/" + registryIdentifier));
        assertNotNull(readRegistryRecord(rawHttpClient, registryUrl, "/internal/dpps/by-dpp-id/" + dppId));

        Instant historicalReadAt = Instant.now();
        assertEquals(DppStatusCode.SuccessNoContent, repoClient.deleteDppById(dppId).getStatusCode());
        DppApiClientException notFound = assertThrows(DppApiClientException.class,
                () -> repoClient.readDppById(dppId));
        assertEquals(404, notFound.httpStatus());
        assertEquals(DppStatusCode.ClientErrorResourceNotFound, notFound.statusCode());
        assertEquals("Granular E2E Name",
                readFullAt(repoClient, dppId, historicalReadAt).getNameplate().getBrand());

        JsonNode eventsPayload = readEvents(rawHttpClient, repoUrl, dppId);
        if (eventsEnabled) {
            assertTrue(eventsPayload.isArray());
            assertEquals("DPP_CREATED", eventsPayload.get(0).get("eventType").asText());
            assertTrue(eventsPayload.get(0).hasNonNull("occurredAt"));
            assertTrue(eventsPayload.toString().contains("DPP_UPDATED"));
            assertTrue(eventsPayload.toString().contains("DATA_ELEMENT_UPDATED"));
            assertTrue(eventsPayload.toString().contains("DPP_DELETED"));
        } else {
            assertTrue(eventsPayload.isNull());
        }
    }

    @Test
    @DisplayName("Repository HTTP validation composes core with the selected profile before persistence")
    void publicClientFixturesRespectCoreAndDpp4FunValidationBoundaries() throws Exception {
        String repoUrl = "http://localhost:" + repoPort;
        Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();
        HttpDppRepoClient repoClient = new HttpDppRepoClient(repoUrl);

        Dpp4Fun valid = withUniqueIdentifiers(factory.createValidChairDpp());
        List<DataElement> compatibleElements = new ArrayList<>(valid.asDigitalProductPassport().getDataElements());
        compatibleElements.add(new SingleValuedDataElement.Builder().elementId("vendorCompatibleExtension")
                .valueDataType("xsd:string").value("unknown but valid extension").build());
        Dpp4Fun compatibleExtension = Dpp4Fun.from(valid.asDigitalProductPassport().toBuilder()
                .dataElements(compatibleElements).build());
        var coreIssues = new dppsdk.core.validation.DppValidator().validate(compatibleExtension.asDigitalProductPassport());
        var formatIssues = new DataFormatValidator().validate(compatibleExtension.asDigitalProductPassport());
        var profileIssues = new Dpp4FunValidationService().validate(compatibleExtension);
        assertTrue(coreIssues.isValid(), "unknown valid extensions pass generic core validation");
        assertTrue(formatIssues.isValid(), "unknown valid extensions pass canonical format validation");
        assertTrue(profileIssues.isValid(), "unknown compatible extensions do not create profile requirements");

        Dpp4Fun profileInvalidForCreate = profileInvalidFixture(
                withUniqueIdentifiers(factory.createValidChairDpp()));
        if ("core".equalsIgnoreCase(profileId)) {
            assertEquals(profileInvalidForCreate.getDigitalProductPassportId(),
                    repoClient.createDpp(jsonFor(profileInvalidForCreate, DppRepresentation.COMPRESSED))
                            .getDigitalProductPassportId());
            assertEquals(profileInvalidForCreate.getDigitalProductPassportId(),
                    readFull(repoClient, profileInvalidForCreate.getDigitalProductPassportId())
                            .getDigitalProductPassportId());
        } else {
            expectCreateValidationRejection(repoClient, profileInvalidForCreate, DppRepresentation.FULL);
        }

        Dpp4Fun coreInvalid = coreInvalidFixture(withUniqueIdentifiers(factory.createValidChairDpp()));
        var coreInvalidIssues = new dppsdk.core.validation.DppValidator().validate(coreInvalid.asDigitalProductPassport());
        assertTrue(coreInvalidIssues.issues().stream()
                .anyMatch(issue -> issue.rule().equals("EN18223_ELEMENT_ID_UNIQUE_WITHIN_LOCATION")));
        assertTrue(new Dpp4FunValidationService().validate(coreInvalid).issues().stream()
                .anyMatch(issue -> issue.rule().equals("EN18223_ELEMENT_ID_UNIQUE_WITHIN_LOCATION")));
        expectCreateValidationRejection(repoClient, coreInvalid, DppRepresentation.FULL);

        Dpp4Fun updateBaseline = withUniqueIdentifiers(factory.createValidChairDpp());
        repoClient.createDpp(jsonFor(updateBaseline, DppRepresentation.COMPRESSED));
        Instant beforeFailedUpdate = Instant.now();
        ObjectNode profileInvalidPatch = objectMapper.createObjectNode();
        Dpp4Fun edited = withHeader(factory.withBrand(factory.createValidChairDpp(), "Rejected Profile Update"),
                updateBaseline.getHeader());
        JsonNode invalidElements = objectMapper.readTree(codec.toJson(edited)).get("elements");
        ((ObjectNode) findElement(invalidElements, "brand"))
                .put("dictionaryReference", "urn:example:deliberately-wrong-brand-semantic");
        profileInvalidPatch.set("elements", invalidElements);
        if ("core".equalsIgnoreCase(profileId)) {
            repoClient.updateDppById(updateBaseline.getDigitalProductPassportId(), profileInvalidPatch,
                    DppRepresentation.FULL);
            assertEquals("Rejected Profile Update",
                    readFull(repoClient, updateBaseline.getDigitalProductPassportId()).getNameplate().getBrand());
        } else {
            expectUpdateValidationRejection(repoClient, updateBaseline, profileInvalidPatch);
            assertEquals(updateBaseline.getNameplate().getBrand(),
                    readFull(repoClient, updateBaseline.getDigitalProductPassportId()).getNameplate().getBrand());
            assertEquals(updateBaseline.getNameplate().getBrand(),
                    readFullAt(repoClient, updateBaseline.getDigitalProductPassportId(),
                            beforeFailedUpdate.plusSeconds(2)).getNameplate().getBrand());
            assertCreatedOnlyEvent(repoUrl, updateBaseline.getDigitalProductPassportId());
        }

        Dpp4Fun coreInvalidUpdate = coreInvalidFixture(withHeader(coreInvalid, updateBaseline.getHeader()));
        ObjectNode coreInvalidPatch = objectMapper.createObjectNode();
        coreInvalidPatch.set("elements", objectMapper.readTree(codec.toJson(coreInvalidUpdate)).get("elements"));
        expectUpdateValidationRejection(repoClient, updateBaseline, coreInvalidPatch);
        String acceptedBrand = "core".equalsIgnoreCase(profileId)
                ? "Rejected Profile Update" : updateBaseline.getNameplate().getBrand();
        assertEquals(acceptedBrand,
                readFull(repoClient, updateBaseline.getDigitalProductPassportId()).getNameplate().getBrand());
        assertEquals(acceptedBrand,
                readFullAt(repoClient, updateBaseline.getDigitalProductPassportId(),
                        Instant.now().plusSeconds(2)).getNameplate().getBrand());
        if ("core".equalsIgnoreCase(profileId)) {
            assertCreatedAndUpdatedEvents(repoUrl, updateBaseline.getDigitalProductPassportId());
        } else {
            assertCreatedOnlyEvent(repoUrl, updateBaseline.getDigitalProductPassportId());
        }
    }

    private Dpp4Fun readFull(HttpDppRepoClient client, String dppId) {
        JsonNode payload = client.readDppById(dppId, DppRepresentation.FULL).getPayload();
        return Dpp4Fun.from(new DppJsonCodec().fromExpandedJson(payload.toString()));
    }

    private Dpp4Fun readFullByProductId(HttpDppRepoClient client, String productId) {
        JsonNode payload = client.readDppByProductId(productId, DppRepresentation.FULL).getPayload();
        return Dpp4Fun.from(new DppJsonCodec().fromExpandedJson(payload.toString()));
    }

    private Dpp4Fun readFullAt(HttpDppRepoClient client, String dppId, Instant date) {
        JsonNode payload = client.readDppVersionByIdAndDate(dppId, date, DppRepresentation.FULL).getPayload();
        return Dpp4Fun.from(new DppJsonCodec().fromExpandedJson(payload.toString()));
    }

    private void expectCreateValidationRejection(HttpDppRepoClient client, Dpp4Fun dpp) throws Exception {
        expectCreateValidationRejection(client, dpp, DppRepresentation.COMPRESSED);
    }

    private void expectCreateValidationRejection(HttpDppRepoClient client, Dpp4Fun dpp,
            DppRepresentation representation) throws Exception {
        DppApiClientException rejected = assertThrows(DppApiClientException.class,
                () -> client.createDpp(jsonFor(dpp, representation), representation));
        assertEquals(400, rejected.httpStatus());
        assertEquals(DppStatusCode.ClientErrorBadRequest, rejected.statusCode());
        assertNotPersisted(client, dpp.getDigitalProductPassportId());
        assertNoLifecycleEvents("http://localhost:" + repoPort, dpp.getDigitalProductPassportId());
    }

    private void expectUpdateValidationRejection(HttpDppRepoClient client, Dpp4Fun current, JsonNode patch) {
        DppApiClientException rejected = assertThrows(DppApiClientException.class,
                () -> client.updateDppById(current.getDigitalProductPassportId(), patch, DppRepresentation.FULL));
        assertEquals(400, rejected.httpStatus());
        assertEquals(DppStatusCode.ClientErrorBadRequest, rejected.statusCode());
    }

    private void assertNotPersisted(HttpDppRepoClient client, String dppId) {
        DppApiClientException current = assertThrows(DppApiClientException.class, () -> client.readDppById(dppId));
        assertEquals(404, current.httpStatus());
        DppApiClientException historical = assertThrows(DppApiClientException.class,
                () -> client.readDppVersionByIdAndDate(dppId, Instant.now().plusSeconds(2)));
        assertEquals(404, historical.httpStatus());
    }

    private JsonNode jsonFor(Dpp4Fun dpp, DppRepresentation representation) throws IOException {
        String json = representation == DppRepresentation.FULL
                ? new DppJsonCodec().toExpandedJson(dpp.asDigitalProductPassport())
                : new DppJsonCodec().toCompressedJson(dpp.asDigitalProductPassport());
        return objectMapper.readTree(json);
    }

    private void assertNoLifecycleEvents(String repoUrl, String dppId) throws Exception {
        if (!eventsEnabled) return;
        HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                .uri(URI.create(repoUrl + "/internal/dpps/" + dppId + "/events")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    private void assertCreatedOnlyEvent(String repoUrl, String dppId) throws Exception {
        if (!eventsEnabled) return;
        JsonNode events = readEvents(HttpClient.newHttpClient(), repoUrl, dppId);
        assertEquals(1, events.size());
        assertEquals("DPP_CREATED", events.get(0).path("eventType").asText());
    }

    private void assertCreatedAndUpdatedEvents(String repoUrl, String dppId) throws Exception {
        if (!eventsEnabled) return;
        JsonNode events = readEvents(HttpClient.newHttpClient(), repoUrl, dppId);
        assertEquals(2, events.size());
        assertEquals("DPP_CREATED", events.get(0).path("eventType").asText());
        assertEquals("DPP_UPDATED", events.get(1).path("eventType").asText());
    }

    private Dpp4Fun coreInvalidFixture(Dpp4Fun valid) {
        List<DataElement> elements = new ArrayList<>(valid.asDigitalProductPassport().getDataElements());
        elements.add(new SingleValuedDataElement.Builder().elementId("extensionDuplicate")
                .valueDataType("xsd:string").value("first").build());
        elements.add(new SingleValuedDataElement.Builder().elementId("extensionDuplicate")
                .valueDataType("xsd:string").value("second").build());
        DigitalProductPassport invalid = valid.asDigitalProductPassport().toBuilder().dataElements(elements).build();
        return Dpp4Fun.from(invalid);
    }

    private Dpp4Fun profileInvalidFixture(Dpp4Fun valid) {
        DppHeader header = valid.getHeader().toBuilder().contentSpecificationIds(
                java.util.stream.Stream.concat(valid.getHeader().getContentSpecificationIds().stream(),
                        java.util.stream.Stream.of("urn:example:unsupported-profile"))
                        .distinct().toList()).build();
        return Dpp4Fun.from(valid.asDigitalProductPassport().toBuilder().dppHeader(header).build());
    }

    private JsonNode findElement(JsonNode node, String elementId) {
        if (node == null) return null;
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findElement(child, elementId);
                if (found != null) return found;
            }
        } else if (node.isObject()) {
            if (elementId.equals(node.path("elementId").asText())) return node;
            JsonNode found = findElement(node.get("elements"), elementId);
            if (found == null) found = findElement(node.get("value"), elementId);
            if (found != null) return found;
        }
        return null;
    }

    private Dpp4Fun withUniqueIdentifiers(Dpp4Fun source) {
        String passportId = UUID.randomUUID().toString();
        int sequence = PRODUCT_SEQUENCE.getAndIncrement();
        String body = "040123456789" + sequence;
        int sum = 0;
        for (int index = body.length() - 1, weight = 3; index >= 0; index--, weight = weight == 3 ? 1 : 3) {
            sum += Character.digit(body.charAt(index), 10) * weight;
        }
        String productId = body + ((10 - sum % 10) % 10);
        DppHeader header = source.getHeader().toBuilder().digitalProductPassportId(passportId)
                .uniqueProductIdentifier(productId).build();
        return source.toBuilder().header(header)
                .nameplate(source.getNameplate().toBuilder().gtinCode(productId).build()).build();
    }

    private Dpp4Fun withHeader(Dpp4Fun source, DppHeader header) {
        return source.toBuilder().header(header)
                .nameplate(source.getNameplate().toBuilder().gtinCode(header.getUniqueProductIdentifier()).build())
                .build();
    }

    private void handleRegisterDpp(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        JsonNode body = objectMapper.readTree(exchange.getRequestBody());
        assertFalse(body.has("backupOperatorIdentifier"));
        Set<String> fieldNames = new HashSet<>();
        body.fieldNames().forEachRemaining(fieldNames::add);
        assertEquals(Set.of(
                "uniqueProductIdentifier",
                "digitalProductPassportId",
                "uniqueEconomicOperatorIdentifier",
                "dppApiEndpoint"
        ), fieldNames);
        String registryId = UUID.randomUUID().toString();
        JsonNode record = objectMapper.createObjectNode()
                .put("registryIdentifier", registryId)
                .put("dppIdentifier", body.get("digitalProductPassportId").asText())
                .put("productIdentifier", body.get("uniqueProductIdentifier").asText())
                .put("operatorIdentifier", body.get("uniqueEconomicOperatorIdentifier").asText())
                .put("repoUrl", body.get("dppApiEndpoint").asText())
                .put("registeredAt", Instant.now().toString())
                .put("lastUpdatedAt", Instant.now().toString());

        registryRecordsByRegistryId.put(registryId, record);
        registryIdsByDppId.put(body.get("digitalProductPassportId").asText(), registryId);

        JsonNode response = objectMapper.createObjectNode()
                .put("statusCode", "SuccessCreated")
                .set("payload", objectMapper.createObjectNode().put("registrationId", registryId));
        writeJson(exchange, 201, response.toString());
    }

    private void handleRegistryRead(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String path = exchange.getRequestURI().getPath();
        JsonNode payload;
        if (path.startsWith("/internal/dpps/by-dpp-id/")) {
            String dppId = path.substring("/internal/dpps/by-dpp-id/".length());
            String registryId = registryIdsByDppId.get(dppId);
            payload = registryId == null ? null : registryRecordsByRegistryId.get(registryId);
        } else {
            String registryId = path.substring("/internal/dpps/".length());
            payload = registryRecordsByRegistryId.get(registryId);
        }

        if (payload == null) {
            JsonNode response = objectMapper.createObjectNode()
                    .put("statusCode", "ClientErrorResourceNotFound");
            writeJson(exchange, 404, response.toString());
            return;
        }

        JsonNode response = objectMapper.createObjectNode()
                .put("statusCode", "Success")
                .set("payload", payload);
        writeJson(exchange, 200, response.toString());
    }

    private JsonNode readEvents(HttpClient client, String repoUrl, String dppId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(repoUrl + "/internal/dpps/" + dppId + "/events"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 501 ? objectMapper.nullNode() : objectMapper.readTree(response.body()).get("payload");
    }

    private JsonNode readRegistryRecord(HttpClient client, String registryUrl, String path)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(registryUrl + path))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body()).get("payload");
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
