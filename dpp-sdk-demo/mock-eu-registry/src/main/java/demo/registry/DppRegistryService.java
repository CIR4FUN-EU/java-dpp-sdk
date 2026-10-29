package demo.registry;

import dpp.registry.payloads.DppStatusCode;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.registry.payloads.RegisterDppResponse;
import java.time.Instant;
import java.net.URI;
import java.util.List;
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

    DppRegistryService(RegistryBackend backend) {
        this.backend = backend;
    }

    RegisterDppResponse register(RegisterDppRequest request) {
        if (request == null) {
            throw new RegistryApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_REGISTRATION",
                    "Registration request must not be null");
        }
        String productIdentifier = requireNonBlank(request.getUniqueProductIdentifier(), "uniqueProductIdentifier");
        String dppIdentifier = requireNonBlank(request.getDigitalProductPassportId(), "digitalProductPassportId");
        String operatorIdentifier = requireNonBlank(request.getUniqueEconomicOperatorIdentifier(), "uniqueEconomicOperatorIdentifier");
        String repoUrl = requireNonBlank(request.getDppApiEndpoint(), "dppApiEndpoint");
        validateEndpointUri(repoUrl);
        if (backend.existsByDppId(dppIdentifier)) {
            throw new RegistryApiException(DppStatusCode.ClientResourceConflict, "REGISTRY_CONFLICT",
                    "A registry record already exists for dpp id " + dppIdentifier);
        }

        RegistryRecord record = backend.create(
                productIdentifier,
                dppIdentifier,
                operatorIdentifier,
                repoUrl,
                Instant.now()
        );
        RegisterDppResponse response = new RegisterDppResponse();
        response.setRegistrationId(record.registryIdentifier());
        return response;
    }

    List<String> readAllRegisteredDppIds() {
        return backend.findAllRegisteredDppIds();
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

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new RegistryApiException(DppStatusCode.ClientErrorBadRequest, "MISSING_REQUIRED_FIELD",
                    fieldName + " must not be blank");
        }
        return value;
    }

    private void validateEndpointUri(String value) {
        final URI endpoint;
        try {
            endpoint = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new RegistryApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_DPP_API_ENDPOINT",
                    "dppApiEndpoint must be a syntactically valid URI");
        }
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
