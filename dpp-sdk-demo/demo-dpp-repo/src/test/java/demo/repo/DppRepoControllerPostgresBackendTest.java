package demo.repo;

import demo.repo.testsupport.DemoDppFactory;
import demo.repo.testsupport.Dpp4FunTestJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dppsdk.dpp4fun.model.Dpp4Fun;
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

import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.org.springframework=WARN",
        "dpp.repo.backend=postgres"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DppRepoControllerPostgresBackendTest {

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

    @Autowired
    private DppRepoBackend backend;

    private final DemoDppFactory factory = new DemoDppFactory();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void clearState() throws Exception {
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("truncate table dpp_lifecycle_events, dpp_projections, dpp_versions, dpp_passports "
                    + "restart identity cascade");
        }
    }

    @Test
    @DisplayName("PostgreSQL backend preserves create, read, and product lookup behavior")
    void postgresModeCreateReadAndProductLookupWork() throws Exception {
        String dppJson = Dpp4FunTestJson.encode(factory.createValidBedDpp());

        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(APPLICATION_JSON)
                        .content(dppJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("SuccessCreated"))
                .andExpect(jsonPath("$.payload.digitalProductPassportId").value(DemoDppFactory.BED_DPP_ID));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath(DemoDppFactory.BRAND_PATH.replace("$", "$.payload") + ".value")
                        .value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/v1/dppsByProductId/04012345678901")
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.uniqueProductIdentifier").value("04012345678901"));
    }

    @Test
    @DisplayName("PostgreSQL backend serves EN 18223 compressed reads without changing full-DPP storage")
    void postgresModeRepresentationReadsMatchMemoryBehavior() throws Exception {
        createValidBed();

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.digitalProductPassportId").value(DemoDppFactory.BED_DPP_ID))
                .andExpect(jsonPath("$.payload.uniqueProductIdentifier").value("04012345678901"))
                .andExpect(jsonPath("$.payload.elements").doesNotExist());

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.uniqueProductIdentifier").value("04012345678901"));

        mockMvc.perform(get("/v1/dppsByProductId/04012345678901")
                        .param("representation", "compressed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.digitalProductPassportId").value(DemoDppFactory.BED_DPP_ID));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .param("representation", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));
    }

    @Test
    @DisplayName("PostgreSQL backend preserves full update history lookup and batch paging behavior")
    void postgresModePatchHistoryAndBatchPagingWork() throws Exception {
        createValidBed();
        // Keep a small timing buffer so the JDBC/PostgreSQL timestamp boundary is stable for history lookups.
        Thread.sleep(25L);
        Instant afterCreate = Instant.now();
        Thread.sleep(25L);

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID).param("representation", "full")
                        .contentType(APPLICATION_JSON)
                        .content(elementsPatch(factory.withBrand(factory.createValidBedDpp(),
                                "Cir4Fun Platform Bed - Updated Demo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath(DemoDppFactory.BRAND_PATH.replace("$", "$.payload") + ".value")
                        .value("Cir4Fun Platform Bed - Updated Demo"));
        Thread.sleep(25L);
        Instant afterPatch = Instant.now();

        mockMvc.perform(get("/v1/dppsByIdAndDate/" + DemoDppFactory.BED_DPP_ID)
                        .param("date", afterCreate.toString())
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(DemoDppFactory.BRAND_PATH.replace("$", "$.payload") + ".value")
                        .value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/v1/dppsByIdAndDate/" + DemoDppFactory.BED_DPP_ID)
                        .param("date", afterPatch.toString())
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(DemoDppFactory.BRAND_PATH.replace("$", "$.payload") + ".value")
                        .value("Cir4Fun Platform Bed - Updated Demo"));

        Dpp4Fun chair = withProductId(factory.createValidChairDpp(), "04012345678902");
        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(APPLICATION_JSON)
                        .content(Dpp4FunTestJson.encode(chair)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/dppsByProductIds")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": ["04012345678901", "04012345678901"],
                                  "limit": 1,
                                  "cursor": "0"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.dppIdentifiers", hasSize(1)))
                .andExpect(jsonPath("$.payload.dppIdentifiers[0]").value(DemoDppFactory.BED_DPP_ID))
                .andExpect(jsonPath("$.payload.nextCursor").value("1"));

        mockMvc.perform(post("/v1/dppsByProductIds")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": ["04012345678901", "04012345678901", "04012345678901"],
                                  "limit": 2,
                                  "cursor": "1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.dppIdentifiers", hasSize(2)))
                .andExpect(jsonPath("$.payload.dppIdentifiers[0]").value(DemoDppFactory.BED_DPP_ID))
                .andExpect(jsonPath("$.payload.dppIdentifiers[1]").value(DemoDppFactory.BED_DPP_ID))
                .andExpect(jsonPath("$.payload.nextCursor").value("3"));
    }

    @Test
    @DisplayName("PostgreSQL HTTP updates that preserve canonical content do not append revisions or events")
    void postgresModeSameContentUpdatesAreNoOps() throws Exception {
        createValidBed();
        String sameContentPatch = elementsPatch(factory.createValidBedDpp());

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID).param("representation", "full")
                        .contentType(APPLICATION_JSON)
                        .content(sameContentPatch))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/"
                        + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full")
                        .contentType(APPLICATION_JSON)
                        .content("{\"elementId\":\"brand\",\"objectType\":\"SingleValuedDataElement\",\"value\":\"Cir4Fun Platform Bed\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID
                        + "/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full")
                        .contentType(APPLICATION_JSON)
                        .content("{\"elementId\":\"brand\",\"objectType\":\"SingleValuedDataElement\",\"value\":\"Cir4Fun Platform Bed\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/internal/dpps/" + DemoDppFactory.BED_DPP_ID + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", hasSize(1)))
                .andExpect(jsonPath("$.payload[0].eventType").value("DPP_CREATED"));
    }

    @Test
    @DisplayName("PostgreSQL adapter rejects a stale loaded version instead of overwriting the newer revision")
    void postgresBackendRejectsStaleVersionWrites() throws Exception {
        createValidBed();
        CurrentDppSnapshot original = backend.findCurrentSnapshotByDppId(DemoDppFactory.BED_DPP_ID).orElseThrow();
        Dpp4Fun latest = factory.withBrand(Dpp4Fun.from(original.dpp()), "Latest concurrent update");
        Dpp4Fun stale = factory.withBrand(Dpp4Fun.from(original.dpp()), "Stale concurrent update");

        backend.appendVersion(latest.asDigitalProductPassport(), original.versionNo(), Instant.now(), "DPP_UPDATED", Map.of());
        RepoApiException conflict = assertThrows(RepoApiException.class,
                () -> backend.appendVersion(stale.asDigitalProductPassport(), original.versionNo(), Instant.now(), "DPP_UPDATED", Map.of()));

        assertEquals("DPP_VERSION_CONFLICT", conflict.errorCode());
        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID).param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(DemoDppFactory.BRAND_PATH.replace("$", "$.payload") + ".value")
                        .value("Latest concurrent update"));
    }

    @Test
    @DisplayName("PostgreSQL backend preserves delete semantics and lifecycle event visibility after delete")
    void postgresModeDeleteAndEventsWork() throws Exception {
        createValidBed();

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID).param("representation", "full")
                        .contentType(APPLICATION_JSON)
                        .content(elementsPatch(factory.withBrand(factory.createValidBedDpp(), "Event update"))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/v1/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v1/dppsByProductId/04012345678901"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/internal/dpps/" + DemoDppFactory.BED_DPP_ID + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", hasSize(3)))
                .andExpect(jsonPath("$.payload[*].eventType").value(hasItem("DPP_CREATED")))
                .andExpect(jsonPath("$.payload[*].eventType").value(hasItem("DPP_UPDATED")))
                .andExpect(jsonPath("$.payload[*].eventType").value(hasItem("DPP_DELETED")))
                .andExpect(jsonPath("$.payload[0].occurredAt").value(notNullValue()));
    }

    @Test
    @DisplayName("PostgreSQL backend preserves fine-granular reads and records DATA_ELEMENT_UPDATED")
    void postgresModeFineGrainedReadAndUpdateWork() throws Exception {
        createValidBed();

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.value").value("Cir4Fun Platform Bed"));

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"elementId":"brand","objectType":"SingleValuedDataElement","value":"Updated Product Name from Postgres"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.value").value("Updated Product Name from Postgres"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(DemoDppFactory.BRAND_PATH.replace("$", "$.payload") + ".value")
                        .value("Updated Product Name from Postgres"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/$.elements[5].elements[7].value[*]")
                        .param("representation", "full"))
                .andExpect(status().isNotImplemented())
                .andExpect(content().string(""));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/$.elements[3].elements[99]")
                        .param("representation", "full"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/elements[3].elements[0].value")
                        .param("representation", "full"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full")
                        .contentType(APPLICATION_JSON)
                        .content("{\"unexpected\":\"object for string value\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(DemoDppFactory.BRAND_PATH.replace("$", "$.payload") + ".value")
                        .value("Updated Product Name from Postgres"));

        mockMvc.perform(get("/internal/dpps/" + DemoDppFactory.BED_DPP_ID + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", hasSize(2)))
                .andExpect(jsonPath("$.payload[*].eventType").value(hasItem("DPP_CREATED")))
                .andExpect(jsonPath("$.payload[*].eventType").value(hasItem("DATA_ELEMENT_UPDATED")))
                .andExpect(jsonPath("$.payload[*].eventType").value(not(hasItem("DPP_UPDATED"))));
    }

    @Test
    @DisplayName("PostgreSQL backend maps duplicate DPP id to DPP_CONFLICT")
    void postgresModeDuplicateDppIdReturnsConflict() throws Exception {
        createValidBed();

        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(APPLICATION_JSON)
                        .content(Dpp4FunTestJson.encode(factory.createValidBedDpp())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"))
                .andExpect(jsonPath("$.message[0].code").value("DPP_CONFLICT"));
    }

    @Test
    @DisplayName("PostgreSQL backend maps duplicate product id to PRODUCT_CONFLICT")
    void postgresModeDuplicateProductIdReturnsConflict() throws Exception {
        createValidBed();
        Dpp4Fun conflictingChair = withProductId(factory.createValidChairDpp(), "04012345678901");

        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(APPLICATION_JSON)
                        .content(Dpp4FunTestJson.encode(conflictingChair)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"))
                .andExpect(jsonPath("$.message[0].code").value("PRODUCT_CONFLICT"));
    }

    private void createValidBed() throws Exception {
        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(APPLICATION_JSON)
                        .content(Dpp4FunTestJson.encode(factory.createValidBedDpp())))
                .andExpect(status().isCreated());
    }

    private Dpp4Fun withProductId(Dpp4Fun dpp, String productId) {
        return dpp.toBuilder()
                .header(dpp.getHeader().toBuilder().uniqueProductIdentifier(productId).build())
                .nameplate(dpp.getNameplate().toBuilder().gtinCode(productId).build())
                .build();
    }

    private String elementsPatch(Dpp4Fun dpp) throws Exception {
        ObjectNode patch = objectMapper.createObjectNode();
        patch.set("elements", objectMapper.readTree(Dpp4FunTestJson.encode(dpp)).get("elements"));
        return objectMapper.writeValueAsString(patch);
    }
}
