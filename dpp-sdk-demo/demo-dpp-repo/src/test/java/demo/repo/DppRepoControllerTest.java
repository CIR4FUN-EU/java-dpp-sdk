package demo.repo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import demo.repo.testsupport.Dpp4FunTestJson;
import demo.repo.testsupport.DemoDppFactory;
import dppsdk.dpp4fun.model.Dpp4Fun;

@SpringBootTest(properties = {
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.org.springframework=WARN"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DppRepoControllerTest {
    private static final String VENDOR_EXTENSION_PATH = "$.elements[20]";

    @Test
    @DisplayName("Controller keeps Swagger example payloads outside the request-handling class")
    void controllerDoesNotDeclareSwaggerExamplePayloads() {
        assertTrue(java.util.Arrays.stream(DppRepoController.class.getDeclaredFields())
                .noneMatch(field -> field.getName().endsWith("_EXAMPLE")));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryDppStore store;

    @Autowired
    private DppRepoService lifecycleService;

    @Autowired
    private DppRepoBackend backend;

    @Autowired
    private ObjectMapper objectMapper;

    private final DemoDppFactory factory = new DemoDppFactory();

    @BeforeEach
    @AfterEach
    void clearState() {
        store.clear();
    }

    @Test
    @DisplayName("POST /dpps creates a readable active DPP and records the initial snapshot and event")
    void createDppCreatesAndCanBeReadByIdAndProductId() throws Exception {
        String dppJson = Dpp4FunTestJson.encode(factory.createValidBedDpp());

        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dppJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("SuccessCreated"))
                .andExpect(jsonPath("$.payload.digitalProductPassportId").value(DemoDppFactory.BED_DPP_ID))
                .andExpect(jsonPath("$.payload.digitalProductPassportId").value(not("")))
                .andExpect(jsonPath("$.payload").exists());

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload").exists())
                .andExpect(jsonPath("$.payload.elements[5].elements[7].value").value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/v1/dppsByProductId/04012345678901")
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload").exists())
                .andExpect(jsonPath("$.payload.uniqueProductIdentifier").value("04012345678901"));

        mockMvc.perform(get("/internal/dpps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload[0]").value(DemoDppFactory.BED_DPP_ID));

        assertEquals(1, store.versionsForProduct("04012345678901").size());
        assertEquals(1, store.eventsFor(DemoDppFactory.BED_DPP_ID).size());
        assertEquals("DPP_CREATED", store.eventsFor(DemoDppFactory.BED_DPP_ID).get(0).eventType());
    }

    @Test
    void fullBodyRequiresExplicitFullRepresentationAndUnsupportedMethodsUseStandard405Envelope() throws Exception {
        mockMvc.perform(post("/v1/dpps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Dpp4FunTestJson.encode(factory.createValidBedDpp())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));
        assertEquals(0, store.versionsForProduct("04012345678901").size());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .request(org.springframework.http.HttpMethod.PUT, "/v1/dpps"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.statusCode").value("ClientMethodNotAllowed"))
                .andExpect(jsonPath("$.message[0].messageType").value("Error"))
                .andExpect(jsonPath("$.message[0].text").isString());
    }

    @Test
    void unsupportedMethodsOnEveryPublicLifecycleRouteUseStandard405Envelope() throws Exception {
        var unsupportedRequests = List.of(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .request(org.springframework.http.HttpMethod.PUT, "/v1/dpps"),
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .request(org.springframework.http.HttpMethod.POST, "/v1/dpps/example-id"),
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .request(org.springframework.http.HttpMethod.POST, "/v1/dppsByProductId/example-product"),
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .request(org.springframework.http.HttpMethod.POST, "/v1/dppsByIdAndDate/example-id"),
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .request(org.springframework.http.HttpMethod.GET, "/v1/dppsByProductIds"),
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .request(org.springframework.http.HttpMethod.PUT,
                                "/v1/dpps/example-id/elements/$.elements[0]")
        );

        for (var request : unsupportedRequests) {
            mockMvc.perform(request)
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.statusCode").value("ClientMethodNotAllowed"))
                    .andExpect(jsonPath("$.message[0].messageType").value("Error"))
                    .andExpect(jsonPath("$.message[0].text").isString());
        }
    }

    @Test
    @DisplayName("Same-content whole and element updates do not append memory revisions or events")
    void sameContentUpdatesAreNoOpsInMemoryBackend() throws Exception {
        createValidBed();
        String sameContentPatch = elementsPatch(factory.createValidBedDpp());

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID).param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameContentPatch))
                .andExpect(status().isOk());
        assertEquals(1, store.versionsForProduct("04012345678901").size(), "whole update");

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/"
                        + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elementId\":\"brand\",\"objectType\":\"SingleValuedDataElement\",\"value\":\"Cir4Fun Platform Bed\"}"))
                .andExpect(status().isOk());
        assertEquals(1, store.versionsForProduct("04012345678901").size(), "same text element");

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID
                        + "/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elementId\":\"brand\",\"objectType\":\"SingleValuedDataElement\",\"value\":\"Cir4Fun Platform Bed\"}"))
                .andExpect(status().isOk());
        assertEquals(1, store.versionsForProduct("04012345678901").size(), "same numeric element");

        assertEquals(1, store.versionsForProduct("04012345678901").size());
        mockMvc.perform(get("/internal/dpps/" + DemoDppFactory.BED_DPP_ID + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", hasSize(1)))
                .andExpect(jsonPath("$.payload[0].eventType").value("DPP_CREATED"));
    }

    @Test
    @DisplayName("In-memory writes reject a stale snapshot instead of overwriting the newer revision")
    void inMemoryBackendRejectsStaleVersionWrites() throws Exception {
        createValidBed();
        CurrentDppSnapshot original = backend.findCurrentSnapshotByDppId(DemoDppFactory.BED_DPP_ID).orElseThrow();
        Dpp4Fun latest = factory.withBrand(Dpp4Fun.from(original.dpp()), "Latest concurrent update");
        Dpp4Fun stale = factory.withBrand(Dpp4Fun.from(original.dpp()), "Stale concurrent update");

        backend.appendVersion(latest.asDigitalProductPassport(), original.versionNo(), Instant.now(), "DPP_UPDATED", java.util.Map.of());
        RepoApiException conflict = assertThrows(RepoApiException.class,
                () -> backend.appendVersion(stale.asDigitalProductPassport(), original.versionNo(), Instant.now(), "DPP_UPDATED", java.util.Map.of()));

        assertEquals("DPP_VERSION_CONFLICT", conflict.errorCode());
        assertEquals("Latest concurrent update", lifecycleService.readById(DemoDppFactory.BED_DPP_ID)
                .path("elements").get(5).path("elements").get(7).path("value").asText());
    }

    @Test
    @DisplayName("DPP reads default to EN 18223 compressed JSON and accept explicit compressed/full representations")
    void fullDppReadsHonorRepresentationContract() throws Exception {
        createValidBed();
        Instant afterCreate = Instant.now();

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.digitalProductPassportId").value(DemoDppFactory.BED_DPP_ID))
                .andExpect(jsonPath("$.payload.uniqueProductIdentifier").value("04012345678901"))
                .andExpect(jsonPath("$.payload.nameplate.brand").value("Cir4Fun Platform Bed"))
                .andExpect(jsonPath("$.payload.elements").doesNotExist());

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .param("representation", "compressed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.digitalProductPassportId").value(DemoDppFactory.BED_DPP_ID))
                .andExpect(jsonPath("$.payload.nameplate.brand").value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.digitalProductPassportId")
                        .value(DemoDppFactory.BED_DPP_ID))
                .andExpect(jsonPath("$.payload.representation").doesNotExist());

        mockMvc.perform(get("/v1/dppsByProductId/04012345678901")
                        .param("representation", "compressed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.digitalProductPassportId").value(DemoDppFactory.BED_DPP_ID));

        mockMvc.perform(get("/v1/dppsByIdAndDate/" + DemoDppFactory.BED_DPP_ID)
                        .param("date", afterCreate.toString())
                        .param("representation", "compressed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.digitalProductPassportId").value(DemoDppFactory.BED_DPP_ID));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .param("representation", "summary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));
    }

    @Test
    @DisplayName("POST /dpps rejects duplicates and malformed JSON while preserving canonical D4.1 data")
    void createRejectsDuplicateMalformedAndInvalidDpps() throws Exception {
        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Dpp4FunTestJson.encode(factory.createValidBedDpp())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Dpp4FunTestJson.encode(factory.createValidBedDpp())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"));

        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Dpp4FunTestJson.encode(withIdAndProduct(factory.createValidChairDpp(),
                                DemoDppFactory.CHAIR_DPP_ID, "04012345678901", "Chair duplicate product"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"));

        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(factory.createMalformedJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.message[0].correlationId").value(notNullValue()));

        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Dpp4FunTestJson.encode(withIdAndProduct(factory.createValidBedDpp(),
                                "aa71b42d-5d34-4fa8-a809-c38b79ce8871", "04012345678909",
                                "Invalid documentation"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET active DPP endpoints return not found for missing or soft-deleted records")
    void readByIdAndProductIdHandleMissingAndDeletedRecords() throws Exception {
        createValidBed();

        mockMvc.perform(get("/v1/dpps/missing-dpp-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/v1/dppsByProductId/missing-product-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(delete("/v1/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/v1/dppsByProductId/04012345678901"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));
    }

    @Test
    @DisplayName("GET /v1/dppsByIdAndDate returns repository snapshots for the requested DPP id and timestamp")
    void readVersionByDppIdAndDateReturnsHistoricalSnapshots() throws Exception {
        createValidBed();
        // TODO: replace these timing gaps with an injected Clock when the demo services gain a time seam.
        Thread.sleep(25L);
        Instant afterCreate = Instant.now();
        Thread.sleep(25L);

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID).param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementsPatch(factory.withBrand(factory.createValidBedDpp(),
                                "Cir4Fun Platform Bed - Updated Demo"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/dppsByIdAndDate/" + DemoDppFactory.BED_DPP_ID)
                        .param("date", afterCreate.toString())
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.elements[5].elements[7].value").value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/v1/dppsByIdAndDate/" + DemoDppFactory.BED_DPP_ID)
                        .param("date", "2020-01-01T00:00:00Z"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/v1/dppsByIdAndDate/" + DemoDppFactory.BED_DPP_ID)
                        .param("date", Instant.now().toString())
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.elements[5].elements[7].value")
                        .value("Cir4Fun Platform Bed - Updated Demo"));

        mockMvc.perform(get("/v1/dppsByIdAndDate/" + DemoDppFactory.BED_DPP_ID)
                        .param("date", "not-a-timestamp"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(get("/v1/dppsByIdAndDate/missing-dpp-id")
                        .param("date", Instant.now().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));
    }

    @Test
    @DisplayName("Old unprefixed internal repository routes are removed")
    void oldInternalRepositoryRoutesReturnNotFound() throws Exception {
        mockMvc.perform(get("/dpps"))
                .andExpect(status().isNotFound());
        mockMvc.perform(head("/dpps/missing-dpp-id"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/dpps/missing-dpp-id/events"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /dppsByProductIds supports limit and cursor semantics and rejects bad input")
    void readDppIdsByProductIdsSupportsLimitCursorAndRejectsBadInput() throws Exception {
        createValidBed();
        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Dpp4FunTestJson.encode(withIdAndProduct(factory.createValidChairDpp(),
                                DemoDppFactory.CHAIR_DPP_ID, "04012345678902", "Chair v1"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/dppsByProductIds")
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

        mockMvc.perform(post("/v1/dppsByProductIds")
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

        mockMvc.perform(post("/v1/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": ["04012345678901"],
                                  "limit": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.message[0].code").value("INVALID_LIMIT"));

        mockMvc.perform(post("/v1/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": ["04012345678901"],
                                  "cursor": "2"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.message[0].code").value("INVALID_CURSOR"));

        mockMvc.perform(post("/v1/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": ["04012345678901"],
                                  "cursor": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(post("/v1/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": ["04012345678901"],
                                  "cursor": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(post("/v1/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productIdentifiers": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/v1/dppsByProductIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));
    }

    @Test
    @DisplayName("PATCH /dpps/{dppId} applies merge patch atomically and returns the updated full DPP")
    void updateByIdUsesMergePatchAndRemainsAtomicOnValidationFailure() throws Exception {
        createValidBed();

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID).param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementsPatch(factory.withBrand(factory.createValidBedDpp(),
                                "Cir4Fun Platform Bed - Updated Demo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.digitalProductPassportId").value(DemoDppFactory.BED_DPP_ID))
                .andExpect(jsonPath("$.payload.elements[5].elements[7].value")
                        .value("Cir4Fun Platform Bed - Updated Demo"))
                .andExpect(jsonPath("$.payload.elements[5].elements[7].elementId").value("brand"));

        int versionsAfterValidUpdate = store.versionsForProduct("04012345678901").size();

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID).param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "digitalProductPassportId": "%s"
                                }
                                """.formatted(DemoDppFactory.CHAIR_DPP_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.message[0].code").value("DPP_ID_IMMUTABLE"));

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID).param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "uniqueProductIdentifier": "04012345670000"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.message[0].code").value("PRODUCT_ID_IMMUTABLE"));

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID).param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elements": [
                                    {"elementId": "nameplate", "objectType": "UnknownDataElement"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.elements[5].elements[7].value")
                        .value("Cir4Fun Platform Bed - Updated Demo"));

        assertEquals(versionsAfterValidUpdate, store.versionsForProduct("04012345678901").size());
        assertTrue(store.eventsFor(DemoDppFactory.BED_DPP_ID).stream().anyMatch(event -> event.eventType().equals("DPP_UPDATED")));

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID).param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"characteristics\":"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/v1/dpps/missing-dpp-id").param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementsPatch(factory.withBrand(factory.createValidBedDpp(), "Missing"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));
    }

    @Test
    @DisplayName("DELETE /dpps/{dppId} soft deletes the active record and keeps historical snapshots available")
    void deleteByIdSoftDeletesAndRecordsLifecycleEvent() throws Exception {
        createValidBed();
        Instant beforeDelete = Instant.now();
        // TODO: replace this timing gap with an injected Clock when the demo services gain a time seam.
        Thread.sleep(5L);

        mockMvc.perform(delete("/v1/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/internal/dpps/" + DemoDppFactory.BED_DPP_ID + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[*].eventType").value(org.hamcrest.Matchers.hasItem("DPP_DELETED")));

        mockMvc.perform(get("/v1/dppsByIdAndDate/" + DemoDppFactory.BED_DPP_ID)
                        .param("date", beforeDelete.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/v1/dpps/missing-dpp-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        assertEquals(1, store.versionsForProduct("04012345678901").size());
    }

    @Test
    @DisplayName("Version lookup stays deterministic across delete and recreate cycles for the same DPP id")
    void versionLookupUsesDeterministicDppDeletionHistory() {
        Instant firstCreate = Instant.parse("2026-05-11T09:00:00Z");
        Instant firstDelete = Instant.parse("2026-05-11T10:00:00Z");
        Instant secondCreate = Instant.parse("2026-05-11T11:00:00Z");
        String productId = "product-history";

        store.create("dpp-old", productId, "{\"id\":\"old\"}", firstCreate);
        store.softDelete("dpp-old", firstDelete, 1);
        store.create("dpp-new", productId, "{\"id\":\"new\"}", secondCreate);

        assertEquals("dpp-old", store.findVersionByDppIdAndDate("dpp-old", Instant.parse("2026-05-11T09:30:00Z"))
                .orElseThrow().dppId());
        assertTrue(store.findVersionByDppIdAndDate("dpp-old", Instant.parse("2026-05-11T10:30:00Z")).isEmpty());
        assertEquals("dpp-new", store.findVersionByDppIdAndDate("dpp-new", Instant.parse("2026-05-11T11:30:00Z"))
                .orElseThrow().dppId());
    }

    @Test
    @DisplayName("Fine-granular endpoints support the bounded singular JSONPath subset and remain atomic on failure")
    void readAndUpdateDataElementWorkAndRemainAtomic() throws Exception {
        createValidBed();

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.BRAND_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.elementId").value("brand"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.elementId").value("brand"))
                .andExpect(jsonPath("$.payload.value").value("Cir4Fun Platform Bed"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.GTIN_PATH)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.value").value("04012345678901"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.BILL_OF_MATERIALS_PATH)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.elements[0].value", hasSize(2)));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID
                        + "/elements/" + DemoDppFactory.MATERIALS_PATH).param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.objectType").value("MultiValuedDataElement"))
                .andExpect(jsonPath("$.payload.value", hasSize(2)))
                .andExpect(jsonPath("$.payload.value[0].objectType").value("DataElementCollection"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.FIRST_MATERIAL_NAME_PATH)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.value").value(not("")));

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elementId":"brand","objectType":"SingleValuedDataElement","value":"Granular Update Name"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.value").value("Granular Update Name"));

        int versionsAfterValidUpdate = store.versionsForProduct("04012345678901").size();

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.BRAND_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elementId":"brand","objectType":"SingleValuedDataElement","value":"Compressed Update"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                null
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.elements[5].elements[7].value").value("Granular Update Name"));

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unexpected":"object for string value"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID)
                        .param("representation", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.elements[5].elements[7].value").value("Granular Update Name"));

        assertEquals(versionsAfterValidUpdate, store.versionsForProduct("04012345678901").size());
        assertTrue(store.eventsFor(DemoDppFactory.BED_DPP_ID).stream()
                .anyMatch(event -> event.eventType().equals("DATA_ELEMENT_UPDATED")));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/$.unsupported.field")
                        .param("representation", "full"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/$.elements[5].elements[7].value[*]")
                        .param("representation", "full"))
                .andExpect(status().isNotImplemented())
                .andExpect(content().string(""));

        mockMvc.perform(get("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/characteristics.brand")
                        .param("representation", "full"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));

        mockMvc.perform(get("/v1/dpps/missing-dpp-id/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(patch("/v1/dpps/missing-dpp-id/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elementId\":\"brand\",\"objectType\":\"SingleValuedDataElement\",\"value\":\"Missing\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));
    }

    @Test
    void fullFineGranularPatchAmendsAndRemovesOptionalMetadata() throws Exception {
        createValidBedWithVendorExtension();
        String path = "/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/"
                + VENDOR_EXTENSION_PATH;

        mockMvc.perform(patch(path).param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictionaryReference\":\"https://example.org/dictionary/vendor-extension\",\"valueDataType\":\"xsd:string\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.dictionaryReference").value("https://example.org/dictionary/vendor-extension"))
                .andExpect(jsonPath("$.payload.value").value("vendor extension value"));

        mockMvc.perform(patch(path).param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dictionaryReference\":null,\"valueDataType\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.dictionaryReference").doesNotExist())
                .andExpect(jsonPath("$.payload.valueDataType").doesNotExist())
                .andExpect(jsonPath("$.payload.value").value("vendor extension value"));
    }

    @Test
    @DisplayName("GET /internal/dpps/{dppId}/events returns structured lifecycle events and rejects missing DPPs")
    void readEventsReturnsLifecycleEventObjectsAndMissingDppFails() throws Exception {
        createValidBed();

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID).param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(elementsPatch(factory.withBrand(factory.createValidBedDpp(), "Updated for events"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/v1/dpps/" + DemoDppFactory.BED_DPP_ID + "/elements/" + DemoDppFactory.BRAND_PATH)
                        .param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elementId":"brand","objectType":"SingleValuedDataElement","value":"Granular event update"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/v1/dpps/" + DemoDppFactory.BED_DPP_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/internal/dpps/" + DemoDppFactory.BED_DPP_ID + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", hasSize(4)))
                .andExpect(jsonPath("$.payload[0].eventType").value(not("")))
                .andExpect(jsonPath("$.payload[0].occurredAt").value(notNullValue()));

        mockMvc.perform(get("/internal/dpps/missing-dpp-id/events"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));
    }

    @Test
    @DisplayName("GET / redirects to the repository Swagger UI")
    void rootRedirectsToSwaggerUi() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/swagger-ui/index.html"))
                .andExpect(redirectedUrl("/swagger-ui/index.html"))
                .andExpect(content().string(not(containsString("Service is running."))))
                .andExpect(content().string(not(containsString("window.location"))))
                .andExpect(content().string(not(containsString("http-equiv"))))
                .andExpect(content().string(not(containsString("<iframe"))))
                .andExpect(result -> assertTrue(result.getModelAndView() == null));
    }

    @Test
    @DisplayName("GET /health reports the demo repository as UP")
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
                .andExpect(jsonPath("$.info.title").value("DPP Demo Repository API"))
                .andExpect(jsonPath("$.paths['/internal/dpps']").exists())
                .andExpect(jsonPath("$.paths['/internal/dpps/{dppId}'].head").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/dpps']").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].get").exists())
                .andExpect(jsonPath("$.paths['/v1/dppsByProductId/{productId}']").exists())
                .andExpect(jsonPath("$.paths['/v1/dppsByIdAndDate/{dppId}'].get.parameters[?(@.name == 'dppId' && @.example == '77777777-7777-7777-7777-777777777777')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dppsByIdAndDate/{dppId}'].get.parameters[?(@.name == 'date' && @.example == '2026-06-08T10:15:30Z')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].get.parameters[?(@.name == 'representation' && @.schema.default == 'compressed')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dppsByProductId/{productId}'].get.parameters[?(@.name == 'representation' && @.schema.default == 'compressed')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dppsByIdAndDate/{dppId}'].get.parameters[?(@.name == 'representation' && @.schema.default == 'compressed')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dppsByProductIds'].post").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].patch").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].get").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].patch").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].get.parameters[?(@.name == 'representation' && @.schema.default == 'compressed')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].patch.parameters[?(@.name == 'representation' && @.schema.default == 'compressed')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].get.parameters[?(@.name == 'elementIdPath' && @.example == '$.elements[1].elements[7]')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].patch.parameters[?(@.name == 'elementIdPath' && @.example == '$.elements[1].elements[7]')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].get.description").value(containsString("Malformed paths return 400")))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].get.responses['501'].description").value(containsString("bounded singular subset")))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].patch.description").value(containsString("validated before persistence")))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].patch.requestBody.description").value(containsString("expanded DataElement object when representation=full")))
                .andExpect(jsonPath("$.paths['/internal/dpps/{dppId}/events'].get").exists())
                .andExpect(jsonPath("$.paths['/dpps']").doesNotExist())
                .andExpect(jsonPath("$.paths['/dpps/{dppId}/events']").doesNotExist())
                .andExpect(jsonPath("$.tags[?(@.name == 'DPP Repository - Life Cycle API')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'DPP Repository - Internal')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps'].post.tags")
                        .value(org.hamcrest.Matchers.contains("DPP Repository - Life Cycle API")))
                .andExpect(jsonPath("$.paths['/internal/dpps'].get.tags")
                        .value(org.hamcrest.Matchers.contains("DPP Repository - Internal")))
                .andExpect(jsonPath("$.paths['/internal/dpps/{dppId}/events'].get.tags")
                        .value(org.hamcrest.Matchers.contains("DPP Repository - Internal")))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].get.tags[?(@ == 'DPP Repository - Fine Granular API')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].get.parameters[?(@.name == 'dppId' && @.example == '49192c87-20c8-4b6f-88de-48b56ca4c211')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].delete.parameters[?(@.name == 'dppId' && @.example == '33333333-3333-3333-3333-333333333333')]").exists())
                .andExpect(jsonPath("$.paths['/v1/dppsByProductId/{productId}'].get.parameters[?(@.name == 'productId' && @.example == '04012345678901')]").exists())
                .andExpect(jsonPath("$.components.schemas.ReadDppIdsRequest.required")
                        .value(org.hamcrest.Matchers.contains("productIdentifiers")))
                .andExpect(jsonPath("$.components.schemas.DppApiMessage.required")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("messageType", "text")))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].get.responses['200'].content['application/json'].examples['Default compressed representation'].value.payload.exampleValue").value("example"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].get.responses['200'].content['application/json'].examples['Full representation'].value.payload.digitalProductPassportId").value("urn:example:dpp:chair-001"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].get.responses['400'].content['application/json'].example.message[0].code").value("INVALID_REPRESENTATION"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].get.responses['404'].content['application/json'].example.statusCode").value("ClientErrorResourceNotFound"))
                .andExpect(jsonPath("$.paths['/v1/dppsByProductId/{productId}'].get.responses['400'].content['application/json'].example.message[0].code").value("INVALID_REPRESENTATION"))
                .andExpect(jsonPath("$.paths['/v1/dppsByProductId/{productId}'].get.responses['404'].content['application/json'].example.message[0].code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/v1/dppsByIdAndDate/{dppId}'].get.responses['200'].content['application/json'].example.payload.digitalProductPassportId").value("urn:example:dpp:historical-001"))
                .andExpect(jsonPath("$.paths['/v1/dppsByIdAndDate/{dppId}'].get.responses['400'].description").value(containsString("date or representation")))
                .andExpect(jsonPath("$.paths['/v1/dppsByIdAndDate/{dppId}'].get.responses['400'].content['application/json'].example.message[0].code").value("INVALID_DATE"))
                .andExpect(jsonPath("$.paths['/v1/dppsByIdAndDate/{dppId}'].get.responses['404'].content['application/json'].example.message[0].code").value("DPP_VERSION_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/v1/dppsByProductIds'].post.requestBody.description").value(containsString("`productIdentifiers` is required")))
                .andExpect(jsonPath("$.paths['/v1/dppsByProductIds'].post.responses['200'].content['application/json'].example.payload.dppIdentifiers[0]").value("urn:example:dpp:chair-001"))
                .andExpect(jsonPath("$.paths['/v1/dppsByProductIds'].post.responses['400'].content['application/json'].example.message[0].code").value("EMPTY_PRODUCT_IDENTIFIERS"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].patch.parameters[?(@.name == 'dppId')].description")
                        .value(org.hamcrest.Matchers.hasItem(containsString("POST /v1/dpps"))))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].patch.responses['200'].content['application/json'].example.payload.digitalProductPassportId").value("urn:example:dpp:chair-001"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].patch.responses['400'].content['application/json'].example.message[0].code").value("INVALID_PATCH"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].patch.responses['409'].content['application/json'].example.message[0].code").value("DPP_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].delete.responses['204'].description").value(containsString("no response body")))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}'].delete.responses['409'].content['application/json'].example.statusCode").value("ClientResourceConflict"))
                .andExpect(jsonPath("$.paths['/v1/dpps'].post.responses['409'].content['application/json'].example.message[0].code").value("DPP_CONFLICT"))
                .andExpect(jsonPath("$.paths['/v1/dpps'].post.responses['500'].content['application/json'].example.statusCode").value("ServerInternalError"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].get.responses['200'].content['application/json'].examples['Default compressed representation'].value.payload").value("Cir4Fun"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].get.responses['200'].content['application/json'].examples['Full representation'].value.payload.elementId").value("brand"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].get.responses['400'].content['application/json'].example.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].get.responses['404'].content['application/json'].example.message[0].code").value("ELEMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].get.responses['501'].content").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].patch.responses['200'].content['application/json'].examples['Default compressed representation'].value.payload").value("Updated Cir4Fun"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].patch.responses['409'].content['application/json'].example.message[0].code").value("DPP_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.paths['/internal/dpps/{dppId}/events'].get.responses['200'].content['application/json'].example.payload[0].eventType").value("DPP_CREATED"))
                .andExpect(jsonPath("$.paths['/internal/dpps/{dppId}/events'].get.responses['200'].content['application/json'].example.payload[0].dppId").value("urn:example:dpp:chair-001"))
                .andExpect(jsonPath("$.paths['/internal/dpps/{dppId}/events'].get.responses['404'].content['application/json'].example.message[0].code").value("DPP_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/v1/dpps'].post.requestBody.content['application/json'].examples['Full DPP JSON'].value.digitalProductPassportId").value("urn:example:dpp:chair-001"))
                .andExpect(jsonPath("$.paths['/v1/dpps'].post.requestBody.content['application/json'].examples['Full DPP JSON'].value.uniqueProductIdentifier").value("urn:example:product:chair-001"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].patch.requestBody.content['application/json'].examples['Default compressed value'].value").value("Updated Cir4Fun"))
                .andExpect(jsonPath("$.paths['/v1/dpps/{dppId}/elements/{elementIdPath}'].patch.requestBody.content['application/json'].examples['Expanded DataElement (set representation=full)'].value.elementId").value("brand"))
                .andExpect(jsonPath("$.paths['/v1/dpps'].post.responses['405'].content['application/json'].example.statusCode").value("ClientMethodNotAllowed"))
                .andExpect(content().string(containsString("See the official standards for payload requirements.")));
    }

    @Test
    @DisplayName("Generated repository OpenAPI describes batch lookup inputs and outputs accurately")
    void generatedOpenApiDescribesBatchLookupAndBodylessDelete() throws Exception {
        String openApiJson = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode openApi = objectMapper.readTree(openApiJson);

        assertEquals("Find DPPs by product IDs",
                openApi.at("/paths/~1v1~1dppsByProductIds/post/summary").asText());
        assertEquals(1, openApi.at("/components/schemas/ReadDppIdsRequest/properties/productIdentifiers/minItems").asInt());
        assertEquals(1, openApi.at("/components/schemas/ReadDppIdsRequest/properties/limit/minimum").asInt());
        assertEquals("^\\+?\\d+$", openApi.at("/components/schemas/ReadDppIdsRequest/properties/cursor/pattern").asText());
        assertEquals("date-time", openApi.at("/paths/~1v1~1dppsByIdAndDate~1{dppId}/get/parameters/1/schema/format").asText());
        assertTrue(openApi.at("/paths/~1v1~1dppsByProductIds/post/responses/200/content/application~1json/schema/$ref")
                .asText().contains("ReadDppIdsResponse"));
        assertTrue(openApi.at("/paths/~1v1~1dpps/{dppId}/delete/responses/204/content").isMissingNode());
        assertTrue(openApi.at("/paths/~1v1~1dpps/post/responses/201/content/application~1json/schema/$ref")
                .asText().contains("CreateDppResponse"));
        assertTrue(openApi.at("/paths/~1internal~1dpps~1{dppId}~1events/get/responses/200/content/application~1json/schema/$ref")
                .asText().contains("LifecycleEventList"));
        assertTrue(openApi.at("/paths/~1internal~1dpps~1{dppId}~1events/get/responses/200/content/application~1json/example/payload/0/eventId")
                .isTextual());
        assertEquals("string", openApi.at("/components/schemas/CreateDppResponse/properties/digitalProductPassportId/type").asText());
        assertEquals("array", openApi.at("/components/schemas/ReadDppIdsResponse/properties/dppIdentifiers/type").asText());
        assertEquals("string", openApi.at("/components/schemas/LifecycleEventRecord/properties/eventId/type").asText());
        assertEquals("DPP Repository - Internal", openApi.at("/paths/~1health/get/tags/0").asText());
        assertTrue(openApi.at("/components/schemas/DppApiResponse/properties/payload/type").isMissingNode());
        assertTrue(openApi.at("/components/schemas/DppApiResponse/properties/message/nullable").asBoolean());

        Set<String> expectedOperations = Set.of(
                "POST /v1/dpps", "GET /v1/dpps/{dppId}", "PATCH /v1/dpps/{dppId}", "DELETE /v1/dpps/{dppId}",
                "GET /v1/dppsByProductId/{productId}", "GET /v1/dppsByIdAndDate/{dppId}",
                "POST /v1/dppsByProductIds", "GET /v1/dpps/{dppId}/elements/{elementIdPath}",
                "PATCH /v1/dpps/{dppId}/elements/{elementIdPath}", "GET /internal/dpps",
                "GET /internal/dpps/{dppId}/events", "GET /health");
        Set<String> actualOperations = new HashSet<>();
        Set<String> operationIds = new HashSet<>();
        openApi.path("paths").fields().forEachRemaining(path -> path.getValue().fields().forEachRemaining(method -> {
            actualOperations.add(method.getKey().toUpperCase() + " " + path.getKey());
            assertTrue(operationIds.add(method.getValue().path("operationId").asText()),
                    "Duplicate operationId " + method.getValue().path("operationId").asText());
        }));
        assertEquals(expectedOperations, actualOperations);
        assertOpenApiReferencesResolve(openApi, openApi);
    }

    private static void assertOpenApiReferencesResolve(JsonNode document, JsonNode node) {
        if (node.isObject()) {
            JsonNode reference = node.get("$ref");
            if (reference != null && reference.asText().startsWith("#/")) {
                assertTrue(!document.at(reference.asText().substring(1)).isMissingNode(),
                        "Unresolved OpenAPI reference " + reference.asText());
            }
            node.fields().forEachRemaining(entry -> assertOpenApiReferencesResolve(document, entry.getValue()));
        } else if (node.isArray()) {
            node.forEach(child -> assertOpenApiReferencesResolve(document, child));
        }
    }

    @Test
    @DisplayName("Concurrent create and update operations keep version and event history consistent enough for demo use")
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
                        lifecycleService.create(Dpp4FunTestJson.encode(dpp));
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
                        String productId = "0401234567" + String.format("%04d", index);
                        Dpp4Fun updated = withIdAndProduct(factory.createValidBedDpp(), dppId, productId, "Updated");
                        lifecycleService.updateById(dppId,
                                elementsPatch(updated));
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
        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Dpp4FunTestJson.encode(factory.createValidBedDpp())))
                .andExpect(status().isCreated());
    }

    private void createValidBedWithVendorExtension() throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode passport = (com.fasterxml.jackson.databind.node.ObjectNode)
                objectMapper.readTree(Dpp4FunTestJson.encode(factory.createValidBedDpp()));
        passport.withArray("elements").add(objectMapper.readTree("""
                {"elementId":"vendorCompatibleExtension","objectType":"SingleValuedDataElement",
                 "dictionaryReference":"https://example.org/dictionary/vendor-extension",
                 "valueDataType":"xsd:string","value":"vendor extension value"}
                """));
        mockMvc.perform(post("/v1/dpps").param("representation", "full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passport.toString()))
                .andExpect(status().isCreated());
    }

    private String elementsPatch(Dpp4Fun dpp) throws Exception {
        com.fasterxml.jackson.databind.JsonNode canonical = objectMapper.readTree(Dpp4FunTestJson.encode(dpp));
        com.fasterxml.jackson.databind.node.ObjectNode patch = objectMapper.createObjectNode();
        patch.set("elements", canonical.get("elements"));
        return objectMapper.writeValueAsString(patch);
    }

    private Dpp4Fun withIdAndProduct(Dpp4Fun base, String dppId, String productId, String brand) {
        return base.toBuilder()
                .header(base.getHeader().toBuilder()
                        .digitalProductPassportId(dppId)
                        .uniqueProductIdentifier(productId)
                        .build())
                .nameplate(base.getNameplate().toBuilder().gtinCode(productId).brand(brand).build())
                .build();
    }

    private record JsonNodeResponse(com.fasterxml.jackson.databind.JsonNode node) {
        String dppId() {
            return node.get("digitalProductPassportId").asText();
        }
    }
}
