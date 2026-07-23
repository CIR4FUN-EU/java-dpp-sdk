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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "DPP_REPO_PORT=18080",
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.org.springframework=WARN"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class MockRegistrySeedDataTest {

    static final String SEEDED_REGISTRY_ID = "8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc";
    static final String SEEDED_DPP_ID = "e7d64b7b-18f2-4d77-9c41-2fa1d1d6b8aa";
    static final String SEEDED_PRODUCT_ID = "04012345678902";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("default Postman registry identifiers resolve against the clean in-memory startup state")
    void defaultPostmanRegistryIdentifiersExistOnCleanStartup() throws Exception {
        mockMvc.perform(get("/internal/dpps/" + SEEDED_REGISTRY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.registryIdentifier").value(SEEDED_REGISTRY_ID))
                .andExpect(jsonPath("$.payload.dppIdentifier").value(SEEDED_DPP_ID))
                .andExpect(jsonPath("$.payload.productIdentifier").value(SEEDED_PRODUCT_ID))
                .andExpect(jsonPath("$.payload.operatorIdentifier").value("operator-123"))
                .andExpect(jsonPath("$.payload.repoUrl").value("http://localhost:18080"));

        mockMvc.perform(get("/internal/dpps/by-dpp-id/" + SEEDED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.registryIdentifier").value(SEEDED_REGISTRY_ID));
    }
}
