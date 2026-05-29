package dpp.repo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dpp.repo.client.core.DppCodec;
import dpp.repo.client.core.DppValidator;
import dpp.repo.client.internal.HttpSupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

final class ClientTestSupport {
    private static final ObjectMapper OBJECT_MAPPER = HttpSupport.buildObjectMapper();

    private ClientTestSupport() {
    }

    static TestServer server() {
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

    static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    static String requestBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }

    record TestDpp(String id, String name) {
    }

    static final class CountingValidator implements DppValidator<TestDpp> {
        private final AtomicInteger calls = new AtomicInteger();
        private boolean fail;

        @Override
        public void validate(TestDpp dpp) {
            calls.incrementAndGet();
            if (fail) {
                throw new IllegalArgumentException("invalid dpp");
            }
        }

        int calls() {
            return calls.get();
        }

        void fail() {
            this.fail = true;
        }
    }

    static final class CountingCodec implements DppCodec<TestDpp> {
        private final AtomicInteger toJsonCalls = new AtomicInteger();
        private final AtomicInteger fromJsonCalls = new AtomicInteger();
        private boolean failToJson;
        private boolean failFromJson;
        private boolean returnNullJson;
        private boolean returnNullDpp;

        @Override
        public String toJson(TestDpp dpp) {
            toJsonCalls.incrementAndGet();
            if (failToJson) {
                throw new IllegalStateException("cannot encode");
            }
            if (returnNullJson) {
                return null;
            }
            return "{\"id\":\"" + dpp.id() + "\",\"name\":\"" + dpp.name() + "\"}";
        }

        @Override
        public TestDpp fromJson(String json) {
            fromJsonCalls.incrementAndGet();
            if (failFromJson) {
                throw new IllegalStateException("cannot decode");
            }
            if (returnNullDpp) {
                return null;
            }
            return new TestDpp(extract(json, "id"), extract(json, "name"));
        }

        int toJsonCalls() {
            return toJsonCalls.get();
        }

        int fromJsonCalls() {
            return fromJsonCalls.get();
        }

        void failToJson() {
            this.failToJson = true;
        }

        void failFromJson() {
            this.failFromJson = true;
        }

        void returnNullJson() {
            this.returnNullJson = true;
        }

        void returnNullDpp() {
            this.returnNullDpp = true;
        }

        private static String extract(String json, String field) {
            String key = "\"" + field + "\":\"";
            int start = json.indexOf(key) + key.length();
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        }
    }

    static final class TestServer implements AutoCloseable {
        private final HttpServer server;
        private final ExecutorService executor;

        private TestServer(HttpServer server, ExecutorService executor) {
            this.server = server;
            this.executor = executor;
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
