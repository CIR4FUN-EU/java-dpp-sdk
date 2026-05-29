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

    @AfterEach
    void clearState() {
        if (repoServer != null) {
            repoServer.stop(0);
            repoServer = null;
        }
        store.clear();
    }

    @Test
    @DisplayName("POST /registerDPP returns a registry ID and both lookup endpoints return registry metadata only")
    void validRegistrationReturnsRegistryIdentifierAndCanBeReadBack() throws Exception {
        String repoUrl = startRepoStubReturning(200);

        String responseBody = mockMvc.perform(post("/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-123", "dpp-123", "operator-123", repoUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("SuccessCreated"))
                .andExpect(jsonPath("$.payload.registryIdentifier").value(notNullValue()))
                .andExpect(jsonPath("$.payload.registryIdentifier").value(not("")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String registryId = responseBody.split("\"registryIdentifier\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/registry/dpps/" + registryId))
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

        mockMvc.perform(get("/registry/dpps/by-dpp-id/dpp-123"))
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

        mockMvc.perform(get("/registry/dpps/missing-registry-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/registry/dpps/by-dpp-id/missing-dpp-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));
    }

    @Test
    @DisplayName("POST /registerDPP rejects blank required fields and duplicate dppIdentifier values")
    void registrationValidatesRequiredFieldsAndRejectsDuplicates() throws Exception {
        String repoUrl = startRepoStubReturning(200);

        assertMissingRequiredFieldFails("productIdentifier");
        assertMissingRequiredFieldFails("dppIdentifier");
        assertMissingRequiredFieldFails("operatorIdentifier");
        assertMissingRequiredFieldFails("repoUrl");

        mockMvc.perform(post("/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-123", "dpp-123", "operator-123", repoUrl)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-456", "dpp-123", "operator-456", "http://localhost:65530")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"));
    }

    @Test
    @DisplayName("POST /registerDPP rejects DPP references that the repo cannot verify")
    void registrationVerifiesReferencedDppExistsInRepo() throws Exception {
        String missingRepoUrl = startRepoStubReturning(404);

        mockMvc.perform(post("/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-404", "missing-dpp", "operator-123", missingRepoUrl)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"))
                .andExpect(jsonPath("$.messages[0].text").value("Referenced DPP missing-dpp was not found in repo " + missingRepoUrl));

        assertTrue(store.findByDppId("missing-dpp").isEmpty());

        repoHeadStatus = 500;
        mockMvc.perform(post("/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-500", "downstream-dpp", "operator-123", missingRepoUrl)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.statusCode").value("ServerErrorBadGateway"));

        assertTrue(store.findByDppId("downstream-dpp").isEmpty());
    }

    @Test
    @DisplayName("POST /registerDPP verifies via the configured internal repo base when the request uses the configured public repo base")
    void registrationMapsConfiguredPublicRepoUrlToConfiguredVerificationRepoUrl() throws Exception {
        String publicRepoUrl = "http://localhost:18080";
        startRepoStubReturningAt(200, 18081);

        String responseBody = mockMvc.perform(post("/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-public", "dpp-public", "operator-123", publicRepoUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("SuccessCreated"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String registryId = responseBody.split("\"registryIdentifier\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/registry/dpps/" + registryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.repoUrl").value(publicRepoUrl));
    }

    @Test
    @DisplayName("POST /registerDPP returns bad gateway when the repo URL is unreachable")
    void registrationFailsWhenRepoUrlIsUnreachable() throws Exception {
        mockMvc.perform(post("/registerDPP")
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
    @DisplayName("GET /health reports the registry mock as UP")
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("mock-eu-registry"));
    }

    private void assertMissingRequiredFieldFails(String fieldName) throws Exception {
        String request = registrationBody("product-123", "dpp-123", "operator-123", "http://localhost:8080")
                .replace("\"" + fieldName + "\": \"" + valueFor(fieldName) + "\"", "\"" + fieldName + "\": \" \"");

        mockMvc.perform(post("/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.messages[0].correlationId").value(notNullValue()));
    }

    private String valueFor(String fieldName) {
        return switch (fieldName) {
            case "productIdentifier" -> "product-123";
            case "dppIdentifier" -> "dpp-123";
            case "operatorIdentifier" -> "operator-123";
            case "repoUrl" -> "http://localhost:8080";
            default -> throw new IllegalArgumentException("Unsupported field " + fieldName);
        };
    }

    private String registrationBody(String productId, String dppId, String operatorId, String repoUrl) {
        return """
                {
                  "productIdentifier": "%s",
                  "dppIdentifier": "%s",
                  "operatorIdentifier": "%s",
                  "repoUrl": "%s"
                }
                """.formatted(productId, dppId, operatorId, repoUrl);
    }

    private String startRepoStubReturning(int status) throws IOException {
        repoHeadStatus = status;
        repoServer = HttpServer.create(new InetSocketAddress(0), 0);
        repoServer.createContext("/dpps", this::handleRepoHead);
        repoServer.start();
        return "http://localhost:" + repoServer.getAddress().getPort();
    }

    private String startRepoStubReturningAt(int status, int port) throws IOException {
        repoHeadStatus = status;
        repoServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        repoServer.createContext("/dpps", this::handleRepoHead);
        repoServer.start();
        return "http://localhost:" + repoServer.getAddress().getPort();
    }

    private void handleRepoHead(HttpExchange exchange) throws IOException {
        if (!"HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        exchange.sendResponseHeaders(repoHeadStatus, -1);
    }
}
