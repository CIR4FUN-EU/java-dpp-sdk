package demo.registry;

import dpp.registry.payloads.DppStatusCode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
class RepositoryDppVerifier {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    void verifyActiveDpp(String repoUrl, String verificationRepoUrl, String dppIdentifier) {
        int statusCode;
        try {
            URI target = URI.create(stripTrailingSlash(verificationRepoUrl)
                    + "/internal/dpps/" + encodePathSegment(dppIdentifier));
            HttpRequest request = HttpRequest.newBuilder(target)
                    .timeout(Duration.ofSeconds(15))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            statusCode = httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (IOException | InterruptedException | IllegalArgumentException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw verificationFailed(repoUrl, verificationRepoUrl, dppIdentifier);
        }

        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        if (statusCode == 404) {
            throw new RegistryApiException(
                    DppStatusCode.ClientErrorResourceNotFound,
                    "REPO_DPP_NOT_FOUND",
                    "Referenced DPP " + dppIdentifier + " was not found in repo " + repoUrl + verificationSuffix(repoUrl, verificationRepoUrl)
            );
        }
        throw verificationFailed(repoUrl, verificationRepoUrl, dppIdentifier);
    }

    private RegistryApiException verificationFailed(String repoUrl, String verificationRepoUrl, String dppIdentifier) {
        return new RegistryApiException(
                DppStatusCode.ServerErrorBadGateway,
                "REPO_VERIFICATION_FAILED",
                "Repo verification failed for DPP " + dppIdentifier + " in repo " + repoUrl + verificationSuffix(repoUrl, verificationRepoUrl)
        );
    }

    private String verificationSuffix(String repoUrl, String verificationRepoUrl) {
        String normalizedRepoUrl = stripTrailingSlash(repoUrl);
        String normalizedVerificationRepoUrl = stripTrailingSlash(verificationRepoUrl);
        return normalizedRepoUrl.equals(normalizedVerificationRepoUrl) ? "" : " via " + normalizedVerificationRepoUrl;
    }

    private String stripTrailingSlash(String repoUrl) {
        return repoUrl.endsWith("/") ? repoUrl.substring(0, repoUrl.length() - 1) : repoUrl;
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
