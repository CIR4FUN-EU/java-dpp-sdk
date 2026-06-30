package demo.registry;

import dpp.registry.payloads.DppStatusCode;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.registry.payloads.RegisterDppResponse;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Stores and serves registry metadata for the mock EC registry service.
 *
 * <p>The active contract is intentionally small: identifiers plus the repository URL. Backup provider
 * and operator concepts are future work and are not part of the current runtime payloads.</p>
 */
@Service
class DppRegistryService {

    private final RegistryBackend backend;
    private final RepositoryDppVerifier repositoryDppVerifier;
    private final String publicRepoBaseUrl;
    private final String verificationRepoBaseUrl;

    DppRegistryService(
            RegistryBackend backend,
            RepositoryDppVerifier repositoryDppVerifier,
            @Value("${demo.repo.public-base-url:http://localhost:${MOCK_REPO_PORT:${DPP_REPO_PORT:8080}}}") String publicRepoBaseUrl,
            @Value("${demo.repo.verification-base-url:http://localhost:${MOCK_REPO_PORT:${DPP_REPO_PORT:8080}}}") String verificationRepoBaseUrl
    ) {
        this.backend = backend;
        this.repositoryDppVerifier = repositoryDppVerifier;
        this.publicRepoBaseUrl = publicRepoBaseUrl;
        this.verificationRepoBaseUrl = verificationRepoBaseUrl;
    }

    RegisterDppResponse register(RegisterDppRequest request) {
        if (request == null) {
            throw new RegistryApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_REGISTRATION",
                    "Registration request must not be null");
        }
        String productIdentifier = requireNonBlank(request.getProductIdentifier(), "productIdentifier");
        String dppIdentifier = requireNonBlank(request.getDppIdentifier(), "dppIdentifier");
        String operatorIdentifier = requireNonBlank(request.getOperatorIdentifier(), "operatorIdentifier");
        String repoUrl = requireNonBlank(request.getRepoUrl(), "repoUrl");
        if (backend.existsByDppId(dppIdentifier)) {
            throw new RegistryApiException(DppStatusCode.ClientResourceConflict, "REGISTRY_CONFLICT",
                    "A registry record already exists for dpp id " + dppIdentifier);
        }

        repositoryDppVerifier.verifyActiveDpp(repoUrl, verificationRepoUrl(repoUrl), dppIdentifier);

        RegistryRecord record = backend.create(
                productIdentifier,
                dppIdentifier,
                operatorIdentifier,
                repoUrl,
                Instant.now()
        );
        RegisterDppResponse response = new RegisterDppResponse();
        response.setRegistryIdentifier(record.registryIdentifier());
        return response;
    }

    RegistryRecordPayload readByRegistryId(String registryId) {
        RegistryRecord record = backend.findByRegistryId(registryId)
                .orElseThrow(() -> new RegistryApiException(DppStatusCode.ClientErrorResourceNotFound,
                        "REGISTRY_NOT_FOUND", "No registry record found for id " + registryId));
        return toPayload(record);
    }

    RegistryRecordPayload readByDppId(String dppId) {
        RegistryRecord record = backend.findByDppId(dppId)
                .orElseThrow(() -> new RegistryApiException(DppStatusCode.ClientErrorResourceNotFound,
                        "REGISTRY_NOT_FOUND", "No registry record found for dpp id " + dppId));
        return toPayload(record);
    }

    void clear() {
        backend.clear();
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new RegistryApiException(DppStatusCode.ClientErrorBadRequest, "MISSING_REQUIRED_FIELD",
                    fieldName + " must not be blank");
        }
        return value;
    }

    private String verificationRepoUrl(String requestedRepoUrl) {
        if (normalized(requestedRepoUrl).equals(normalized(publicRepoBaseUrl))) {
            return verificationRepoBaseUrl;
        }
        return requestedRepoUrl;
    }

    private String normalized(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private RegistryRecordPayload toPayload(RegistryRecord record) {
        return new RegistryRecordPayload(
                record.registryIdentifier(),
                record.dppIdentifier(),
                record.productIdentifier(),
                record.operatorIdentifier(),
                record.repoUrl(),
                record.registeredAt(),
                record.lastUpdatedAt()
        );
    }
}
