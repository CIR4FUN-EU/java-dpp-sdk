package dpp.registry.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dpp.registry.client.exception.DppHttpClientException;
import dpp.registry.client.exception.DppMappingClientException;
import dpp.registry.client.exception.DppNetworkClientException;
import dpp.registry.payloads.RegisterDppRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpDppRegistryClientNegativePathTest {
    @Test
    void non2xxRegistryResponsesMapToHttpException() {
        try (TestServer server = TestServer.start()) {
            server.httpServer().createContext("/registerDPP", exchange ->
                    respond(exchange, 503, "{\"statusCode\":\"ServerErrorBadGateway\",\"payload\":null,\"messages\":[]}"));
            DppRegistryClient client = new HttpDppRegistryClient(server.baseUrl());

            DppHttpClientException ex = assertThrows(
                    DppHttpClientException.class,
                    () -> client.postNewDppToRegistry(new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082"))
            );

            assertEquals(503, ex.statusCode());
            assertTrue(ex.responseBody().contains("ServerErrorBadGateway"));
        }
    }

    @Test
    void malformedRegistryWrapperResponsesMapToMappingException() {
        try (TestServer server = TestServer.start()) {
            server.httpServer().createContext("/registerDPP", exchange -> respond(exchange, 200, "{"));
            DppRegistryClient client = new HttpDppRegistryClient(server.baseUrl());

            DppMappingClientException ex = assertThrows(
                    DppMappingClientException.class,
                    () -> client.postNewDppToRegistry(new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082"))
            );

            assertEquals("DPP API response could not be mapped", ex.getMessage());
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
