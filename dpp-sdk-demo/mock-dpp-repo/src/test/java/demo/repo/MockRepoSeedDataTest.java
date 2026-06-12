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
        mockMvc.perform(head("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/characteristics.productName"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload").value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/dppsByProductId/04012345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.passportMetadata.uniqueProductIdentifier").value(DemoDppFactory.BED_DPP_ID));

        mockMvc.perform(head("/dpps/" + PostmanSeedData.DELETE_EXAMPLE_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/dpps/" + PostmanSeedData.LIFECYCLE_DEFAULT_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/dpps/" + PostmanSeedData.LIFECYCLE_DELETE_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/dpps/" + PostmanSeedData.FINE_GRAINED_DEFAULT_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/dpps/" + PostmanSeedData.FINE_GRAINED_DELETE_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/dpps/" + PostmanSeedData.REGISTRY_DELETE_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(delete("/dpps/" + PostmanSeedData.DELETE_EXAMPLE_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("SuccessNoContent"));

        mockMvc.perform(head("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/dpps/" + PostmanSeedData.DELETE_EXAMPLE_DPP_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }
}
