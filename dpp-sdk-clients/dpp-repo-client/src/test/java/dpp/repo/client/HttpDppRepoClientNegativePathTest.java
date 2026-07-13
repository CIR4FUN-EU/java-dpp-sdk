package dpp.repo.client;

import com.fasterxml.jackson.databind.JsonNode;
import dpp.repo.client.ClientTestSupport.CountingCodec;
import dpp.repo.client.ClientTestSupport.CountingValidator;
import dpp.repo.client.ClientTestSupport.TestDpp;
import dpp.repo.client.ClientTestSupport.TestServer;
import dpp.repo.client.exception.DppApiClientException;
import dpp.repo.client.exception.DppHttpClientException;
import dpp.repo.client.exception.DppMappingClientException;
import dpp.repo.client.exception.DppNetworkClientException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static dpp.repo.client.ClientTestSupport.objectMapper;
import static dpp.repo.client.ClientTestSupport.requestBody;
import static dpp.repo.client.ClientTestSupport.respond;
import static dpp.repo.client.ClientTestSupport.server;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpDppRepoClientNegativePathTest {
    @Test
    void createDppMapsNon2xxHttpResponsesToHttpException() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps", exchange ->
                    respond(exchange, 502, "{\"statusCode\":\"ServerErrorBadGateway\",\"payload\":null,\"messages\":[]}"));
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), new CountingCodec(), new CountingValidator());

            DppHttpClientException ex = assertThrows(
                    DppHttpClientException.class,
                    () -> client.createDpp(new TestDpp("dpp-1", "chair"))
            );

            assertEquals(502, ex.statusCode());
            assertTrue(ex.responseBody().contains("ServerErrorBadGateway"));
        }
    }

    @Test
    void wrapperApiErrorsExposeRepositoryStatusAndMessages() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/dpp-1", exchange ->
                    respond(exchange, 200, "{\"statusCode\":\"ClientErrorResourceNotFound\",\"payload\":null,\"messages\":[{\"messageType\":\"Error\",\"text\":\"missing\"}]}"));
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), new CountingCodec(), new CountingValidator());

            DppApiClientException ex = assertThrows(
                    DppApiClientException.class,
                    () -> client.readDppById("dpp-1")
            );

            assertEquals("ClientErrorResourceNotFound", ex.statusCode().name());
            assertEquals("missing", ex.messages().get(0).getText());
        }
    }

    @Test
    void createDppMapsCodecSerializationFailuresBeforeSendingRequest() {
        try (TestServer server = server()) {
            AtomicInteger hits = new AtomicInteger();
            server.httpServer().createContext("/v1/dpps", exchange -> {
                hits.incrementAndGet();
                respond(exchange, 201, "{\"statusCode\":\"SuccessCreated\",\"payload\":{\"dppId\":\"dpp-1\"},\"messages\":[]}");
            });
            CountingCodec codec = new CountingCodec();
            codec.failToJson();
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), codec, new CountingValidator());

            DppMappingClientException ex = assertThrows(
                    DppMappingClientException.class,
                    () -> client.createDpp(new TestDpp("dpp-1", "chair"))
            );

            assertEquals("DPP serialization failed before request", ex.getMessage());
            assertEquals(0, hits.get());
        }
    }

    @Test
    void createDppRejectsNullJsonReturnedByCodecBeforeSendingRequest() {
        try (TestServer server = server()) {
            AtomicInteger hits = new AtomicInteger();
            server.httpServer().createContext("/v1/dpps", exchange -> {
                hits.incrementAndGet();
                respond(exchange, 201, "{\"statusCode\":\"SuccessCreated\",\"payload\":{\"dppId\":\"dpp-1\"},\"messages\":[]}");
            });
            CountingCodec codec = new CountingCodec();
            codec.returnNullJson();
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), codec, new CountingValidator());

            DppMappingClientException ex = assertThrows(
                    DppMappingClientException.class,
                    () -> client.createDpp(new TestDpp("dpp-1", "chair"))
            );

            assertEquals("DPP serialization failed before request", ex.getMessage());
            assertEquals(0, hits.get());
        }
    }

    @Test
    void readDppByIdMapsCodecDeserializationFailures() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/dpp-1", exchange ->
                    respond(exchange, 200, successDppBody("dpp-1", "chair")));
            CountingCodec codec = new CountingCodec();
            codec.failFromJson();
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), codec, new CountingValidator());

            DppMappingClientException ex = assertThrows(
                    DppMappingClientException.class,
                    () -> client.readDppById("dpp-1")
            );

            assertEquals("DPP deserialization failed after response", ex.getMessage());
        }
    }

    @Test
    void readDppByIdRejectsNullDppReturnedByCodec() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/dpp-1", exchange ->
                    respond(exchange, 200, successDppBody("dpp-1", "chair")));
            CountingCodec codec = new CountingCodec();
            codec.returnNullDpp();
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), codec, new CountingValidator());

            DppMappingClientException ex = assertThrows(
                    DppMappingClientException.class,
                    () -> client.readDppById("dpp-1")
            );

            assertEquals("DPP deserialization failed after response", ex.getMessage());
        }
    }

    @Test
    void readDppByIdRejectsMalformedWrapperResponses() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/dpp-1", exchange -> respond(exchange, 200, "{"));
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), new CountingCodec(), new CountingValidator());

            DppMappingClientException ex = assertThrows(
                    DppMappingClientException.class,
                    () -> client.readDppById("dpp-1")
            );

            assertEquals("DPP API response could not be mapped", ex.getMessage());
        }
    }

    @Test
    void readDppByIdRequiresPayloadWhenWrapperSucceeds() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/dpp-1", exchange ->
                    respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":null,\"messages\":[]}"));
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), new CountingCodec(), new CountingValidator());

            DppMappingClientException ex = assertThrows(
                    DppMappingClientException.class,
                    () -> client.readDppById("dpp-1")
            );

            assertEquals("Missing required response field: payload", ex.getMessage());
        }
    }

    @Test
    void readDataElementRequiresPayloadWhenWrapperSucceeds() {
        try (TestServer server = server()) {
            server.httpServer().createContext("/v1/dpps/dpp-1/elements/$.characteristics.productName", exchange ->
                    respond(exchange, 200, "{\"statusCode\":\"Success\",\"payload\":null,\"messages\":[]}"));
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), new CountingCodec(), new CountingValidator());

            DppMappingClientException ex = assertThrows(
                    DppMappingClientException.class,
                    () -> client.readDataElement("dpp-1", "$.characteristics.productName")
            );

            assertEquals("Missing required response field: payload", ex.getMessage());
        }
    }

    @Test
    void fineGranularElementIdPathMustNotBeBlank() {
        DppRepoClient<TestDpp> client = new HttpDppRepoClient<>("http://localhost:1", new CountingCodec(), new CountingValidator());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> client.readDataElement("dpp-1", " "));

        assertEquals("elementIdPath must not be blank", exception.getMessage());
    }

    @Test
    void repoRequestsMapConnectionFailuresToNetworkException() {
        DppRepoClient<TestDpp> client = new HttpDppRepoClient<>("http://localhost:1", new CountingCodec(), new CountingValidator());

        DppNetworkClientException ex = assertThrows(
                DppNetworkClientException.class,
                () -> client.readDppById("dpp-1")
        );

        assertEquals("DPP HTTP request could not complete", ex.getMessage());
    }

    @Test
    void partialUpdateDoesNotTriggerFullDppValidation() throws Exception {
        try (TestServer server = server()) {
            CountingValidator validator = new CountingValidator();
            validator.fail();
            AtomicInteger hits = new AtomicInteger();
            JsonNode patch = objectMapper().readTree("{\"name\":\"Updated Name\"}");
            server.httpServer().createContext("/v1/dpps/dpp-1", exchange -> {
                hits.incrementAndGet();
                assertEquals("PATCH", exchange.getRequestMethod());
                assertEquals("{\"name\":\"Updated Name\"}", requestBody(exchange));
                respond(exchange, 200, successDppBody("dpp-1", "Updated Name"));
            });
            DppRepoClient<TestDpp> client = new HttpDppRepoClient<>(server.baseUrl(), new CountingCodec(), validator);

            TestDpp updated = client.updateDppById("dpp-1", patch);

            assertEquals(new TestDpp("dpp-1", "Updated Name"), updated);
            assertEquals(0, validator.calls());
            assertEquals(1, hits.get());
        }
    }

    private static String successDppBody(String id, String name) {
        return "{\"statusCode\":\"Success\",\"payload\":{\"id\":\"" + id + "\",\"name\":\"" + name + "\"},\"messages\":[]}";
    }
}
