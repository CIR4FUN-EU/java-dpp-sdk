package dpp.repo.client;

import dpp.repo.client.exception.DppApiClientException;
import dpp.repo.client.exception.DppHttpClientException;
import dpp.repo.client.exception.DppMappingClientException;
import dpp.repo.client.exception.DppNetworkClientException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static dpp.repo.client.ClientTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpDppRepoClientNegativePathTest {
    @Test
    void constructorRejectsNonHttpAbsoluteBaseUrls() {
        assertThrows(IllegalArgumentException.class, () -> new HttpDppRepoClient("relative/path"));
        assertThrows(IllegalArgumentException.class, () -> new HttpDppRepoClient("ftp://example.test/api"));
    }

    @Test
    void createMapsStructuredNon2xxResponsesToApiException() throws Exception {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps", exchange ->
                    respond(exchange, 502, "{\"statusCode\":\"ServerErrorBadGateway\",\"payload\":null,\"message\":[]}"));
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            DppApiClientException ex = assertThrows(DppApiClientException.class,
                    () -> client.createDpp(objectMapper().readTree("{}")));

            assertEquals(502, ex.httpStatus());
            assertEquals(dpp.repo.payloads.DppStatusCode.ServerErrorBadGateway, ex.statusCode());
            assertTrue(ex.rawResponseBody().contains("ServerErrorBadGateway"));
        }
    }

    @Test
    void structuredHttpErrorsKeepHttpStatusMessagesAndRawBody() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/", exchange -> {
                int status = Integer.parseInt(exchange.getRequestURI().getPath().substring("/v1/dpps/".length()));
                String apiCode = switch (status) {
                    case 400 -> "ClientErrorBadRequest";
                    case 401 -> "ClientNotAuthorized";
                    case 403 -> "ClientForbidden";
                    case 404 -> "ClientErrorResourceNotFound";
                    case 405 -> "ClientMethodNotAllowed";
                    case 409 -> "ClientResourceConflict";
                    case 500 -> "ServerInternalError";
                    case 502 -> "ServerErrorBadGateway";
                    default -> throw new IllegalArgumentException("unexpected status");
                };
                respond(exchange, status, "{\"statusCode\":\"" + apiCode
                        + "\",\"message\":[{\"messageType\":\"Error\",\"text\":\"failure-" + status
                        + "\",\"code\":\"CODE-" + status + "\"}]}");
            });
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            for (int status : new int[]{400, 401, 403, 404, 405, 409, 500, 502}) {
                DppApiClientException ex = assertThrows(DppApiClientException.class,
                        () -> client.readDppById(Integer.toString(status)));
                assertEquals(status, ex.httpStatus());
                assertEquals("failure-" + status, ex.message().get(0).getText());
                assertEquals("CODE-" + status, ex.message().get(0).getCode());
                assertTrue(ex.rawResponseBody().contains("failure-" + status));
            }
        }
    }

    @Test
    void bodylessAndMalformedHttpErrorBodiesRetainRawResponseAndParseCause() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/bodyless", exchange -> {
                exchange.sendResponseHeaders(501, -1);
                exchange.close();
            });
            server.httpServer().createContext("/v1/dpps/malformed", exchange -> respond(exchange, 500, "upstream unavailable"));
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            DppHttpClientException bodyless = assertThrows(DppHttpClientException.class,
                    () -> client.readDppById("bodyless"));
            assertEquals(501, bodyless.statusCode());
            assertEquals("", bodyless.responseBody());
            assertInstanceOf(DppMappingClientException.class, bodyless.getCause());

            DppHttpClientException malformed = assertThrows(DppHttpClientException.class,
                    () -> client.readDppById("malformed"));
            assertEquals(500, malformed.statusCode());
            assertEquals("upstream unavailable", malformed.responseBody());
            assertInstanceOf(DppMappingClientException.class, malformed.getCause());
        }
    }

    @Test
    void wrapperApiErrorsExposeRepositoryStatusAndMessages() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/id", exchange ->
                    respond(exchange, 200, "{\"statusCode\":\"ClientErrorResourceNotFound\",\"payload\":null,\"message\":[{\"messageType\":\"Error\",\"text\":\"missing\"}]}"));
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            DppApiClientException ex = assertThrows(DppApiClientException.class, () -> client.readDppById("id"));
            assertEquals("ClientErrorResourceNotFound", ex.statusCode().name());
            assertEquals("missing", ex.message().get(0).getText());
        }
    }

    @Test
    void deletePreservesStructuredNotFoundAndConflictErrors() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/", exchange -> {
                boolean missing = exchange.getRequestURI().getPath().endsWith("missing");
                int status = missing ? 404 : 409;
                String code = missing ? "ClientErrorResourceNotFound" : "ClientResourceConflict";
                respond(exchange, status, "{\"statusCode\":\"" + code
                        + "\",\"message\":[{\"messageType\":\"Error\",\"text\":\"delete\"}]}");
            });
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            for (String id : new String[]{"missing", "conflict"}) {
                DppApiClientException ex = assertThrows(DppApiClientException.class, () -> client.deleteDppById(id));
                assertEquals(id.equals("missing") ? 404 : 409, ex.httpStatus());
                assertEquals("delete", ex.message().get(0).getText());
            }
        }
    }

    @Test
    void malformedWrapperResponsesAreMappingErrors() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/id", exchange -> respond(exchange, 200, "{"));
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());
            DppMappingClientException ex = assertThrows(DppMappingClientException.class, () -> client.readDppById("id"));
            assertEquals("DPP API response could not be mapped", ex.getMessage());
        }
    }

    @Test
    void malformedPresentMessagesRemainMappingFailuresForApiAndHttpErrors() {
        try (TestServer server = server()) {
            String malformedApiError = "{\"statusCode\":\"ClientErrorBadRequest\",\"message\":[{\"messageType\":\"Error\"}]}";
            String malformedHttpError = "{\"statusCode\":\"ServerErrorBadGateway\",\"message\":[{\"messageType\":\"Error\"}]}";
            server.httpServer().createContext("/v1/dpps/api-error", exchange -> respond(exchange, 200, malformedApiError));
            server.httpServer().createContext("/v1/dpps/http-error", exchange -> respond(exchange, 502, malformedHttpError));
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            DppMappingClientException apiFailure = assertThrows(DppMappingClientException.class,
                    () -> client.readDppById("api-error"));
            assertEquals("DPP API response could not be mapped", apiFailure.getMessage());

            DppHttpClientException httpFailure = assertThrows(DppHttpClientException.class,
                    () -> client.readDppById("http-error"));
            assertEquals(502, httpFailure.statusCode());
            assertEquals(malformedHttpError, httpFailure.responseBody());
            assertInstanceOf(DppMappingClientException.class, httpFailure.getCause());
        }
    }

    @Test
    void successfulWrapperMustContainOperationSpecificFields() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dppsByProductIds", exchange ->
                    respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":{},\"message\":[]}"));
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());
            DppMappingClientException ex = assertThrows(DppMappingClientException.class,
                    () -> client.readDppIdsByProductIds(java.util.List.of("p"), null, null));
            assertEquals("Missing required response field: payload.dppIdentifiers", ex.getMessage());
        }
    }

    @Test
    void successfulReadMayOmitOptionalPayloadButFineGranularReadRequiresIt() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/id", exchange ->
                    respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":null,\"message\":[]}"));
            server.httpServer().createContext("/v1/dpps/id/elements/$.name", exchange ->
                    respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":null,\"message\":[]}"));
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            assertTrue(client.readDppById("id").getPayload().isNull());
            DppMappingClientException ex = assertThrows(DppMappingClientException.class,
                    () -> client.readDataElement("id", "$.name"));
            assertEquals("Missing required response field: payload", ex.getMessage());
        }
    }

    @Test
    void invalidRequestArgumentsAreRejectedBeforeHttp() {
        DppRepoClient client = new HttpDppRepoClient("http://localhost:1");
        assertEquals("elementIdPath must not be blank", assertThrows(IllegalArgumentException.class,
                () -> client.readDataElement("id", " ")).getMessage());
        assertThrows(NullPointerException.class, () -> client.readDppVersionByIdAndDate("id", null));
    }

    @Test
    void repoRequestsMapConnectionFailuresToNetworkException() {
        DppRepoClient client = new HttpDppRepoClient("http://localhost:1");
        DppNetworkClientException ex = assertThrows(DppNetworkClientException.class, () -> client.readDppById("id"));
        assertEquals("DPP HTTP request could not complete", ex.getMessage());
    }

    @Test
    void createSendsJsonWithoutSemanticValidationAndNullIsRejectedBeforeRequest() throws Exception {
        try (TestServer server = server()) {
            AtomicInteger hits = new AtomicInteger();
            server.httpServer().createContext("/v1/dpps", exchange -> {
                hits.incrementAndGet();
                assertEquals("{\"arbitrary\":true}", requestBody(exchange));
                respond(exchange, 201, "{\"statusCode\":\"SuccessCreated\",\"payload\":{},\"message\":[]}");
            });
            DppRepoClient client = new HttpDppRepoClient(server.baseUrl());

            assertNull(client.createDpp(objectMapper().readTree("{\"arbitrary\":true}")).getDigitalProductPassportId());
            assertThrows(NullPointerException.class, () -> client.createDpp(null));
            assertEquals(1, hits.get());
        }
    }
}
