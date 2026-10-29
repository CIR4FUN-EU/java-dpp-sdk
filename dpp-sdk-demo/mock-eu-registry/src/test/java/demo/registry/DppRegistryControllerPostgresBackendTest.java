package demo.registry;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import javax.sql.DataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "demo.repo.public-base-url=http://localhost:18080",
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
        startPostgresIfNeeded();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static void startPostgresIfNeeded() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void clearDatabase() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("truncate table registry_records restart identity cascade");
        }
    }

    @Test
    @DisplayName("PostgreSQL backend preserves register and both registry lookup endpoints")
    void postgresModeRegisterAndLookupWork() throws Exception {
        String repoUrl = "http://127.0.0.1:1/v1";

        String responseBody = mockMvc.perform(post("/v1/registerDPP")
                        .contentType(APPLICATION_JSON)
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
                .andExpect(jsonPath("$.payload.lastUpdatedAt").value(notNullValue()));

        mockMvc.perform(get("/internal/dpps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0]").value("dpp-123"));
    }

    @Test
    @DisplayName("PostgreSQL backend preserves duplicate and missing-record behavior")
    void postgresModeDuplicateAndMissingCasesMatchMemoryMode() throws Exception {
        String repoUrl = "urn:example:dpp-api";

        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(APPLICATION_JSON)
                        .content(registrationBody("product-123", "dpp-123", "operator-123", repoUrl)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(APPLICATION_JSON)
                        .content(registrationBody("product-456", "dpp-123", "operator-456", repoUrl)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"))
                .andExpect(jsonPath("$.message[0].code").value("REGISTRY_CONFLICT"));

        mockMvc.perform(get("/internal/dpps/missing-registry-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"))
                .andExpect(jsonPath("$.message[0].code").value("REGISTRY_NOT_FOUND"));

        mockMvc.perform(get("/internal/dpps/by-dpp-id/missing-dpp-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"))
                .andExpect(jsonPath("$.message[0].code").value("REGISTRY_NOT_FOUND"));
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

}
