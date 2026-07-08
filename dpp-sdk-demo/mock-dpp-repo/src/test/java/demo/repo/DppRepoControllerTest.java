package demo.repo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import demo.repo.testsupport.Dpp4FunDppCodecAdapter;
import demo.repo.testsupport.DemoDppFactory;
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.PassportMetadata;
import dppsdk.dpp4fun.model.Dpp4Fun;

@SpringBootTest(properties = {
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.org.springframework=WARN"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DppRepoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryDppStore store;

    @Autowired
    private DppRepoService lifecycleService;

    @Autowired
    private ObjectMapper objectMapper;

    private final DemoDppFactory factory = new DemoDppFactory();
    private final Dpp4FunDppCodecAdapter codec = new Dpp4FunDppCodecAdapter();

    @BeforeEach
    @AfterEach
    void clearState() {
        store.clear();
    }

    @Test
    @DisplayName("POST /dpps creates a readable active DPP and records the initial snapshot and event")
    void createDppCreatesAndCanBeReadByIdAndProductId() throws Exception {
        String dppJson = codec.toJson(factory.createValidBedDpp());

        mockMvc.perform(post("/dpps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dppJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("SuccessCreated"))
                .andExpect(jsonPath("$.payload.dppId").value(DemoDppFactory.BED_DPP_ID))
                .andExpect(jsonPath("$.payload.dppId").value(not("")))
                .andExpect(jsonPath("$.payload").exists());

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload").exists())
                .andExpect(jsonPath("$.payload.characteristics.productName").value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/dppsByProductId/04012345678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload").exists())
                .andExpect(jsonPath("$.payload.passportMetadata.uniqueProductIdentifier").value(DemoDppFactory.BED_DPP_ID));

        assertEquals(1, store.versionsForProduct("04012345678901").size());
        assertEquals(1, store.eventsFor(DemoDppFactory.BED_DPP_ID).size());
        assertEquals("DPP_CREATED", store.eventsFor(DemoDppFactory.BED_DPP_ID).get(0).eventType());
    }

    @Test
    @DisplayName("POST /dpps rejects duplicates, malformed JSON, and SDK validation failures")
    void createRejectsDuplicateMalformedAndInvalidDpps() throws Exception {
        mockMvc.perform(post("/dpps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codec.toJson(factory.createValidBedDpp())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/dpps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codec.toJson(factory.createValidBedDpp())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"));

        mockMvc.perform(post("/dpps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codec.toJson(withIdAndProduct(factory.createValidChairDpp(),
                                DemoDppFactory.CHAIR_DPP_ID, "04012345678901", "Chair duplicate product"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"));

        mockMvc.perform(post("/dpps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(factory.createMalformedJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.messages[0].correlationId").value(notNullValue()));

        mockMvc.perform(post("/dpps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codec.toJson(factory.createDppWithInvalidDocumentation())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0].text").value(containsString("Validation failed")));
    }

    @Test
    @DisplayName("GET active DPP endpoints return not found for missing or soft-deleted records")
    void readByIdAndProductIdHandleMissingAndDeletedRecords() throws Exception {
        createValidBed();

        mockMvc.perform(get("/dpps/missing-dpp-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/dppsByProductId/missing-product-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(delete("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("SuccessNoContent"));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/dppsByProductId/04012345678901"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));
    }

    @Test
    @DisplayName("HEAD /dpps/{dppId} verifies active DPP existence without a response body")
    void headByDppIdVerifiesActiveDppExistence() throws Exception {
        createValidBed();

        mockMvc.perform(head("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mockMvc.perform(head("/dpps/missing-dpp-id"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mockMvc.perform(delete("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk());

        mockMvc.perform(head("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("GET /dppsByProductIdAndDate returns repository snapshots for the requested timestamp")
    void readVersionByProductIdAndDateReturnsHistoricalSnapshots() throws Exception {
        createValidBed();
        // TODO: replace these timing gaps with an injected Clock when the mock services gain a time seam.
        Thread.sleep(25L);
        Instant afterCreate = Instant.now();
        Thread.sleep(25L);

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "characteristics": {
                                    "productName": "Cir4Fun Platform Bed - Updated Demo"
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dppsByProductIdAndDate/04012345678901")
                        .param("date", afterCreate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.characteristics.productName").value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/dppsByProductIdAndDate/04012345678901")
                        .param("date", "2020-01-01T00:00:00Z"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/dppsByProductIdAndDate/04012345678901")
                        .param("date", Instant.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.characteristics.productName").value("Cir4Fun Platform Bed - Updated Demo"));

        mockMvc.perform(get("/dppsByProductIdAndDate/04012345678901")
                        .param("date", "not-a-timestamp"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(get("/dppsByProductIdAndDate/missing-product")
                        .param("date", Instant.now().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));
    }

    @Test
    @DisplayName("POST /dppsByProductIds supports limit and cursor semantics and rejects bad input")
    void readDppIdsByProductIdsSupportsLimitCursorAndRejectsBadInput() throws Exception {
        createValidBed();
        mockMvc.perform(post("/dpps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codec.toJson(withIdAndProduct(factory.createValidChairDpp(),
                                DemoDppFactory.CHAIR_DPP_ID, "04012345678902", "Chair v1"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": ["04012345678901", "04012345678902"],
                                  "limit": 1,
                                  "cursor": "0"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.dppIdentifiers", hasSize(1)))
                .andExpect(jsonPath("$.payload.nextCursor").value("1"));

        mockMvc.perform(post("/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": ["04012345678901", "04012345678902"],
                                  "limit": 1,
                                  "cursor": "1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.dppIdentifiers", hasSize(1)))
                .andExpect(jsonPath("$.payload.nextCursor").doesNotExist());

        mockMvc.perform(post("/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": ["04012345678901"],
                                  "limit": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.messages[0].code").value("INVALID_LIMIT"));

        mockMvc.perform(post("/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": ["04012345678901"],
                                  "cursor": "2"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.messages[0].code").value("INVALID_CURSOR"));

        mockMvc.perform(post("/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": ["04012345678901"],
                                  "cursor": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(post("/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": ["04012345678901"],
                                  "cursor": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(post("/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));
    }

    @Test
    @DisplayName("PATCH /dpps/{dppId} applies merge patch atomically and returns the updated full DPP")
    void updateByIdUsesMergePatchAndRemainsAtomicOnValidationFailure() throws Exception {
        createValidBed();

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "characteristics": {
                                    "productName": "Cir4Fun Platform Bed - Updated Demo"
                                  },
                                  "documentation": {
                                    "safetyInstructionsLink": null
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.passportMetadata.uniqueProductIdentifier").value(DemoDppFactory.BED_DPP_ID))
                .andExpect(jsonPath("$.payload.characteristics.productName").value("Cir4Fun Platform Bed - Updated Demo"))
                .andExpect(jsonPath("$.payload.documentation.safetyInstructionsLink").doesNotExist());

        int versionsAfterValidUpdate = store.versionsForProduct("04012345678901").size();

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passportMetadata": {
                                    "uniqueProductIdentifier": "%s"
                                  }
                                }
                                """.formatted(DemoDppFactory.CHAIR_DPP_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.messages[0].code").value("DPP_ID_IMMUTABLE"));

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nameplate": {
                                    "gtinCode": "04012345670000"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.messages[0].code").value("PRODUCT_ID_IMMUTABLE"));

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nameplate": {
                                    "supplier": {
                                      "role": "DISTRIBUTOR"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.characteristics.productName").value("Cir4Fun Platform Bed - Updated Demo"));

        assertEquals(versionsAfterValidUpdate, store.versionsForProduct("04012345678901").size());
        assertTrue(store.eventsFor(DemoDppFactory.BED_DPP_ID).stream().anyMatch(event -> event.eventType().equals("DPP_UPDATED")));

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characteristics\":"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/dpps/missing-dpp-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "characteristics": {
                                    "productName": "Missing"
                                  }
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));
    }

    @Test
    @DisplayName("DELETE /dpps/{dppId} soft deletes the active record and keeps historical snapshots available")
    void deleteByIdSoftDeletesAndRecordsLifecycleEvent() throws Exception {
        createValidBed();
        Instant beforeDelete = Instant.now();
        // TODO: replace this timing gap with an injected Clock when the mock services gain a time seam.
        Thread.sleep(5L);

        mockMvc.perform(delete("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("SuccessNoContent"));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[*].eventType").value(org.hamcrest.Matchers.hasItem("DPP_DELETED")));

        mockMvc.perform(get("/dppsByProductIdAndDate/04012345678901")
                        .param("date", beforeDelete.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/dpps/missing-dpp-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        assertEquals(1, store.versionsForProduct("04012345678901").size());
    }

    @Test
    @DisplayName("Version lookup stays deterministic across delete and recreate cycles for the same product")
    void versionLookupUsesDeterministicProductDeletionHistory() {
        Instant firstCreate = Instant.parse("2026-05-11T09:00:00Z");
        Instant firstDelete = Instant.parse("2026-05-11T10:00:00Z");
        Instant secondCreate = Instant.parse("2026-05-11T11:00:00Z");
        String productId = "product-history";

        store.create("dpp-old", productId, "{\"id\":\"old\"}", firstCreate);
        store.softDelete("dpp-old", firstDelete);
        store.create("dpp-new", productId, "{\"id\":\"new\"}", secondCreate);

        assertEquals("dpp-old", store.findVersionByProductIdAndDate(productId, Instant.parse("2026-05-11T09:30:00Z"))
                .orElseThrow().dppId());
        assertTrue(store.findVersionByProductIdAndDate(productId, Instant.parse("2026-05-11T10:30:00Z")).isEmpty());
        assertEquals("dpp-new", store.findVersionByProductIdAndDate(productId, Instant.parse("2026-05-11T11:30:00Z"))
                .orElseThrow().dppId());
    }

    @Test
    @DisplayName("Fine-granular read and update endpoints support curated paths and remain atomic on failure")
    void readAndUpdateDataElementWorkAndRemainAtomic() throws Exception {
        createValidBed();

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/characteristics.productName"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/coreDpp.nameplate.gtinCode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value("04012345678901"));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/billOfMaterials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.materials", hasSize(2)));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/billOfMaterials.materials[0].name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value(not("")));

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/characteristics.productName")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": "Granular Update Name"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value("Granular Update Name"));

        int versionsAfterValidUpdate = store.versionsForProduct("04012345678901").size();

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/characteristics.productName")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.characteristics.productName").value("Granular Update Name"));

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/nameplate.supplier.role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": "DISTRIBUTOR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.nameplate.supplier.role").value("SUPPLIER"));

        assertEquals(versionsAfterValidUpdate, store.versionsForProduct("04012345678901").size());
        assertTrue(store.eventsFor(DemoDppFactory.BED_DPP_ID).stream()
                .anyMatch(event -> event.eventType().equals("DATA_ELEMENT_UPDATED")));

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/unsupported.field"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(get("/dpps/missing-dpp-id/elements/characteristics.productName"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(patch("/dpps/missing-dpp-id/elements/characteristics.productName")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": "Missing"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));
    }

    @Test
    @DisplayName("GET /dpps/{dppId}/events returns structured lifecycle events and rejects missing DPPs")
    void readEventsReturnsLifecycleEventObjectsAndMissingDppFails() throws Exception {
        createValidBed();

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "characteristics": {
                                    "productName": "Updated for events"
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/characteristics.productName")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": "Granular event update"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dpps/" + DemoDppFactory.BED_DPP_ID + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", hasSize(4)))
                .andExpect(jsonPath("$.payload[0].eventType").value(not("")))
                .andExpect(jsonPath("$.payload[0].occurredAt").value(notNullValue()));

        mockMvc.perform(get("/dpps/missing-dpp-id/events"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));
    }

    @Test
    @DisplayName("GET / returns a simple repository landing page")
    void rootReturnsRepositoryLandingPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("DPP Repository API")))
                .andExpect(content().string(containsString("Service is running.")))
                .andExpect(content().string(containsString("/swagger-ui/index.html")));
    }

    @Test
    @DisplayName("GET /health reports the repository mock as UP")
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("dpp-repo-api"));
    }

    @Test
    @DisplayName("GET /v3/api-docs exposes repository OpenAPI paths and tags")
    void openApiDocsExposeRepositoryEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("DPP Mock Repository API"))
                .andExpect(jsonPath("$.paths['/dpps']").exists())
                .andExpect(jsonPath("$.paths['/dpps/{dppId}'].get").exists())
                .andExpect(jsonPath("$.paths['/dpps/{dppId}'].head").exists())
                .andExpect(jsonPath("$.paths['/dppsByProductId/{productId}']").exists())
                .andExpect(jsonPath("$.paths['/dppsByProductIdAndDate/{productId}'].get.parameters[?(@.name == 'date')]").exists())
                .andExpect(jsonPath("$.paths['/dppsByProductIds'].post").exists())
                .andExpect(jsonPath("$.paths['/dpps/{dppId}'].patch").exists())
                .andExpect(jsonPath("$.paths['/dpps/{dppId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/dpps/{dppId}/elements/{elementPath}'].get").exists())
                .andExpect(jsonPath("$.paths['/dpps/{dppId}/elements/{elementPath}'].patch").exists())
                .andExpect(jsonPath("$.paths['/dpps/{dppId}/events'].get").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'DPP Repository - Life Cycle API')]").exists())
                .andExpect(jsonPath("$.paths['/dpps/{dppId}/elements/{elementPath}'].get.tags[?(@ == 'DPP Repository - Fine Granular API')]").exists())
                .andExpect(jsonPath("$.paths['/dpps/{dppId}/events'].get.tags[?(@ == 'DPP Repository - Events')]").exists())
                .andExpect(jsonPath("$.paths['/dpps/{dppId}'].get.parameters[?(@.name == 'dppId' && @.example == '49192c87-20c8-4b6f-88de-48b56ca4c211')]").exists())
                .andExpect(jsonPath("$.paths['/dpps/{dppId}'].delete.parameters[?(@.name == 'dppId' && @.example == '33333333-3333-3333-3333-333333333333')]").exists())
                .andExpect(jsonPath("$.paths['/dppsByProductId/{productId}'].get.parameters[?(@.name == 'productId' && @.example == '04012345678901')]").exists())
                .andExpect(jsonPath("$.paths['/dpps'].post.requestBody.content['application/json'].examples['Full DPP JSON'].value.passportMetadata.uniqueProductIdentifier").value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$.paths['/dpps'].post.requestBody.content['application/json'].examples['Full DPP JSON'].value.nameplate.gtinCode").value("04012345678999"));
    }

    @Test
    @DisplayName("Concurrent create and update operations keep version and event history consistent enough for mock use")
    void concurrencyRobustnessCreatesAndUpdatesManyDpps() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> createTasks = IntStream.range(0, 100)
                    .mapToObj(index -> (Callable<Void>) () -> {
                        Dpp4Fun dpp = withIdAndProduct(
                                factory.createValidBedDpp(),
                                UUID.randomUUID().toString(),
                                "0401234567" + String.format("%04d", index),
                                "Product-" + index
                        );
                        lifecycleService.create(codec.toJson(dpp));
                        return null;
                    })
                    .toList();

            List<Future<Void>> createResults = executorService.invokeAll(createTasks);
            for (Future<Void> future : createResults) {
                future.get();
            }

            List<String> createdIds = new ArrayList<>();
            for (int index = 0; index < 100; index++) {
                String productId = "0401234567" + String.format("%04d", index);
                JsonNodeResponse response = new JsonNodeResponse(lifecycleService.readByProductId(productId));
                createdIds.add(response.dppId());
            }

            List<Callable<Void>> updateTasks = IntStream.range(0, 30)
                    .mapToObj(index -> (Callable<Void>) () -> {
                        String dppId = createdIds.get(index);
                        lifecycleService.updateById(dppId, """
                                {
                                  "characteristics": {
                                    "productName": "Updated"
                                  }
                                }
                                """);
                        return null;
                    })
                    .toList();

            for (Future<Void> future : executorService.invokeAll(updateTasks)) {
                future.get();
            }

            long updatedEventCount = createdIds.stream()
                    .flatMap(dppId -> store.eventsFor(dppId).stream())
                    .filter(event -> event.eventType().equals("DPP_UPDATED"))
                    .count();
            int totalVersionCount = IntStream.range(0, 100)
                    .map(index -> store.versionsForProduct("0401234567" + String.format("%04d", index)).size())
                    .sum();

            assertEquals(30, updatedEventCount);
            assertEquals(130, totalVersionCount);
        } finally {
            executorService.shutdownNow();
        }
    }

    private void createValidBed() throws Exception {
        mockMvc.perform(post("/dpps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(codec.toJson(factory.createValidBedDpp())))
                .andExpect(status().isCreated());
    }

    private Dpp4Fun withIdAndProduct(Dpp4Fun base, String dppId, String productId, String productName) {
        PassportMetadata metadata = base.getCoreDpp().getPassportMetadata().toBuilder()
                .uniqueProductIdentifier(UUID.fromString(dppId))
                .build();
        Nameplate nameplate = base.getCoreDpp().getNameplate().toBuilder()
                .gtinCode(productId)
                .build();
        DppCore coreDpp = base.getCoreDpp().toBuilder()
                .passportMetadata(metadata)
                .nameplate(nameplate)
                .build();
        return base.toBuilder()
                .coreDpp(coreDpp)
                .characteristics(base.getCharacteristics().toBuilder().productName(productName).build())
                .build();
    }

    private record JsonNodeResponse(com.fasterxml.jackson.databind.JsonNode node) {
        String dppId() {
            return node.get("passportMetadata").get("uniqueProductIdentifier").asText();
        }
    }
}
