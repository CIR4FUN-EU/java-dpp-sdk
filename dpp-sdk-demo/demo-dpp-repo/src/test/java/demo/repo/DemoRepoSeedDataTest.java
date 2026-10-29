package demo.repo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class DemoRepoSeedDataTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("default Postman repo identifiers resolve against the clean in-memory startup state")
    void defaultPostmanRepoIdentifiersExistOnCleanStartup() throws Exception {
        mockMvc.perform(get("/internal/dpps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", org.hamcrest.Matchers.hasItem(DemoDppFactory.BED_DPP_ID)));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + PostmanSeedData.BRAND_PATH)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.value").value("Cir4Fun"));

        mockMvc.perform(get("/v1/dppsByProductId/04012345678901")
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.uniqueProductIdentifier").value("04012345678901"));

        mockMvc.perform(get("/internal/dpps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", org.hamcrest.Matchers.hasItems(
                        PostmanSeedData.DELETE_EXAMPLE_DPP_ID,
                        PostmanSeedData.LIFECYCLE_DEFAULT_DPP_ID,
                        PostmanSeedData.LIFECYCLE_DELETE_DPP_ID,
                        PostmanSeedData.FINE_GRAINED_DEFAULT_DPP_ID,
                        PostmanSeedData.FINE_GRAINED_DELETE_DPP_ID,
                        PostmanSeedData.REGISTRY_DELETE_DPP_ID)));

        mockMvc.perform(delete("/v1/dpps/" + PostmanSeedData.DELETE_EXAMPLE_DPP_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/internal/dpps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", org.hamcrest.Matchers.hasItem(DemoDppFactory.BED_DPP_ID)))
                .andExpect(jsonPath("$.payload", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(PostmanSeedData.DELETE_EXAMPLE_DPP_ID))));
    }

    @Test
    @DisplayName("historical Swagger example resolves from isolated clean-start seed data")
    void historicalSwaggerExampleResolvesOnCleanStartup() throws Exception {
        mockMvc.perform(get("/v1/dppsByIdAndDate/" + PostmanSeedData.HISTORICAL_SWAGGER_DPP_ID)
                        .param("date", PostmanSeedData.HISTORICAL_SWAGGER_QUERY_AT)
                        .param("representation", "compressed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.digitalProductPassportId")
                        .value(PostmanSeedData.HISTORICAL_SWAGGER_DPP_ID))
                .andExpect(jsonPath("$.payload.elements").doesNotExist());

        mockMvc.perform(get("/v1/dppsByIdAndDate/" + PostmanSeedData.HISTORICAL_SWAGGER_DPP_ID)
                        .param("date", PostmanSeedData.HISTORICAL_SWAGGER_QUERY_AT)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.digitalProductPassportId")
                        .value(PostmanSeedData.HISTORICAL_SWAGGER_DPP_ID));
    }
}
