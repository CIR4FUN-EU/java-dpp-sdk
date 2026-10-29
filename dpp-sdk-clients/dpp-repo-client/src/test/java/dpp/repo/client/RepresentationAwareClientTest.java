package dpp.repo.client;

import com.fasterxml.jackson.databind.JsonNode;
import dpp.repo.client.exception.DppHttpClientException;
import dpp.repo.payloads.DppApiResponse;
import dpp.repo.payloads.DppRepresentation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static dpp.repo.client.ClientTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class RepresentationAwareClientTest {
    @Test
    void createPostsPreparedJsonWithDefaultCompressedAndExplicitRepresentations() throws Exception {
        try (var server = server()) {
            AtomicReference<String> query = new AtomicReference<>();
            AtomicReference<String> body = new AtomicReference<>();
            server.httpServer().createContext("/v1/dpps", exchange -> {
                query.set(exchange.getRequestURI().getRawQuery());
                body.set(requestBody(exchange));
                respond(exchange, 201, "{\"statusCode\":\"SuccessCreated\",\"payload\":{\"digitalProductPassportId\":\"id\"}}");
            });
            JsonNode prepared = objectMapper().readTree("{\"digitalProductPassportId\":\"id\",\"unvalidated\":true}");
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            client.createDpp(prepared);
            assertNull(query.get());
            assertEquals(prepared, objectMapper().readTree(body.get()));

            client.createDpp(prepared, DppRepresentation.COMPRESSED);
            assertNull(query.get());
            assertEquals(prepared, objectMapper().readTree(body.get()));

            client.createDpp(prepared, DppRepresentation.FULL);
            assertEquals("representation=full", query.get());
            assertEquals(prepared, objectMapper().readTree(body.get()));
        }
    }

    @Test
    void clientRejectsNullJsonBeforeSendingRequest() {
        try (var server = server()) {
            var client = new HttpDppRepoClient(server.baseUrl());
            assertThrows(NullPointerException.class, () -> client.createDpp(null));
            assertThrows(NullPointerException.class, () -> client.updateDppById("id", null));
            assertThrows(NullPointerException.class, () -> client.updateDataElement("id", "$.name", null));
        }
    }

    @Test
    void readsReturnRawJsonPayloadAndPreserveDefaultAndFullRepresentation() throws Exception {
        try (var server = server()) {
            AtomicReference<String> query = new AtomicReference<>();
            server.httpServer().createContext("/v1/dpps/id", exchange -> {
                query.set(exchange.getRequestURI().getRawQuery());
                respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":{\"elements\":[{\"elementId\":\"name\"}]},\"message\":[]}");
            });
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            DppApiResponse<JsonNode> compressed = client.readDppById("id");
            assertNull(query.get());
            assertEquals("name", compressed.getPayload().path("elements").get(0).path("elementId").asText());

            DppApiResponse<JsonNode> full = client.readDppById("id", DppRepresentation.FULL);
            assertEquals("representation=full", query.get());
            assertEquals(compressed.getPayload(), full.getPayload());
        }
    }

    @Test
    void elementCallsOmitRepresentationByDefaultAndAllowExplicitFull() throws Exception {
        try (var server = server()) {
            server.httpServer().createContext("/v1/dpps/id/elements/$.name", exchange -> {
                String query = exchange.getRequestURI().getRawQuery();
                String body = exchange.getRequestMethod().equals("PATCH") ? requestBody(exchange) : null;
                if (query == null) {
                    if (body != null) assertEquals("\"new\"", body);
                    respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":\"new\"}");
                } else {
                    assertEquals("representation=full", query);
                    if (body != null) assertEquals("{\"elementId\":\"name\",\"objectType\":\"SingleValuedDataElement\",\"value\":\"new\"}", body);
                    respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":{\"elementId\":\"name\",\"objectType\":\"SingleValuedDataElement\",\"value\":\"new\"}}");
                }
            });
            var client = new HttpDppRepoClient(server.baseUrl());
            JsonNode operational = objectMapper().readTree("\"new\"");
            JsonNode expanded = objectMapper().readTree(
                    "{\"elementId\":\"name\",\"objectType\":\"SingleValuedDataElement\",\"value\":\"new\"}");

            assertEquals(operational, client.readDataElement("id", "$.name").getPayload());
            assertEquals(operational, client.updateDataElement("id", "$.name", operational).getPayload());
            assertEquals(expanded, client.readDataElement("id", "$.name", DppRepresentation.FULL).getPayload());
            assertEquals(expanded, client.updateDataElement("id", "$.name", expanded, DppRepresentation.FULL).getPayload());
        }
    }

    @Test
    void bodylessNotImplementedResponseIsAnHttpErrorWithoutResultParsing() throws Exception {
        try (var server = server()) {
            server.httpServer().createContext("/v1/dpps/id", exchange -> respond(exchange, 501, ""));
            var client = new HttpDppRepoClient(server.baseUrl());
            var error = assertThrows(DppHttpClientException.class,
                    () -> client.readDppById("id", DppRepresentation.COMPRESSED));
            assertEquals(501, error.statusCode());
        }
    }
}
