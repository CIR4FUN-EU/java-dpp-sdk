package dpp.repo.client;

import com.fasterxml.jackson.databind.JsonNode;
import dpp.repo.client.ClientTestSupport.CountingCodec;
import dpp.repo.client.ClientTestSupport.CountingValidator;
import dpp.repo.client.ClientTestSupport.TestDpp;
import dpp.repo.client.ClientTestSupport.TestServer;
import dpp.repo.client.exception.DppMappingClientException;
import dpp.repo.client.exception.DppValidationClientException;
import dpp.repo.payloads.CreateDppResponse;
import dpp.repo.payloads.DeleteDppResponse;
import dpp.repo.payloads.DppStatusCode;
import dpp.repo.payloads.ReadDppIdsResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static dpp.repo.client.ClientTestSupport.objectMapper;
import static dpp.repo.client.ClientTestSupport.requestBody;
import static dpp.repo.client.ClientTestSupport.respond;
import static dpp.repo.client.ClientTestSupport.server;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpDppRepoClientTest {
    @Test
    void repositoryMethodsUseEn18222PathsAndGenericPayloadBoundary() throws Exception {
        try (TestServer server = server()) {
            CountingValidator validator = new CountingValidator();
            CountingCodec codec = new CountingCodec();
            AtomicInteger requestCount = new AtomicInteger();
            JsonNode patch = objectMapper().readTree("{\"characteristics\":{\"productName\":\"Updated Name\"}}");

            server.httpServer().createContext("/v1/dpps", exchange -> {
                requestCount.incrementAndGet();
                assertEquals("POST", exchange.getRequestMethod());
                assertEquals("{\"id\":\"dpp-1\",\"name\":\"chair\"}", requestBody(exchange));
                respond(exchange, 201, "{\"statusCode\":\"SuccessCreated\",\"payload\":{\"dppId\":\"dpp-1\"},\"messages\":[]}");
            });
            server.httpServer().createContext("/v1/dpps/dpp-1", exchange -> {
                requestCount.incrementAndGet();
                switch (exchange.getRequestMethod()) {
                    case "GET" -> {
                        if ("representation=compressed".equals(exchange.getRequestURI().getRawQuery())) {
                            respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":{\"representation\":\"compressed\",\"dppId\":\"dpp-1\"},\"messages\":[]}");
                        } else {
                            assertEquals("representation=full", exchange.getRequestURI().getRawQuery());
                            respond(exchange, 200, successDppBody("dpp-1", "chair"));
                        }
                    }
                    case "PATCH" -> {
                        assertEquals(objectMapper().writeValueAsString(patch), requestBody(exchange));
                        respond(exchange, 200, successDppBody("dpp-1", "Updated Name"));
                    }
                    case "DELETE" -> respond(exchange, 200, "{\"statusCode\":\"SuccessNoContent\",\"payload\":null,\"messages\":[]}");
                    default -> respond(exchange, 405, "{\"statusCode\":\"ClientMethodNotAllowed\",\"payload\":null,\"messages\":[]}");
                }
            });
            server.httpServer().createContext("/v1/dppsByProductId/product-1", exchange -> {
                requestCount.incrementAndGet();
                assertEquals("GET", exchange.getRequestMethod());
                assertEquals("representation=full", exchange.getRequestURI().getRawQuery());
                respond(exchange, 200, successDppBody("dpp-1", "chair"));
            });
            server.httpServer().createContext("/v1/dppsByIdAndDate/dpp-1", exchange -> {
                requestCount.incrementAndGet();
                assertEquals("date=2026-05-11T09%3A30%3A15Z&representation=full", exchange.getRequestURI().getRawQuery());
                respond(exchange, 200, successDppBody("dpp-1", "chair"));
            });
            server.httpServer().createContext("/v1/dppsByProductIds", exchange -> {
                requestCount.incrementAndGet();
                assertEquals("POST", exchange.getRequestMethod());
                assertEquals(
                        "{\"productIdentifiers\":[\"product-1\",\"product-2\"],\"limit\":2,\"cursor\":\"cursor-1\"}",
                        requestBody(exchange)
                );
                respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":{\"dppIdentifiers\":[\"dpp-1\",\"dpp-2\"],\"nextCursor\":\"cursor-2\"},\"messages\":[]}");
            });
            server.httpServer().createContext("/v1/dpps/dpp-1/elements/$.characteristics.productName", exchange -> {
                requestCount.incrementAndGet();
                switch (exchange.getRequestMethod()) {
                    case "GET" -> respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":\"chair\",\"messages\":[]}");
                    case "PATCH" -> {
                        assertEquals("\"Updated Name\"", requestBody(exchange));
                        respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":\"Updated Name\",\"messages\":[]}");
                    }
                    default -> respond(exchange, 405, "{\"statusCode\":\"ClientMethodNotAllowed\",\"payload\":null,\"messages\":[]}");
                }
            });

            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), codec, validator);

            CreateDppResponse created = client.createDpp(new TestDpp("dpp-1", "chair"));
            TestDpp byId = client.readDppById("dpp-1");
            JsonNode compressed = client.readCompressedDppById("dpp-1");
            TestDpp byProductId = client.readDppByProductId("product-1");
            TestDpp byDate = client.readDppVersionByIdAndDate("dpp-1", Instant.parse("2026-05-11T09:30:15Z"));
            ReadDppIdsResponse ids = client.readDppIdsByProductIds(List.of("product-1", "product-2"), 2, "cursor-1");
            TestDpp updated = client.updateDppById("dpp-1", patch);
            DeleteDppResponse deleted = client.deleteDppById("dpp-1");
            JsonNode element = client.readDataElement("dpp-1", "$.characteristics.productName");
            JsonNode updatedElement = client.updateDataElement("dpp-1", "$.characteristics.productName", objectMapper().readTree("\"Updated Name\""));

            assertEquals("dpp-1", created.getDppId());
            assertEquals(new TestDpp("dpp-1", "chair"), byId);
            assertEquals("compressed", compressed.path("representation").asText());
            assertEquals("dpp-1", compressed.path("dppId").asText());
            assertEquals(new TestDpp("dpp-1", "chair"), byProductId);
            assertEquals(new TestDpp("dpp-1", "chair"), byDate);
            assertEquals(List.of("dpp-1", "dpp-2"), ids.getDppIdentifiers());
            assertEquals("cursor-2", ids.getNextCursor());
            assertEquals(new TestDpp("dpp-1", "Updated Name"), updated);
            assertEquals(DppStatusCode.SuccessNoContent, deleted.getStatusCode());
            assertEquals("chair", element.textValue());
            assertEquals("Updated Name", updatedElement.textValue());
            assertEquals(10, requestCount.get());
            assertEquals(1, validator.calls());
            assertEquals(1, codec.toJsonCalls());
            assertEquals(4, codec.fromJsonCalls());
        }
    }

    @Test
    void legacyHistoricalReadRemainsAvailableAsDeprecatedCompatibilityPath() throws Exception {
        try (TestServer server = server()) {
            server.httpServer().createContext("/dppsByProductIdAndDate/product-1", exchange -> {
                assertEquals("GET", exchange.getRequestMethod());
                assertEquals("date=2026-05-11T09%3A30%3A15Z", exchange.getRequestURI().getRawQuery());
                respond(exchange, 200, successDppBody("dpp-1", "chair"));
            });
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), new CountingCodec(), new CountingValidator());

            assertEquals(
                    new TestDpp("dpp-1", "chair"),
                    client.readDppVersionByProductIdAndDate("product-1", Instant.parse("2026-05-11T09:30:15Z"))
            );
        }
    }

    @Test
    void createDppValidationFailureDoesNotSendRequest() {
        try (TestServer server = server()) {
            CountingValidator validator = new CountingValidator();
            validator.fail();
            AtomicInteger hits = new AtomicInteger();
            server.httpServer().createContext("/dpps", exchange -> {
                hits.incrementAndGet();
                respond(exchange, 201, "{}");
            });
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), new CountingCodec(), validator);

            assertThrows(DppValidationClientException.class, () -> client.createDpp(new TestDpp("dpp-1", "chair")));
            assertEquals(0, hits.get());
        }
    }

    @Test
    void createDppRequiresCreatedIdentifier() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps", exchange ->
                    respond(exchange, 201, "{\"statusCode\":\"SuccessCreated\",\"payload\":{},\"messages\":[]}"));
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), new CountingCodec(), new CountingValidator());

            DppMappingClientException ex = assertThrows(
                    DppMappingClientException.class,
                    () -> client.createDpp(new TestDpp("dpp-1", "chair"))
            );

            assertEquals("Missing required response field: payload.dppId", ex.getMessage());
        }
    }

    @Test
    void idsProductIdsAndElementPathsAreUrlEncoded() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/", exchange -> {
                assertEquals("/v1/dpps/id%20with%20space%2Fslash", exchange.getRequestURI().getRawPath());
                respond(exchange, 200, successDppBody("id with space/slash", "chair"));
            });
            server.httpServer().createContext("/v1/dppsByProductId/", exchange -> {
                assertEquals("/v1/dppsByProductId/product%2Fwith%20space", exchange.getRequestURI().getRawPath());
                respond(exchange, 200, successDppBody("dpp-1", "chair"));
            });
            server.httpServer().createContext("/v1/dpps/dpp-1/elements/", exchange -> {
                assertEquals(
                        "/v1/dpps/dpp-1/elements/%24%5B%27billOfMaterials%27%5D.materials%5B0%5D.name",
                        exchange.getRequestURI().getRawPath()
                );
                respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":\"steel\",\"messages\":[]}");
            });
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), new CountingCodec(), new CountingValidator());

            assertEquals("id with space/slash", client.readDppById("id with space/slash").id());
            assertEquals("dpp-1", client.readDppByProductId("product/with space").id());
            assertEquals("steel", client.readDataElement("dpp-1", "$['billOfMaterials'].materials[0].name").textValue());
        }
    }

    private static String successDppBody(String id, String name) {
        return "{\"statusCode\":\"Success\",\"payload\":{\"id\":\"" + id + "\",\"name\":\"" + name + "\"},\"messages\":[]}";
    }
}
