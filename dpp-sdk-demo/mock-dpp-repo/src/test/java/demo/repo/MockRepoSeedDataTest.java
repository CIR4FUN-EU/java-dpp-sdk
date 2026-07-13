package demo.repo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import demo.repo.testsupport.DemoDppFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.org.springframework=WARN"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class MockRepoSeedDataTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("default Postman repo identifiers resolve against the clean in-memory startup state")
    void defaultPostmanRepoIdentifiersExistOnCleanStartup() throws Exception {
        mockMvc.perform(head("/internal/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/$.characteristics.productName"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload").value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/v1/dppsByProductId/04012345678901")
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.passportMetadata.uniqueProductIdentifier").value(DemoDppFactory.BED_DPP_ID));

        mockMvc.perform(head("/internal/dpps/" + PostmanSeedData.DELETE_EXAMPLE_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/internal/dpps/" + PostmanSeedData.LIFECYCLE_DEFAULT_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/internal/dpps/" + PostmanSeedData.LIFECYCLE_DELETE_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/internal/dpps/" + PostmanSeedData.FINE_GRAINED_DEFAULT_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/internal/dpps/" + PostmanSeedData.FINE_GRAINED_DELETE_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/internal/dpps/" + PostmanSeedData.REGISTRY_DELETE_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(delete("/v1/dpps/" + PostmanSeedData.DELETE_EXAMPLE_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("SuccessNoContent"));

        mockMvc.perform(head("/internal/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/internal/dpps/" + PostmanSeedData.DELETE_EXAMPLE_DPP_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("historical Swagger example resolves from isolated clean-start seed data")
    void historicalSwaggerExampleResolvesOnCleanStartup() throws Exception {
        mockMvc.perform(get("/v1/dppsByIdAndDate/" + PostmanSeedData.HISTORICAL_SWAGGER_DPP_ID)
                        .param("date", PostmanSeedData.HISTORICAL_SWAGGER_QUERY_AT)
                        .param("representation", "compressed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.representation").value("compressed"))
                .andExpect(jsonPath("$.payload.dppId").value(PostmanSeedData.HISTORICAL_SWAGGER_DPP_ID));

        mockMvc.perform(get("/v1/dppsByIdAndDate/" + PostmanSeedData.HISTORICAL_SWAGGER_DPP_ID)
                        .param("date", PostmanSeedData.HISTORICAL_SWAGGER_QUERY_AT)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.passportMetadata.uniqueProductIdentifier")
                        .value(PostmanSeedData.HISTORICAL_SWAGGER_DPP_ID));
    }
}
