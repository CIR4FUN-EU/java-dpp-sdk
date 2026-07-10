package demo.producer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DemoServicePreflightTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    @DisplayName("Health check passes when the expected mock service is reachable")
    void healthCheckPassesWhenServiceIsReachable() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", this::writeOkHealthResponse);
        server.start();

        DemoServicePreflight preflight = new DemoServicePreflight();

        assertDoesNotThrow(() -> preflight.verifyReachable("Registry", baseUrl(), "dpp-registry-api"));
    }

    @Test
    @DisplayName("Resolves the preferred Docker-style URL before considering the localhost fallback")
    void resolveReachablePrefersPrimaryUrl() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", this::writeOkHealthResponse);
        server.start();

        DemoServicePreflight preflight = new DemoServicePreflight();

        String resolved = preflight.resolveReachable(
                "Registry",
                baseUrl(),
                "http://127.0.0.1:65529",
                "dpp-registry-api"
        );

        assertEquals(baseUrl(), resolved);
    }

    @Test
    @DisplayName("Falls back to the localhost URL when the preferred Docker-style URL is unreachable")
    void resolveReachableFallsBackToSecondaryUrl() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", this::writeOkHealthResponse);
        server.start();

        DemoServicePreflight preflight = new DemoServicePreflight();

        String resolved = preflight.resolveReachable(
                "Registry",
                "http://127.0.0.1:65529",
                baseUrl(),
                "dpp-registry-api"
        );

        assertEquals(baseUrl(), resolved);
    }

    @Test
    @DisplayName("Health check fails with an actionable message when the mock service is unreachable")
    void healthCheckFailsWithActionableMessageWhenServiceIsUnreachable() {
        DemoServicePreflight preflight = new DemoServicePreflight();
        String unreachableUrl = "http://127.0.0.1:65529";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> preflight.verifyReachable("Registry", unreachableUrl, "dpp-registry-api")
        );

        assertTrue(exception.getMessage().contains("Registry service is not reachable"));
        assertTrue(exception.getMessage().contains(unreachableUrl));
        assertTrue(exception.getMessage().contains("dpp-registry-api"));
    }

    @Test
    @DisplayName("Resolve reachable fails only after both the Docker-style and localhost URLs fail")
    void resolveReachableFailsAfterBothCandidatesFail() {
        DemoServicePreflight preflight = new DemoServicePreflight();
        String preferredUrl = "http://127.0.0.1:65529";
        String fallbackUrl = "http://127.0.0.1:65528";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> preflight.resolveReachable("Registry", preferredUrl, fallbackUrl, "dpp-registry-api")
        );

        assertTrue(exception.getMessage().contains(preferredUrl));
        assertTrue(exception.getMessage().contains(fallbackUrl));
        assertTrue(exception.getMessage().contains("dpp-registry-api"));
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void writeOkHealthResponse(HttpExchange exchange) throws IOException {
        byte[] body = "{\"status\":\"UP\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }
}
