package demo.repo;

import demo.repo.testsupport.DemoDppFactory;
import demo.repo.testsupport.Dpp4FunDppCodecAdapter;
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Nameplate;
import dppsdk.dpp4fun.model.Dpp4Fun;
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

import java.time.Instant;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
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
    private DppRepoService repoService;

    private final DemoDppFactory factory = new DemoDppFactory();
    private final Dpp4FunDppCodecAdapter codec = new Dpp4FunDppCodecAdapter();

    @BeforeEach
    @AfterEach
    void clearState() {
        repoService.clear();
    }

    @Test
    @DisplayName("PostgreSQL backend preserves create, read, head, and product lookup behavior")
    void postgresModeCreateReadHeadAndProductLookupWork() throws Exception {
        String dppJson = codec.toJson(factory.createValidBedDpp());

        mockMvc.perform(post("/dpps")
                        .contentType(APPLICATION_JSON)
                        .content(dppJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("SuccessCreated"))
                .andExpect(jsonPath("$.payload.dppId").value(DemoDppFactory.BED_DPP_ID));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.characteristics.productName").value("Cir4Fun Platform Bed"));

        mockMvc.perform(head("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(get("/dppsByProductId/04012345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.passportMetadata.uniqueProductIdentifier").value(DemoDppFactory.BED_DPP_ID));
    }

    @Test
    @DisplayName("PostgreSQL backend preserves full update history lookup and batch paging behavior")
    void postgresModePatchHistoryAndBatchPagingWork() throws Exception {
        createValidBed();
        Instant afterCreate = Instant.now();

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "characteristics": {
                                    "productName": "Cir4Fun Platform Bed - Updated Demo"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.characteristics.productName").value("Cir4Fun Platform Bed - Updated Demo"));
        Instant afterPatch = Instant.now();

        mockMvc.perform(get("/dppsByProductIdAndDate/04012345678901")
                        .param("date", afterCreate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.characteristics.productName").value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/dppsByProductIdAndDate/04012345678901")
                        .param("date", afterPatch.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.characteristics.productName").value("Cir4Fun Platform Bed - Updated Demo"));

        Dpp4Fun chair = withProductId(factory.createValidChairDpp(), "04012345678902");
        mockMvc.perform(post("/dpps")
                        .contentType(APPLICATION_JSON)
                        .content(codec.toJson(chair)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/dppsByProductIds")
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

        mockMvc.perform(post("/dppsByProductIds")
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
    @DisplayName("PostgreSQL backend preserves delete semantics and lifecycle event visibility after delete")
    void postgresModeDeleteAndEventsWork() throws Exception {
        createValidBed();

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "characteristics": {
                                    "productName": "Event update"
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("SuccessNoContent"));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/dppsByProductId/04012345678901"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID + "/events"))
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

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/characteristics.productName"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value("Cir4Fun Platform Bed"));

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/characteristics.productName")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": "Updated Product Name from Postgres"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value("Updated Product Name from Postgres"));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.characteristics.productName").value("Updated Product Name from Postgres"));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID + "/events"))
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

        mockMvc.perform(post("/dpps")
                        .contentType(APPLICATION_JSON)
                        .content(codec.toJson(factory.createValidBedDpp())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"))
                .andExpect(jsonPath("$.messages[0].code").value("DPP_CONFLICT"));
    }

    @Test
    @DisplayName("PostgreSQL backend maps duplicate product id to PRODUCT_CONFLICT")
    void postgresModeDuplicateProductIdReturnsConflict() throws Exception {
        createValidBed();
        Dpp4Fun conflictingChair = withProductId(factory.createValidChairDpp(), "04012345678901");

        mockMvc.perform(post("/dpps")
                        .contentType(APPLICATION_JSON)
                        .content(codec.toJson(conflictingChair)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"))
                .andExpect(jsonPath("$.messages[0].code").value("PRODUCT_CONFLICT"));
    }

    private void createValidBed() throws Exception {
        mockMvc.perform(post("/dpps")
                        .contentType(APPLICATION_JSON)
                        .content(codec.toJson(factory.createValidBedDpp())))
                .andExpect(status().isCreated());
    }

    private Dpp4Fun withProductId(Dpp4Fun dpp, String productId) {
        Nameplate nameplate = dpp.getNameplate().toBuilder()
                .gtinCode(productId)
                .build();
        DppCore coreDpp = dpp.getCoreDpp().toBuilder()
                .nameplate(nameplate)
                .build();
        return dpp.toBuilder()
                .coreDpp(coreDpp)
                .build();
    }
}
