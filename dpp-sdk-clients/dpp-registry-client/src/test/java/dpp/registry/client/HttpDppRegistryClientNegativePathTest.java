package dpp.registry.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dpp.registry.client.exception.DppApiClientException;
import dpp.registry.client.exception.DppHttpClientException;
import dpp.registry.client.exception.DppMappingClientException;
import dpp.registry.client.exception.DppNetworkClientException;
import dpp.registry.payloads.RegisterDppRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpDppRegistryClientNegativePathTest {
    @Test
    void constructorRejectsNonHttpAbsoluteBaseUrls() {
        assertThrows(IllegalArgumentException.class, () -> new HttpDppRegistryClient("relative/path"));
        assertThrows(IllegalArgumentException.class, () -> new HttpDppRegistryClient("ftp://example.test/api"));
    }

    @Test
    void structuredNon2xxRegistryResponsesMapToApiException() {
        try (TestServer server = TestServer.start()) {
            server.httpServer().createContext("/v1/registerDPP", exchange ->
                    respond(exchange, 502, "{\"statusCode\":\"ServerErrorBadGateway\",\"payload\":null,\"messages\":[]}"));
            DppRegistryClient client = new HttpDppRegistryClient(server.baseUrl());

            DppApiClientException ex = assertThrows(
                    DppApiClientException.class,
                    () -> client.postNewDppToRegistry(new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082"))
            );

            assertEquals(502, ex.httpStatus());
            assertEquals(dpp.registry.payloads.DppStatusCode.ServerErrorBadGateway, ex.statusCode());
            assertTrue(ex.rawResponseBody().contains("ServerErrorBadGateway"));
        }
    }

    @Test
    void malformedRegistryWrapperResponsesMapToMappingException() {
        try (TestServer server = TestServer.start()) {
            server.httpServer().createContext("/v1/registerDPP", exchange -> respond(exchange, 200, "{"));
            DppRegistryClient client = new HttpDppRegistryClient(server.baseUrl());

            DppMappingClientException ex = assertThrows(
                    DppMappingClientException.class,
                    () -> client.postNewDppToRegistry(new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082"))
            );

            assertEquals("DPP API response could not be mapped", ex.getMessage());
        }
    }

    @Test
    void malformedPresentMessagesRemainMappingFailuresForApiAndHttpErrors() {
        try (TestServer server = TestServer.start()) {
            String malformedApiError = "{\"statusCode\":\"ClientErrorBadRequest\",\"message\":[{\"messageType\":\"Error\"}]}";
            String malformedHttpError = "{\"statusCode\":\"ServerErrorBadGateway\",\"message\":[{\"messageType\":\"Error\"}]}";
            AtomicInteger requests = new AtomicInteger();
            server.httpServer().createContext("/v1/registerDPP", exchange -> {
                boolean apiError = requests.getAndIncrement() == 0;
                respond(exchange, apiError ? 200 : 502, apiError ? malformedApiError : malformedHttpError);
            });
            DppRegistryClient client = new HttpDppRegistryClient(server.baseUrl());

            DppMappingClientException apiFailure = assertThrows(DppMappingClientException.class,
                    () -> client.postNewDppToRegistry(request()));
            assertEquals("DPP API response could not be mapped", apiFailure.getMessage());

            DppHttpClientException httpFailure = assertThrows(DppHttpClientException.class,
                    () -> client.postNewDppToRegistry(request()));
            assertEquals(502, httpFailure.statusCode());
            assertEquals(malformedHttpError, httpFailure.responseBody());
            assertTrue(httpFailure.getCause() instanceof DppMappingClientException);
        }
    }

    @Test
    void malformedNonJsonHttpErrorsKeepRawBodyAndParseCause() {
        try (TestServer server = TestServer.start()) {
            server.httpServer().createContext("/v1/registerDPP", exchange ->
                    respond(exchange, 502, "bad gateway"));
            DppRegistryClient client = new HttpDppRegistryClient(server.baseUrl());

            DppHttpClientException ex = assertThrows(DppHttpClientException.class,
                    () -> client.postNewDppToRegistry(
                            new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082")));

            assertEquals(502, ex.statusCode());
            assertEquals("bad gateway", ex.responseBody());
            assertTrue(ex.getCause() instanceof DppMappingClientException);
        }
    }

    @Test
    void bodyless501IsHandledWithoutAResultObject() {
        try (TestServer server = TestServer.start()) {
            server.httpServer().createContext("/v1/registerDPP", exchange -> {
                exchange.sendResponseHeaders(501, -1);
                exchange.close();
            });
            DppRegistryClient client = new HttpDppRegistryClient(server.baseUrl());

            DppHttpClientException ex = assertThrows(DppHttpClientException.class,
                    () -> client.postNewDppToRegistry(
                            new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082")));

            assertEquals(501, ex.statusCode());
            assertEquals("", ex.responseBody());
            assertTrue(ex.getCause() instanceof DppMappingClientException);
        }
    }

    @Test
    void registryRequestsMapConnectionFailuresToNetworkException() {
        DppRegistryClient client = new HttpDppRegistryClient("http://localhost:1");

        DppNetworkClientException ex = assertThrows(
                DppNetworkClientException.class,
                () -> client.postNewDppToRegistry(new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082"))
        );

        assertEquals("DPP HTTP request could not complete", ex.getMessage());
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static RegisterDppRequest request() {
        return new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082");
    }

    private static final class TestServer implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;

        private TestServer(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
        }

        static TestServer start() {
            try {
                HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                ExecutorService executor = Executors.newCachedThreadPool();
                httpServer.setExecutor(executor);
                httpServer.start();
                return new TestServer(httpServer, executor);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        HttpServer httpServer() {
            return server;
        }

        String baseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
