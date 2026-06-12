package dpp.registry.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dpp.registry.client.exception.DppApiClientException;
import dpp.registry.client.exception.DppMappingClientException;
import dpp.registry.payloads.DppStatusCode;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.registry.payloads.RegisterDppResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpDppRegistryClientTest {
    @Test
    void postNewDppToRegistryUsesDraftAlignedPathAndSystemIntegrationFields() throws IOException {
        try (TestServer server = TestServer.start()) {
            AtomicInteger hits = new AtomicInteger();
            server.httpServer().createContext("/registerDPP", exchange -> {
                hits.incrementAndGet();
                assertEquals("POST", exchange.getRequestMethod());
                assertEquals(
                        "{\"productIdentifier\":\"product-1\",\"dppIdentifier\":\"dpp-1\",\"operatorIdentifier\":\"operator-1\",\"repoUrl\":\"http://localhost:8082\"}",
                        requestBody(exchange)
                );
                respond(exchange, 201, "{\"statusCode\":\"SuccessCreated\",\"payload\":{\"registryIdentifier\":\"registry-1\"},\"messages\":[]}");
            });

            DppRegistryClient client = new HttpDppRegistryClient(server.baseUrl());

            RegisterDppResponse response = client.postNewDppToRegistry(
                    new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082")
            );

            assertEquals("registry-1", response.getRegistryIdentifier());
            assertEquals(1, hits.get());
        }
    }

    @Test
    void registryRequestSerializationDoesNotIncludeRemovedBackupFields() {
        try (TestServer server = TestServer.start()) {
            server.httpServer().createContext("/registerDPP", exchange -> {
                String body = requestBody(exchange);
                assertFalse(body.contains("backupOperatorIdentifier"));
                assertFalse(body.contains("backupProviderIdentifier"));
                assertFalse(body.contains("backupProvider"));
                assertFalse(body.contains("backupId"));
                respond(exchange, 201, "{\"statusCode\":\"SuccessCreated\",\"payload\":{\"registryIdentifier\":\"registry-1\"},\"messages\":[]}");
            });
            DppRegistryClient client = new HttpDppRegistryClient(server.baseUrl());

            RegisterDppResponse response = client.postNewDppToRegistry(
                    new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082")
            );

            assertEquals("registry-1", response.getRegistryIdentifier());
        }
    }

    @Test
    void postNewDppToRegistryRequiresRegistryIdentifier() {
        try (TestServer server = TestServer.start()) {
            server.httpServer().createContext("/registerDPP", exchange ->
                    respond(exchange, 201, "{\"statusCode\":\"SuccessCreated\",\"payload\":{},\"messages\":[]}"));
            DppRegistryClient client = new HttpDppRegistryClient(server.baseUrl());

            DppMappingClientException ex = assertThrows(
                    DppMappingClientException.class,
                    () -> client.postNewDppToRegistry(new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082"))
            );

            assertEquals("Missing required response field: payload.registryIdentifier", ex.getMessage());
        }
    }

    @Test
    void wrapperApiErrorsExposeRegistryStatus() {
        try (TestServer server = TestServer.start()) {
            server.httpServer().createContext("/registerDPP", exchange ->
                    respond(exchange, 200, "{\"statusCode\":\"ClientResourceConflict\",\"payload\":null,\"messages\":[{\"messageType\":\"Error\",\"text\":\"duplicate\"}]}"));
            DppRegistryClient client = new HttpDppRegistryClient(server.baseUrl());

            DppApiClientException ex = assertThrows(
                    DppApiClientException.class,
                    () -> client.postNewDppToRegistry(new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082"))
            );

            assertEquals(DppStatusCode.ClientResourceConflict, ex.statusCode());
            assertEquals("duplicate", ex.messages().get(0).getText());
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String requestBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
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
