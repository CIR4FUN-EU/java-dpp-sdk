package demo.registry;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "demo.repo.public-base-url=http://localhost:18080",
        "debug=false",
        "logging.level.root=WARN",
        "logging.level.org.springframework=WARN"
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DppRegistryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryRegistryStore store;

    @Test
    @DisplayName("Controller keeps Swagger example payloads outside the request-handling class")
    void controllerDoesNotDeclareSwaggerExamplePayloads() {
        assertTrue(java.util.Arrays.stream(DppRegistryController.class.getDeclaredFields())
                .noneMatch(field -> field.getName().endsWith("_EXAMPLE")));
    }

    @org.junit.jupiter.api.AfterEach
    void clearState() { store.clear(); }

    @Test
    @DisplayName("POST /v1/registerDPP returns registrationId and internal lookup endpoints return registry metadata only")
    void validRegistrationReturnsRegistrationIdAndCanBeReadBack() throws Exception {
        String repoUrl = "http://localhost:8080";

        String responseBody = mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-123", "dpp-123", "operator-123", repoUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("SuccessCreated"))
                .andExpect(jsonPath("$.payload.registrationId").value(notNullValue()))
                .andExpect(jsonPath("$.payload.registrationId").value(not("")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String registryId = responseBody.split("\"registrationId\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/internal/dpps/" + registryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.registryIdentifier").value(registryId))
                .andExpect(jsonPath("$.payload.dppIdentifier").value("dpp-123"))
                .andExpect(jsonPath("$.payload.productIdentifier").value("product-123"))
                .andExpect(jsonPath("$.payload.operatorIdentifier").value("operator-123"))
                .andExpect(jsonPath("$.payload.repoUrl").value(repoUrl))
                .andExpect(jsonPath("$.payload.registeredAt").value(notNullValue()))
                .andExpect(jsonPath("$.payload.lastUpdatedAt").value(notNullValue()))
                .andExpect(jsonPath("$.payload.characteristics").doesNotExist())
                .andExpect(jsonPath("$.payload.passportMetadata").doesNotExist())
                .andExpect(jsonPath("$.payload.backupOperatorIdentifier").doesNotExist());

        mockMvc.perform(get("/internal/dpps/by-dpp-id/dpp-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload.registryIdentifier").value(registryId))
                .andExpect(jsonPath("$.payload.dppIdentifier").value("dpp-123"))
                .andExpect(jsonPath("$.payload.productIdentifier").value("product-123"))
                .andExpect(jsonPath("$.payload.operatorIdentifier").value("operator-123"))
                .andExpect(jsonPath("$.payload.repoUrl").value(repoUrl))
                .andExpect(jsonPath("$.payload.registeredAt").value(notNullValue()))
                .andExpect(jsonPath("$.payload.lastUpdatedAt").value(notNullValue()))
                .andExpect(jsonPath("$.payload.characteristics").doesNotExist())
                .andExpect(jsonPath("$.payload.passportMetadata").doesNotExist())
                .andExpect(jsonPath("$.payload.backupOperatorIdentifier").doesNotExist());

        mockMvc.perform(get("/internal/dpps/missing-registry-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/internal/dpps/by-dpp-id/missing-dpp-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorResourceNotFound"));

        mockMvc.perform(get("/internal/dpps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("Success"))
                .andExpect(jsonPath("$.payload[0]").value("dpp-123"));
    }

    @Test
    @DisplayName("registration stores metadata without contacting an unreachable DPP API endpoint")
    void registrationDoesNotRequireRepositoryReachability() throws Exception {
        String unreachableEndpoint = "http://127.0.0.1:1/v1";

        String responseBody = mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-unreachable", "dpp-unreachable", "operator-123", unreachableEndpoint)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("SuccessCreated"))
                .andExpect(jsonPath("$.payload.registrationId").value(notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String registryId = responseBody.split("\"registrationId\":\"")[1].split("\"")[0];
        mockMvc.perform(get("/internal/dpps/" + registryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.repoUrl").value(unreachableEndpoint));
    }

    @Test
    @DisplayName("registry accepts a syntactically valid URI without imposing an HTTP transport")
    void registrationAcceptsNonHttpUri() throws Exception {
        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-uri", "dpp-uri", "operator-uri", "urn:example:dpp-api")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("SuccessCreated"));

        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-relative", "dpp-relative", "operator-relative", "../dpps/api")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("SuccessCreated"));
    }

    @Test
    @DisplayName("POST /v1/registerDPP rejects blank standard fields and duplicate DPP ids")
    void registrationValidatesRequiredFieldsAndRejectsDuplicates() throws Exception {
        String repoUrl = "http://localhost:8080";

        assertMissingRequiredFieldFails("uniqueProductIdentifier");
        assertMissingRequiredFieldFails("digitalProductPassportId");
        assertMissingRequiredFieldFails("uniqueEconomicOperatorIdentifier");
        assertMissingRequiredFieldFails("dppApiEndpoint");

        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-123", "dpp-123", "operator-123", repoUrl)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-456", "dpp-123", "operator-456", "http://localhost:65530")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("ClientResourceConflict"));
    }

    @Test
    void registrationRejectsMalformedEndpointUris() throws Exception {
        for (String endpoint : new String[]{"http://bad host", "http://[bad"}) {
            mockMvc.perform(post("/v1/registerDPP")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationBody("product-uri", "dpp-uri", "operator-uri", endpoint)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"));
        }
        assertTrue(store.findByDppId("dpp-uri").isEmpty());
    }

    @Test
    void malformedEndpointUriUsesBadRequest() throws Exception {
        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody("product-uri", "dpp-uri-syntax", "operator-uri", "http://[bad")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message[0].code").value("INVALID_DPP_API_ENDPOINT"));
    }

    @Test
    void unsupportedMethodOnRegistrationUsesStandard405ErrorEnvelope() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .request(org.springframework.http.HttpMethod.GET, "/v1/registerDPP"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.statusCode").value("ClientMethodNotAllowed"))
                .andExpect(jsonPath("$.message[0].messageType").value("Error"))
                .andExpect(jsonPath("$.message[0].text").isString());
    }

    @Test
    @DisplayName("GET / redirects to the registry Swagger UI")
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
    @DisplayName("GET /health reports the registry mock as UP")
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("dpp-registry-api"));
    }

    @Test
    @DisplayName("GET /v3/api-docs exposes registry OpenAPI paths and tag")
    void openApiDocsExposeRegistryEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("DPP Mock Registry API"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post").exists())
                .andExpect(jsonPath("$.paths['/internal/dpps/{registryId}'].get").exists())
                .andExpect(jsonPath("$.paths['/internal/dpps/by-dpp-id/{dppId}'].get").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'DPP Registry')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'DPP Registry - Internal')]").exists())
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.tags")
                        .value(org.hamcrest.Matchers.contains("DPP Registry")))
                .andExpect(jsonPath("$.paths['/internal/dpps'].get.tags")
                        .value(org.hamcrest.Matchers.contains("DPP Registry - Internal")))
                .andExpect(jsonPath("$.paths['/internal/dpps/{registryId}'].get.tags")
                        .value(org.hamcrest.Matchers.contains("DPP Registry - Internal")))
                .andExpect(jsonPath("$.paths['/internal/dpps/by-dpp-id/{dppId}'].get.tags")
                        .value(org.hamcrest.Matchers.contains("DPP Registry - Internal")))
                .andExpect(jsonPath("$.paths['/internal/dpps/{registryId}'].get.parameters[?(@.name == 'registryId' && @.example == '8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc')]").exists())
                .andExpect(jsonPath("$.paths['/internal/dpps/by-dpp-id/{dppId}'].get.parameters[?(@.name == 'dppId' && @.example == 'e7d64b7b-18f2-4d77-9c41-2fa1d1d6b8aa')]").exists())
                .andExpect(jsonPath("$.paths['/internal/dpps'].get").exists())
                .andExpect(jsonPath("$.paths['/registry/dpps']").doesNotExist())
                .andExpect(jsonPath("$.paths['/registry/dpps/{registryId}']").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.RegisterDppRequest.required")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                "uniqueProductIdentifier",
                                "digitalProductPassportId",
                                "uniqueEconomicOperatorIdentifier",
                                "dppApiEndpoint")))
                .andExpect(jsonPath("$.components.schemas.DppApiMessage.required")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("messageType", "text")))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.requestBody.content['application/json'].examples['Register DPP metadata'].value.digitalProductPassportId").value("49192c87-20c8-4b6f-88de-48b56ca4c211"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.requestBody.content['application/json'].examples['Register DPP metadata'].value.uniqueProductIdentifier").value("04012345678901"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['201'].content['application/json'].example.statusCode").value("SuccessCreated"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['201'].content['application/json'].example.payload.registrationId").value("8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['201'].content['application/json'].example.payload.registryIdentifier").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['400'].content['application/json'].example.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['405'].content['application/json'].example.statusCode").value("ClientMethodNotAllowed"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['409'].content['application/json'].example.statusCode").value("ClientResourceConflict"))
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['404']").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['502']").doesNotExist())
                .andExpect(jsonPath("$.paths['/v1/registerDPP'].post.responses['500'].content['application/json'].example.statusCode").value("ServerInternalError"))
                .andExpect(jsonPath("$.paths['/internal/dpps/{registryId}'].get.responses['200'].content['application/json'].example.payload.registeredAt").value("2026-05-19T00:00:00Z"))
                .andExpect(jsonPath("$.paths['/internal/dpps/{registryId}'].get.responses['200'].content['application/json'].example.payload.lastUpdatedAt").exists())
                .andExpect(jsonPath("$.paths['/internal/dpps/{registryId}'].get.responses['404'].content['application/json'].example.message[0].code").value("REGISTRY_NOT_FOUND"))
                .andExpect(jsonPath("$.paths['/internal/dpps/by-dpp-id/{dppId}'].get.responses['200'].content['application/json'].example.payload.registryIdentifier").value("8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc"))
                .andExpect(jsonPath("$.paths['/internal/dpps/by-dpp-id/{dppId}'].get.responses['200'].content['application/json'].example.payload.registrationId").doesNotExist())
                .andExpect(jsonPath("$.paths['/internal/dpps/by-dpp-id/{dppId}'].get.responses['500'].content['application/json'].example.statusCode").value("ServerInternalError"));
    }

    @Test
    @DisplayName("Generated registry OpenAPI describes metadata fields and typed response payloads")
    void generatedOpenApiDescribesRegistryMetadataContract() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode openApi = objectMapper.readTree(json);

        assertTrue(openApi.at("/paths/~1v1~1registerDPP/post/responses/201/content/application~1json/schema/$ref")
                .asText().contains("RegisterDppResponse"));
        assertTrue(openApi.at("/paths/~1internal~1dpps~1{registryId}/get/responses/200/content/application~1json/schema/$ref")
                .asText().contains("RegistryRecordPayload"));
        assertEquals("string", openApi.at("/components/schemas/RegisterDppResponse/properties/registrationId/type").asText());
        assertEquals("string", openApi.at("/components/schemas/RegistryRecordPayload/properties/registryIdentifier/type").asText());
        assertEquals("date-time", openApi.at("/components/schemas/RegistryRecordPayload/properties/registeredAt/format").asText());
        assertEquals("DPP Registry - Internal", openApi.at("/paths/~1health/get/tags/0").asText());
        assertTrue(openApi.at("/components/schemas/RegisterDppRequest/properties/uniqueProductIdentifier/description")
                .asText().contains("product ID"));
        assertTrue(openApi.at("/components/schemas/RegisterDppRequest/properties/digitalProductPassportId/description")
                .asText().contains("DPP ID"));
        assertTrue(openApi.at("/components/schemas/RegisterDppRequest/properties/uniqueEconomicOperatorIdentifier/description")
                .asText().contains("economic operator"));
        assertEquals("uri", openApi.at("/components/schemas/RegisterDppRequest/properties/dppApiEndpoint/format").asText());
        assertTrue(openApi.at("/components/schemas/RegisterDppRequest/properties/dppApiEndpoint/description")
                .asText().contains("does not contact"));
        assertEquals(1, openApi.at("/components/schemas/RegisterDppRequest/properties/uniqueProductIdentifier/minLength").asInt());
        assertEquals(".*\\S.*", openApi.at("/components/schemas/RegisterDppRequest/properties/dppApiEndpoint/pattern").asText());
        assertTrue(openApi.at("/components/schemas/DppApiResponse/properties/payload/type").isMissingNode());
        assertTrue(openApi.at("/components/schemas/DppApiResponse/properties/message/nullable").asBoolean());

        Set<String> expectedOperations = Set.of(
                "POST /v1/registerDPP", "GET /internal/dpps", "GET /internal/dpps/{registryId}",
                "GET /internal/dpps/by-dpp-id/{dppId}", "GET /health");
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

    private void assertMissingRequiredFieldFails(String fieldName) throws Exception {
        String request = registrationBody("product-123", "dpp-123", "operator-123", "http://localhost:8080")
                .replace("\"" + fieldName + "\": \"" + valueFor(fieldName) + "\"", "\"" + fieldName + "\": \" \"");

        mockMvc.perform(post("/v1/registerDPP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ClientErrorBadRequest"))
                .andExpect(jsonPath("$.message[0].correlationId").value(notNullValue()));
    }

    private String valueFor(String fieldName) {
        return switch (fieldName) {
            case "uniqueProductIdentifier" -> "product-123";
            case "digitalProductPassportId" -> "dpp-123";
            case "uniqueEconomicOperatorIdentifier" -> "operator-123";
            case "dppApiEndpoint" -> "http://localhost:8080";
            default -> throw new IllegalArgumentException("Unsupported field " + fieldName);
        };
    }

    private String registrationBody(String productId, String dppId, String operatorId, String repoUrl) {
        return """
                {
                  "uniqueProductIdentifier": "%s",
                  "digitalProductPassportId": "%s",
                  "uniqueEconomicOperatorIdentifier": "%s",
                  "dppApiEndpoint": "%s"
                }
                """.formatted(productId, dppId, operatorId, repoUrl);
    }

    @Test
    @DisplayName("Old registry-prefixed internal routes are removed")
    void oldInternalRegistryRoutesReturnNotFound() throws Exception {
        mockMvc.perform(get("/registry/dpps"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/registry/dpps/missing-registry-id"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/registry/dpps/by-dpp-id/missing-dpp-id"))
                .andExpect(status().isNotFound());
    }

}
