package demo.registry;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "demo.repo.public-base-url=http://localhost:18080",
        "demo.repo.verification-base-url=http://localhost:18081",
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.org.springframework=WARN",
        "dpp.registry.backend=postgres"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DppRegistryControllerPostgresBackendTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DppRegistryService registryService;

    private HttpServer repoServer;
    private int repoHeadStatus = 200;

    @BeforeEach
    @AfterEach
    void clearState() {
        if (repoServer != null) {
            repoServer.stop(0);
            repoServer = null;
        }
        registryService.clear();
    }

    @Test
    @DisplayName("PostgreSQL backend preserves register and both registry lookup endpoints")
    void postgresModeRegisterAndLookupWork() throws Exception {
        String repoUrl = startRepoStubReturning(200);

        String responseBody = mockMvc.perform(post("/registerDPP")
                        .contentType(APPLICATION_JSON)
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
                .andExpect(jsonPath("$.payload.lastUpdatedAt").value(notNullValue()));
    }

    @Test
    @DisplayName("PostgreSQL backend preserves duplicate and missing-record behavior")
    void postgresModeDuplicateAndMissingCasesMatchMemoryMode() throws Exception {
        String repoUrl = startRepoStubReturning(200);

        mockMvc.perform(post("/registerDPP")
                        .contentType(APPLICATION_JSON)
                        .content(registrationBody("product-123", "dpp-123", "operator-123", repoUrl)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/registerDPP")
                        .contentType(APPLICATION_JSON)
                        .content(registrationBody("product-456", "dpp-123", "operator-456", repoUrl)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"))
                .andExpect(jsonPath("$.messages[0].code").value("REGISTRY_CONFLICT"));

        mockMvc.perform(get("/registry/dpps/missing-registry-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"))
                .andExpect(jsonPath("$.messages[0].code").value("REGISTRY_NOT_FOUND"));

        mockMvc.perform(get("/registry/dpps/by-dpp-id/missing-dpp-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"))
                .andExpect(jsonPath("$.messages[0].code").value("REGISTRY_NOT_FOUND"));
    }

    @Test
    @DisplayName("PostgreSQL backend still verifies the repo DPP with HEAD before persisting")
    void postgresModeStillVerifiesRepoHeadBeforePersisting() throws Exception {
        String missingRepoUrl = startRepoStubReturning(404);

        mockMvc.perform(post("/registerDPP")
                        .contentType(APPLICATION_JSON)
                        .content(registrationBody("product-404", "missing-dpp", "operator-123", missingRepoUrl)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"))
                .andExpect(jsonPath("$.messages[0].text").value("Referenced DPP missing-dpp was not found in repo " + missingRepoUrl));

        mockMvc.perform(get("/registry/dpps/by-dpp-id/missing-dpp"))
                .andExpect(status().isNotFound());

        repoHeadStatus = 500;
        mockMvc.perform(post("/registerDPP")
                        .contentType(APPLICATION_JSON)
                        .content(registrationBody("product-500", "downstream-dpp", "operator-123", missingRepoUrl)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.statusCode").value("ServerErrorBadGateway"));

        mockMvc.perform(get("/registry/dpps/by-dpp-id/downstream-dpp"))
                .andExpect(status().isNotFound());
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

    private void handleRepoHead(HttpExchange exchange) throws IOException {
        if (!"HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        exchange.sendResponseHeaders(repoHeadStatus, -1);
    }
}
