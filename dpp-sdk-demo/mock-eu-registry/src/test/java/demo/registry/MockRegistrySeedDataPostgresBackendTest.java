package demo.registry;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "DPP_REPO_PORT=18080",
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.org.springframework=WARN",
        "dpp.registry.backend=postgres"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Testcontainers(disabledWithoutDocker = true)
class MockRegistrySeedDataPostgresBackendTest {

    static final String SEEDED_REGISTRY_ID = "8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc";
    static final String SEEDED_DPP_ID = "e7d64b7b-18f2-4d77-9c41-2fa1d1d6b8aa";
    static final String SEEDED_PRODUCT_ID = "04012345678902";

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

    @Test
    @DisplayName("default Postman registry identifiers resolve against clean PostgreSQL startup state")
    void defaultPostmanRegistryIdentifiersExistOnCleanPostgresStartup() throws Exception {
        mockMvc.perform(get("/registry/dpps/" + SEEDED_REGISTRY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.registryIdentifier").value(SEEDED_REGISTRY_ID))
                .andExpect(jsonPath("$.payload.dppIdentifier").value(SEEDED_DPP_ID))
                .andExpect(jsonPath("$.payload.productIdentifier").value(SEEDED_PRODUCT_ID))
                .andExpect(jsonPath("$.payload.operatorIdentifier").value("operator-123"))
                .andExpect(jsonPath("$.payload.repoUrl").value("http://localhost:18080"));

        mockMvc.perform(get("/registry/dpps/by-dpp-id/" + SEEDED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.registryIdentifier").value(SEEDED_REGISTRY_ID));
    }
}
