package demo.producer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class DemoServicePreflight {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    void verifyReachable(String displayName, String baseUrl, String moduleName) {
        ProbeResult probeResult = probe(baseUrl, moduleName);
        if (!probeResult.reachable()) {
            throw unreachable(displayName, baseUrl, moduleName, probeResult.failureDetail());
        }
    }

    String resolveReachable(String displayName, String preferredBaseUrl, String fallbackBaseUrl, String moduleName) {
        if (preferredBaseUrl.equals(fallbackBaseUrl)) {
            verifyReachable(displayName, preferredBaseUrl, moduleName);
            return preferredBaseUrl;
        }

        ProbeResult preferredProbe = probe(preferredBaseUrl, moduleName);
        if (preferredProbe.reachable()) {
            return preferredBaseUrl;
        }

        ProbeResult fallbackProbe = probe(fallbackBaseUrl, moduleName);
        if (fallbackProbe.reachable()) {
            return fallbackBaseUrl;
        }

        throw new IllegalStateException(
                displayName + " service is not reachable at either "
                        + preferredBaseUrl
                        + " or "
                        + fallbackBaseUrl
                        + ". Start "
                        + moduleName
                        + " before running dpp-integration-demo."
        );
    }

    private IllegalStateException unreachable(String displayName, String baseUrl, String moduleName, String detail) {
        return new IllegalStateException(
                displayName + " service is not reachable at " + baseUrl + ". "
                        + detail
        );
    }

    private ProbeResult probe(String baseUrl, String moduleName) {
        URI healthEndpoint;
        try {
            healthEndpoint = URI.create(stripTrailingSlash(baseUrl) + "/health");
        } catch (IllegalArgumentException exception) {
            return new ProbeResult(false, "The configured URL is invalid.");
        }

        HttpRequest request = HttpRequest.newBuilder(healthEndpoint)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<Void> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new ProbeResult(false, "Start " + moduleName + " before running dpp-integration-demo.");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return new ProbeResult(
                    false,
                    "Expected a healthy service at /health but received HTTP " + response.statusCode() + "."
            );
        }

        return new ProbeResult(true, "");
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record ProbeResult(boolean reachable, String failureDetail) {
    }
}
