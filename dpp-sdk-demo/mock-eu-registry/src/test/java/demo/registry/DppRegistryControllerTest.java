package demo.registry;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "demo.repo.public-base-url=http://localhost:18080",
        "demo.repo.verification-base-url=http://localhost:18081",
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.org.springframework=WARN"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DppRegistryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryRegistryStore store;

    private HttpServer repoServer;
    private int repoHeadStatus = 200;

    @Test
    @DisplayName("Controller keeps Swagger example payloads outside the request-handling class")
    void controllerDoesNotDeclareSwaggerExamplePayloads() {
        assertTrue(java.util.Arrays.stream(DppRegistryController.class.getDeclaredFields())
                .noneMatch(field -> field.getName().endsWith("_EXAMPLE")));
    }

    @AfterEach
    void clearState() {
        if (repoServer != null) {
            repoServer.stop(0);
            repoServer = null;
        }
        store.clear();
    }

    @Test
    @DisplayName("POST /v1/registerDPP returns registrationId and internal lookup endpoints return registry metadata only")
    void validRegistrationReturnsRegistrationIdAndCanBeReadBack() throws Exception {
        String repoUrl = startRepoStubReturning(200);

        String responseBody = mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-123", "dpp-123", "operator-123", repoUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("SuccessCreated"))
                .andExpect(jsonPath("$.payload.registrationId").value(notNullValue()))
                .andExpect(jsonPath("$.payload.registrationId").value(not("")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String registryId = responseBody.split("\"registrationId\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/internal/dpps/" + registryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.registryIdentifier").value(registryId))
                .andExpect(jsonPath("$.payload.dppIdentifier").value("dpp-123"))
                .andExpect(jsonPath("$.payload.productIdentifier").value("product-123"))
                .andExpect(jsonPath("$.payload.operatorIdentifier").value("operator-123"))
                .andExpect(jsonPath("$.payload.repoUrl").value(repoUrl))
                .andExpect(jsonPath("$.payload.registeredAt").value(notNullValue()))
                .andExpect(jsonPath("$.payload.lastUpdatedAt").value(notNullValue()))
                .andExpect(jsonPath("$.payload.characteristics").doesNotExist())
                .andExpect(jsonPath("$.payload.passportMetadata").doesNotExist())
                .andExpect(jsonPath("$.payload.backupOperatorIdentifier").doesNotExist());

        mockMvc.perform(get("/internal/dpps/by-dpp-id/dpp-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.registryIdentifier").value(registryId))
                .andExpect(jsonPath("$.payload.dppIdentifier").value("dpp-123"))
                .andExpect(jsonPath("$.payload.productIdentifier").value("product-123"))
                .andExpect(jsonPath("$.payload.operatorIdentifier").value("operator-123"))
                .andExpect(jsonPath("$.payload.repoUrl").value(repoUrl))
                .andExpect(jsonPath("$.payload.registeredAt").value(notNullValue()))
                .andExpect(jsonPath("$.payload.lastUpdatedAt").value(notNullValue()))
                .andExpect(jsonPath("$.payload.characteristics").doesNotExist())
                .andExpect(jsonPath("$.payload.passportMetadata").doesNotExist())
                .andExpect(jsonPath("$.payload.backupOperatorIdentifier").doesNotExist());

        mockMvc.perform(get("/internal/dpps/missing-registry-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/internal/dpps/by-dpp-id/missing-dpp-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/internal/dpps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload[0]").value("dpp-123"));
    }

    @Test
    @DisplayName("POST /v1/registerDPP rejects blank standard fields and duplicate DPP ids")
    void registrationValidatesRequiredFieldsAndRejectsDuplicates() throws Exception {
        String repoUrl = startRepoStubReturning(200);

        assertMissingRequiredFieldFails("uniqueProductIdentifier");
        assertMissingRequiredFieldFails("digitalProductPassportId");
        assertMissingRequiredFieldFails("uniqueEconomicOperatorIdentifier");
        assertMissingRequiredFieldFails("dppApiEndpoint");

        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-123", "dpp-123", "operator-123", repoUrl)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-456", "dpp-123", "operator-456", "http://localhost:65530")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"));
    }

    @Test
    @DisplayName("POST /v1/registerDPP rejects DPP references that the repo cannot verify")
    void registrationVerifiesReferencedDppExistsInRepo() throws Exception {
        String missingRepoUrl = startRepoStubReturning(404);

        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-404", "missing-dpp", "operator-123", missingRepoUrl)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"))
                .andExpect(jsonPath("$.messages[0].text").value("Referenced DPP missing-dpp was not found in repo " + missingRepoUrl));

        assertTrue(store.findByDppId("missing-dpp").isEmpty());

        repoHeadStatus = 500;
        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-500", "downstream-dpp", "operator-123", missingRepoUrl)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.statusCode").value("ServerErrorBadGateway"));

        assertTrue(store.findByDppId("downstream-dpp").isEmpty());
    }

    @Test
    @DisplayName("POST /v1/registerDPP verifies via the configured internal repo base when the request uses the configured public repo base")
    void registrationMapsConfiguredPublicRepoUrlToConfiguredVerificationRepoUrl() throws Exception {
        String publicRepoUrl = "http://localhost:18080";
        startRepoStubReturningAt(200, 18081);

        String responseBody = mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-public", "dpp-public", "operator-123", publicRepoUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("SuccessCreated"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String registryId = responseBody.split("\"registrationId\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/internal/dpps/" + registryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.repoUrl").value(publicRepoUrl));
    }

    @Test
    @DisplayName("POST /v1/registerDPP returns bad gateway when the repo URL is unreachable")
    void registrationFailsWhenRepoUrlIsUnreachable() throws Exception {
        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(
                                "product-unreachable",
                                "unreachable-dpp",
                                "operator-123",
                                "http://localhost:65530"
                        )))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.statusCode").value("ServerErrorBadGateway"));

        assertTrue(store.findByDppId("unreachable-dpp").isEmpty());
    }

    @Test
    @DisplayName("GET / returns a simple registry landing page")
    void rootReturnsRegistryLandingPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("DPP Registry API")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Service is running.")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("/swagger-ui/index.html")));
    }

    @Test
    @DisplayName("GET /health reports the registry mock as UP")
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("dpp-registry-api"));
    }

    @Test
    @DisplayName("GET /v3/api-docs exposes registry OpenAPI paths and tag")
    void openApiDocsExposeRegistryEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("DPP Mock Registry API"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post").exists())
                .andExpect(jsonPath("$.paths['/internal/dpps/{registryId}'].get").exists())
                .andExpect(jsonPath("$.paths['/internal/dpps/by-dpp-id/{dppId}'].get").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'DPP Registry')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'DPP Registry - Internal')]").exists())
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.tags")
                        .value(org.hamcrest.Matchers.contains("DPP Registry")))
                .andExpect(jsonPath("$.paths['/internal/dpps'].get.tags")
                        .value(org.hamcrest.Matchers.contains("DPP Registry - Internal")))
                .andExpect(jsonPath("$.paths['/internal/dpps/{registryId}'].get.tags")
                        .value(org.hamcrest.Matchers.contains("DPP Registry - Internal")))
                .andExpect(jsonPath("$.paths['/internal/dpps/by-dpp-id/{dppId}'].get.tags")
                        .value(org.hamcrest.Matchers.contains("DPP Registry - Internal")))
                .andExpect(jsonPath("$.paths['/internal/dpps/{registryId}'].get.parameters[?(@.name == 'registryId' && @.example == '8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc')]").exists())
                .andExpect(jsonPath("$.paths['/internal/dpps/by-dpp-id/{dppId}'].get.parameters[?(@.name == 'dppId' && @.example == 'e7d64b7b-18f2-4d77-9c41-2fa1d1d6b8aa')]").exists())
                .andExpect(jsonPath("$.paths['/internal/dpps'].get").exists())
                .andExpect(jsonPath("$.paths['/registry/dpps']").doesNotExist())
                .andExpect(jsonPath("$.paths['/registry/dpps/{registryId}']").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.RegisterDppRequest.required")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                "uniqueProductIdentifier",
                                "digitalProductPassportId",
                                "uniqueEconomicOperatorIdentifier",
                                "dppApiEndpoint")))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.requestBody.content['application/json'].examples['Register DPP metadata'].value.digitalProductPassportId").value("49192c87-20c8-4b6f-88de-48b56ca4c211"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.requestBody.content['application/json'].examples['Register DPP metadata'].value.uniqueProductIdentifier").value("04012345678901"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['201'].content['application/json'].example.statusCode").value("SuccessCreated"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['201'].content['application/json'].example.payload.registrationId").value("8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['201'].content['application/json'].example.payload.registryIdentifier").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['400'].content['application/json'].example.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['404'].content['application/json'].example.messages[0].code").value("REPO_DPP_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['409'].content['application/json'].example.statusCode").value("ClientResourceConflict"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['502'].content['application/json'].example.statusCode").value("ServerErrorBadGateway"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['500'].content['application/json'].example.statusCode").value("ServerInternalError"))
                .andExpect(jsonPath("$.paths['/internal/dpps/{registryId}'].get.responses['200'].content['application/json'].example.payload.registeredAt").value("2026-05-19T00:00:00Z"))
                .andExpect(jsonPath("$.paths['/internal/dpps/{registryId}'].get.responses['200'].content['application/json'].example.payload.lastUpdatedAt").exists())
                .andExpect(jsonPath("$.paths['/internal/dpps/{registryId}'].get.responses['404'].content['application/json'].example.messages[0].code").value("REGISTRY_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/internal/dpps/by-dpp-id/{dppId}'].get.responses['200'].content['application/json'].example.payload.registryIdentifier").value("8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc"))
                .andExpect(jsonPath("$.paths['/internal/dpps/by-dpp-id/{dppId}'].get.responses['200'].content['application/json'].example.payload.registrationId").doesNotExist())
                .andExpect(jsonPath("$.paths['/internal/dpps/by-dpp-id/{dppId}'].get.responses['500'].content['application/json'].example.statusCode").value("ServerInternalError"));
    }

    private void assertMissingRequiredFieldFails(String fieldName) throws Exception {
        String request = registrationBody("product-123", "dpp-123", "operator-123", "http://localhost:8080")
                .replace("\"" + fieldName + "\": \"" + valueFor(fieldName) + "\"", "\"" + fieldName + "\": \" \"");

        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.messages[0].correlationId").value(notNullValue()));
    }

    private String valueFor(String fieldName) {
        return switch (fieldName) {
            case "uniqueProductIdentifier" -> "product-123";
            case "digitalProductPassportId" -> "dpp-123";
            case "uniqueEconomicOperatorIdentifier" -> "operator-123";
            case "dppApiEndpoint" -> "http://localhost:8080";
            default -> throw new IllegalArgumentException("Unsupported field " + fieldName);
        };
    }

    private String registrationBody(String productId, String dppId, String operatorId, String repoUrl) {
        return """
                {
                  "uniqueProductIdentifier": "%s",
                  "digitalProductPassportId": "%s",
                  "uniqueEconomicOperatorIdentifier": "%s",
                  "dppApiEndpoint": "%s"
                }
                """.formatted(productId, dppId, operatorId, repoUrl);
    }

    private String startRepoStubReturning(int status) throws IOException {
        repoHeadStatus = status;
        repoServer = HttpServer.create(new InetSocketAddress(0), 0);
        repoServer.createContext("/internal/dpps", this::handleRepoHead);
        repoServer.start();
        return "http://localhost:" + repoServer.getAddress().getPort();
    }

    private String startRepoStubReturningAt(int status, int port) throws IOException {
        repoHeadStatus = status;
        repoServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        repoServer.createContext("/internal/dpps", this::handleRepoHead);
        repoServer.start();
        return "http://localhost:" + repoServer.getAddress().getPort();
    }

    @Test
    @DisplayName("Old registry-prefixed internal routes are removed")
    void oldInternalRegistryRoutesReturnNotFound() throws Exception {
        mockMvc.perform(get("/registry/dpps"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/registry/dpps/missing-registry-id"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/registry/dpps/by-dpp-id/missing-dpp-id"))
                .andExpect(status().isNotFound());
    }

    private void handleRepoHead(HttpExchange exchange) throws IOException {
        if (!"HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        exchange.sendResponseHeaders(repoHeadStatus, -1);
    }
}
