package demo.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import dppsdk.core.model.DataElement;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.transport.json.DppJsonCodec;
import dppsdk.postgres.core.PostgresDppRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "debug=false",
        "logging.level.root=WARN",
        "dpp.repo.backend=postgres",
        "dpp.repo.profile=core",
        "dpp.repo.events.enabled=true"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CoreProfilePostgresBackendTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired PostgresDppRepository repository;
    private final DppJsonCodec codec = new DppJsonCodec();
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach void clearState() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("truncate table dpp_lifecycle_events, dpp_versions, dpp_passports restart identity cascade");
        }
    }

    @Test
    void coreProfileAcceptsGenericDppAndComposesPostgresHistoryAndEvents() throws Exception {
        DigitalProductPassport passport = genericPassport();
        String json = codec.toExpandedJson(passport);
        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        var currentSnapshot = repository.findCurrentSnapshotByDppId("core-profile-1").orElseThrow();
        assertEquals(passport, currentSnapshot.passport());
        assertEquals(1L, currentSnapshot.versionNo());

        mockMvc.perform(get("/v1/dpps/core-profile-1").param("representation", "full"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.payload.elements", hasSize(1)));
        mockMvc.perform(patch("/v1/dpps/core-profile-1").param("representation", "full").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elements\":[{\"elementId\":\"generic\",\"objectType\":\"SingleValuedDataElement\",\"value\":\"changed\"}]}"))
                .andExpect(status().isOk());
        assertEquals(2, repository.findHistoryByDppId("core-profile-1").size());

        mockMvc.perform(get("/internal/dpps/core-profile-1/events"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.payload", hasSize(2)));
        mockMvc.perform(delete("/v1/dpps/core-profile-1")).andExpect(status().isNoContent());
        mockMvc.perform(get("/v1/dpps/core-profile-1").param("representation", "full"))
                .andExpect(status().isNotFound());
        assertEquals(2, repository.findHistoryByDppId("core-profile-1").size());
    }

    @Autowired javax.sql.DataSource dataSource;

    private DigitalProductPassport genericPassport() {
        DppHeader header = new DppHeader.Builder().digitalProductPassportId("core-profile-1")
                .uniqueProductIdentifier("urn:test:core-profile-product").granularity(Granularity.MODEL)
                .dppSchemaVersion("1").dppStatus("active").lastUpdated(Instant.parse("2026-01-01T00:00:00Z"))
                .economicOperatorId("operator").build();
        DataElement element = new SingleValuedDataElement.Builder().elementId("generic").value("initial").build();
        return new DigitalProductPassport.Builder().dppHeader(header).dataElements(List.of(element)).build();
    }
}
