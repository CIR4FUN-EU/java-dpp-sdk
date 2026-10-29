package dpp.repo.client;

import com.fasterxml.jackson.databind.JsonNode;
import dpp.repo.payloads.DppRepresentation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static dpp.repo.client.ClientTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpDppRepoClientTest {
    @Test
    void repositoryLifecycleUsesJsonPayloadsAndEn18222Paths() throws Exception {
        try (TestServer server = server()) {
            AtomicInteger requestCount = new AtomicInteger();
            JsonNode payload = objectMapper().readTree("{\"digitalProductPassportId\":\"dpp-1\",\"name\":\"chair\"}");
            JsonNode patch = objectMapper().readTree("{\"name\":\"updated\"}");
            server.httpServer().createContext("/v1/dpps", exchange -> {
                requestCount.incrementAndGet();
                assertEquals("POST", exchange.getRequestMethod());
                assertNull(exchange.getRequestURI().getRawQuery());
                assertEquals(payload, objectMapper().readTree(requestBody(exchange)));
                respond(exchange, 201, "{\"statusCode\":\"SuccessCreated\",\"payload\":{\"digitalProductPassportId\":\"dpp-1\"},\"message\":[]}");
            });
            server.httpServer().createContext("/v1/dpps/dpp-1", exchange -> {
                requestCount.incrementAndGet();
                switch (exchange.getRequestMethod()) {
                    case "GET" -> respond(exchange, 200, successDppBody("dpp-1", "chair"));
                    case "PATCH" -> {
                        assertEquals(patch, objectMapper().readTree(requestBody(exchange)));
                        respond(exchange, 200, successDppBody("dpp-1", "updated"));
                    }
                    case "DELETE" -> respond(exchange, 204, "");
                    default -> fail("unexpected method " + exchange.getRequestMethod());
                }
            });
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            assertEquals("dpp-1", client.createDpp(payload).getDigitalProductPassportId());
            assertEquals("chair", client.readDppById("dpp-1").getPayload().path("name").asText());
            assertEquals("updated", client.updateDppById("dpp-1", patch).getPayload().path("name").asText());
            assertEquals(dpp.repo.payloads.DppStatusCode.SuccessNoContent, client.deleteDppById("dpp-1").getStatusCode());
            assertEquals(4, requestCount.get());
        }
    }

    @Test
    void productHistoryAndProductIdBatchRequestsAreEncodedAndParsed() throws Exception {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dppsByProductId/", exchange -> {
                assertEquals("/v1/dppsByProductId/product%2Fwith%20space", exchange.getRequestURI().getRawPath());
                respond(exchange, 200, successDppBody("dpp-1", "chair"));
            });
            server.httpServer().createContext("/v1/dppsByIdAndDate/", exchange -> {
                assertEquals("/v1/dppsByIdAndDate/dpp%2F1", exchange.getRequestURI().getRawPath());
                String query = exchange.getRequestURI().getRawQuery();
                assertTrue(query.equals("date=2026-05-11T09%3A30%3A15Z")
                        || query.equals("date=2026-05-11T09%3A30%3A15Z&representation=full"));
                respond(exchange, 200, successDppBody("dpp-1", "chair"));
            });
            server.httpServer().createContext("/v1/dppsByProductIds", exchange -> {
                assertEquals("POST", exchange.getRequestMethod());
                assertEquals("{\"productIdentifiers\":[\"p-1\",\"p-2\"],\"limit\":5,\"cursor\":\"next\"}", requestBody(exchange));
                respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":{\"dppIdentifiers\":[]},\"message\":[]}");
            });
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            assertEquals("dpp-1", client.readDppByProductId("product/with space").getPayload().path("id").asText());
            assertEquals("dpp-1", client.readDppVersionByIdAndDate("dpp/1", Instant.parse("2026-05-11T09:30:15Z"))
                    .getPayload().path("id").asText());
            assertEquals("dpp-1", client.readDppVersionByIdAndDate("dpp/1", Instant.parse("2026-05-11T09:30:15Z"),
                    DppRepresentation.FULL).getPayload().path("id").asText());
            assertNotNull(client.readDppIdsByProductIds(List.of("p-1", "p-2"), 5, "next"));
        }
    }

    @Test
    void createAcceptsAbsentOptionalCreatedIdentifierAndReadReturnsMessageEnvelope() throws Exception {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps", exchange ->
                    respond(exchange, 201, "{\"statusCode\":\"SuccessCreated\",\"payload\":{},\"message\":[]}"));
            server.httpServer().createContext("/v1/dpps/id", exchange ->
                    respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":{\"id\":\"id\"},\"message\":[{\"messageType\":\"Info\",\"text\":\"read\",\"code\":\"READ\"}]}"));
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            assertNull(client.createDpp(objectMapper().readTree("{}" )).getDigitalProductPassportId());
            var response = client.readDppById("id");
            assertEquals("id", response.getPayload().path("id").asText());
            assertEquals("read", response.getMessage().get(0).getText());
        }
    }

    @Test
    void idsAndElementPathsAreUrlEncodedAndElementUpdateUsesNeutralJson() throws Exception {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/", exchange -> {
                assertEquals("/v1/dpps/id%20with%20space%2Fslash", exchange.getRequestURI().getRawPath());
                respond(exchange, 200, successDppBody("id with space/slash", "chair"));
            });
            server.httpServer().createContext("/v1/dpps/dpp-1/elements/", exchange -> {
                assertEquals("/v1/dpps/dpp-1/elements/%24%5B%27billOfMaterials%27%5D.materials%5B0%5D.name",
                        exchange.getRequestURI().getRawPath());
                assertEquals("PATCH", exchange.getRequestMethod());
                assertEquals("\"steel\"", requestBody(exchange));
                respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":\"steel\"}");
            });
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            assertEquals("id with space/slash", client.readDppById("id with space/slash").getPayload().path("id").asText());
            assertEquals("steel", client.updateDataElement("dpp-1", "$['billOfMaterials'].materials[0].name",
                    objectMapper().readTree("\"steel\"")).getPayload().textValue());
        }
    }

    private static String successDppBody(String id, String name) {
        return "{\"statusCode\":\"Success\",\"payload\":{\"id\":\"" + id + "\",\"name\":\"" + name + "\"},\"message\":[]}";
    }
}
