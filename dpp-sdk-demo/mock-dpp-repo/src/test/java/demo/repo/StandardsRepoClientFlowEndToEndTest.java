package demo.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import demo.repo.testsupport.DemoDppFactory;
import demo.repo.testsupport.Dpp4FunDppCodecAdapter;
import demo.repo.testsupport.Dpp4FunDppValidatorAdapter;
import dpp.registry.client.HttpDppRegistryClient;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.repo.client.HttpDppRepoClient;
import dpp.repo.client.exception.DppHttpClientException;
import dpp.repo.payloads.DppStatusCode;
import dppsdk.dpp4fun.model.Dpp4Fun;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(properties = {
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.org.springframework=WARN"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StandardsRepoClientFlowEndToEndTest {

    @LocalServerPort
    private int repoPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryDppStore store;

    private HttpServer registryServer;
    private final Map<String, JsonNode> registryRecordsByRegistryId = new HashMap<>();
    private final Map<String, String> registryIdsByDppId = new HashMap<>();

    private final DemoDppFactory factory = new DemoDppFactory();

    @BeforeEach
    void clearRepoStore() {
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
        registryServer.createContext("/registerDPP", this::handleRegisterDpp);
        registryServer.createContext("/registry/dpps", this::handleRegistryRead);
        registryServer.start();
        int registryPort = registryServer.getAddress().getPort();

        String repoUrl = "http://localhost:" + repoPort;
        String registryUrl = "http://localhost:" + registryPort;

        HttpDppRepoClient<Dpp4Fun> repoClient = new HttpDppRepoClient<>(
                repoUrl,
                new Dpp4FunDppCodecAdapter(),
                new Dpp4FunDppValidatorAdapter()
        );
        HttpDppRegistryClient registryClient = new HttpDppRegistryClient(registryUrl);
        HttpClient rawHttpClient = HttpClient.newHttpClient();

        Dpp4Fun dpp = factory.createValidBedDpp();
        String dppId = dpp.getDppId();
        String productId = dpp.getProductId();

        assertEquals(dppId, repoClient.createDpp(dpp).getDppId());
        assertEquals(dppId, repoClient.readDppById(dppId).getDppId());
        assertEquals(productId, repoClient.readDppByProductId(productId).getProductId());

        com.fasterxml.jackson.databind.node.ObjectNode patch = objectMapper.createObjectNode();
        patch.putObject("characteristics").put("productName", "Updated End To End Name");
        assertEquals("Updated End To End Name", repoClient.updateDppById(dppId, patch).getCharacteristics().getProductName());

        assertEquals("Updated End To End Name", repoClient.readDataElement(dppId, "characteristics.productName").asText());
        assertEquals("Granular E2E Name",
                repoClient.updateDataElement(dppId, "characteristics.productName",
                        objectMapper.getNodeFactory().textNode("Granular E2E Name")).asText());

        assertEquals("Granular E2E Name",
                repoClient.readDppVersionByProductIdAndDate(productId, Instant.now()).getCharacteristics().getProductName());

        assertEquals(List.of(dppId), repoClient.readDppIdsByProductIds(List.of(productId), 10, "0").getDppIdentifiers());

        RegisterDppRequest request = new RegisterDppRequest(productId, dppId, "operator-123", repoUrl);
        String registryIdentifier = registryClient.postNewDppToRegistry(request).getRegistryIdentifier();
        assertFalse(registryIdentifier.isBlank());
        assertNotNull(readRegistryRecord(rawHttpClient, registryUrl, "/registry/dpps/" + registryIdentifier));
        assertNotNull(readRegistryRecord(rawHttpClient, registryUrl, "/registry/dpps/by-dpp-id/" + dppId));

        assertEquals(DppStatusCode.SuccessNoContent, repoClient.deleteDppById(dppId).getStatusCode());
        assertThrows(DppHttpClientException.class, () -> repoClient.readDppById(dppId));

        JsonNode eventsPayload = readEvents(rawHttpClient, repoUrl, dppId);
        assertTrue(eventsPayload.isArray());
        assertEquals("DPP_CREATED", eventsPayload.get(0).get("eventType").asText());
        assertTrue(eventsPayload.get(0).hasNonNull("occurredAt"));
        assertTrue(eventsPayload.toString().contains("DPP_UPDATED"));
        assertTrue(eventsPayload.toString().contains("DATA_ELEMENT_UPDATED"));
        assertTrue(eventsPayload.toString().contains("DPP_DELETED"));
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
        assertEquals(Set.of("productIdentifier", "dppIdentifier", "operatorIdentifier", "repoUrl"), fieldNames);
        String registryId = UUID.randomUUID().toString();
        JsonNode record = objectMapper.createObjectNode()
                .put("registryIdentifier", registryId)
                .put("dppIdentifier", body.get("dppIdentifier").asText())
                .put("productIdentifier", body.get("productIdentifier").asText())
                .put("operatorIdentifier", body.get("operatorIdentifier").asText())
                .put("repoUrl", body.get("repoUrl").asText())
                .put("registeredAt", Instant.now().toString())
                .put("lastUpdatedAt", Instant.now().toString());

        registryRecordsByRegistryId.put(registryId, record);
        registryIdsByDppId.put(body.get("dppIdentifier").asText(), registryId);

        JsonNode response = objectMapper.createObjectNode()
                .put("statusCode", "SuccessCreated")
                .set("payload", objectMapper.createObjectNode().put("registryIdentifier", registryId));
        writeJson(exchange, 201, response.toString());
    }

    private void handleRegistryRead(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String path = exchange.getRequestURI().getPath();
        JsonNode payload;
        if (path.startsWith("/registry/dpps/by-dpp-id/")) {
            String dppId = path.substring("/registry/dpps/by-dpp-id/".length());
            String registryId = registryIdsByDppId.get(dppId);
            payload = registryId == null ? null : registryRecordsByRegistryId.get(registryId);
        } else {
            String registryId = path.substring("/registry/dpps/".length());
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
                .uri(URI.create(repoUrl + "/dpps/" + dppId + "/events"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body()).get("payload");
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
