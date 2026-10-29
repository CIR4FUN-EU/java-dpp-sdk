package demo.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import demo.producer.support.DemoDppFactory;
import dpp.registry.client.DppRegistryClient;
import dpp.registry.client.HttpDppRegistryClient;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.repo.client.HttpDppRepoClient;
import dpp.repo.payloads.DppRepresentation;
import dpp.repo.payloads.DppApiResponse;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import java.time.Instant;
import java.util.List;

/**
 * Demonstrates the SDK -> client -> demo service flow using the standard HTTP APIs.
 *
 * <p>This runner is for internal review and manual verification. It shows lifecycle, registry, and
 * fine-granular behaviors, but it is not production orchestration and its lifecycle events are only the
 * basic internal demo events rather than a full Track & Trace implementation.</p>
 */
class HttpServiceDemoRunner {

    private final DemoDppFactory factory = new DemoDppFactory();
    private final Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();
    private final Dpp4FunValidationService validator = new Dpp4FunValidationService();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DemoServicePreflight servicePreflight = new DemoServicePreflight();
    private final int registryPort;
    private final int repoPort;

    HttpServiceDemoRunner() {
        this(configuredPort("DPP_REGISTRY_PORT", 8081), configuredPort("DPP_REPO_PORT", 8080));
    }

    HttpServiceDemoRunner(int registryPort, int repoPort) {
        this.registryPort = registryPort;
        this.repoPort = repoPort;
    }

    void run(String... args) {
        run(null, args);
    }

    void run(DemoDppSamples samples, String... args) {
        String registryUrl = resolveServiceUrl(args, 0, "Registry", "dpp-registry-api",
                defaultRegistryDockerUrl(), defaultRegistryLocalUrl());
        String repoUrl = resolveServiceUrl(args, 1, "Repo", "dpp-repo-api",
                defaultRepoDockerUrl(), defaultRepoLocalUrl());

        HttpDppRepoClient repoClient = new HttpDppRepoClient(repoUrl);
        DppRegistryClient registryClient = new HttpDppRegistryClient(registryUrl);

        ConsoleSupport.header("DPP4Fun SDK + dpp-sdk-clients Standards Demo");
        System.out.println("Repo     : " + repoUrl);
        System.out.println("Registry : " + registryUrl);
        System.out.println("Purpose  : standard-style lifecycle, registry, and fine-granular APIs");

        Dpp4Fun dppTemplate = samples == null ? factory.createValidBedDpp() : samples.validBedDpp();
        Dpp4Fun dpp = factory.withFreshDppId(dppTemplate);
        Dpp4Fun invalidDpp = samples == null
                ? factory.createIncompleteDpp()
                : samples.incompleteDpp();
        String dppId = DemoDppFactory.idOf(dpp);
        String productId = dpp.getUniqueProductIdentifier();
        deleteStaleDemoDppIfPresent(repoClient, productId);

        // Demonstrates createDpp -> POST /v1/dpps and the SuccessCreated wrapper payload with digitalProductPassportId.
        ConsoleSupport.step("CreateDPP");
        validate(dpp);
        JsonNode dppJson;
        try {
            dppJson = objectMapper.readTree(codec.toJson(dpp));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Could not encode the typed DPP as neutral JSON", exception);
        }
        var created = repoClient.createDpp(dppJson, DppRepresentation.FULL);
        System.out.println("statusCode : " + created.getStatusCode());
        System.out.println("message    : " + created.getMessage());
        ConsoleSupport.createResponse(created);

        // Shows EN 18223 compressed JSON beside the complete DPP decoded by the typed client.
        ConsoleSupport.step("Compare compressed and full DPP representations");
        DppApiResponse<JsonNode> compressedResponse = repoClient.readDppById(dppId);
        System.out.println("statusCode : " + compressedResponse.getStatusCode());
        System.out.println("message    : " + compressedResponse.getMessage());
        ConsoleSupport.compressedDpp(compressedResponse.getPayload());
        DppApiResponse<JsonNode> fullResponse = repoClient.readDppById(dppId, DppRepresentation.FULL);
        ConsoleSupport.jsonValue("FULL DPP (expanded JSON)", fullResponse.getPayload());

        // Demonstrates readDppByProductId -> GET /v1/dppsByProductId/{productId} for the active product mapping.
        ConsoleSupport.step("ReadDPPByProductId");
        DppApiResponse<JsonNode> productRead = repoClient.readDppByProductId(productId, DppRepresentation.FULL);
        ConsoleSupport.jsonValue("FULL DPP by product ID", productRead.getPayload());

        // Demonstrates updateDppById -> PATCH /v1/dpps/{dppId} with a raw merge-patch payload.
        ConsoleSupport.step("UpdateDPPById");
        ObjectNode patch = objectMapper.createObjectNode();
        Dpp4Fun updatedDpp = factory.withBrand(dpp, "Updated via standards demo");
        try {
            patch.set("elements", objectMapper.readTree(codec.toJson(updatedDpp)).get("elements"));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not prepare canonical DPP update", exception);
        }
        ConsoleSupport.jsonValue("Updated FULL DPP", repoClient.updateDppById(
                dppId, patch, DppRepresentation.FULL).getPayload());

        // Demonstrates a dot-member selector, an array-index selector, and a replacement of one singular target.
        ConsoleSupport.step("Fine-granular JSONPath read and update");
        String brandPath = DemoDppFactory.BRAND_PATH;
        String firstMaterialNamePath = DemoDppFactory.FIRST_MATERIAL_NAME_PATH;
        String nameplatePath = "$.elements[1]";
        ConsoleSupport.jsonPathValue("Nameplate collection", nameplatePath,
                repoClient.readDataElement(dppId, nameplatePath, DppRepresentation.FULL).getPayload());
        ConsoleSupport.jsonPathValue("Brand value before update", brandPath,
                repoClient.readDataElement(dppId, brandPath).getPayload());
        ConsoleSupport.jsonPathValue("First BOM material name", firstMaterialNamePath,
                repoClient.readDataElement(dppId, firstMaterialNamePath).getPayload());
        JsonNode brandElement = repoClient.readDataElement(dppId, brandPath, DppRepresentation.FULL).getPayload();
        ConsoleSupport.jsonPathValue("Brand DataElement (FULL)", brandPath, brandElement);
        ((com.fasterxml.jackson.databind.node.ObjectNode) brandElement).put("value", "Updated element value");
        ConsoleSupport.jsonPathValue("Brand DataElement after update", brandPath, repoClient.updateDataElement(
                dppId,
                brandPath,
                brandElement,
                DppRepresentation.FULL
        ).getPayload());

        // Demonstrates snapshot lookup by DPP identifier and repository history time, not by any DPP metadata version field.
        ConsoleSupport.step("ReadDPPVersionByIdAndDate");
        DppApiResponse<JsonNode> historical = repoClient.readDppVersionByIdAndDate(
                dppId, Instant.now(), DppRepresentation.FULL);
        ConsoleSupport.jsonValue("Historical FULL DPP", historical.getPayload());

        // Demonstrates batched identifier lookup via POST /v1/dppsByProductIds.
        ConsoleSupport.step("ReadDPPIdsByProductIds");
        ConsoleSupport.idsResponse(repoClient.readDppIdsByProductIds(List.of(productId), 10, "0"));

        // Demonstrates independent registry metadata registration and lookup.
        ConsoleSupport.step("Register stored DPP in Registry");
        RegisterDppRequest registerRequest = new RegisterDppRequest(
                productId,
                dppId,
                "operator-123",
                repoUrl
        );
        ConsoleSupport.registryResponse(registryClient.postNewDppToRegistry(registerRequest));

        // The neutral JSON client stays model-independent; the application validates before deciding to send.
        ConsoleSupport.step("Invalid typed DPP rejected by application validation before send");
        try {
            validate(invalidDpp);
            throw new IllegalStateException("The deliberately incomplete DPP unexpectedly passed validation");
        } catch (dppsdk.core.validation.ValidationException exception) {
            System.out.println("validation error : " + exception.getMessage());
        }

        // Demonstrates API/HTTP failure handling for a missing repository object.
        ConsoleSupport.step("HTTP error from missing repo object");
        try {
            repoClient.readDppById("missing-dpp-id");
        } catch (dpp.repo.client.exception.DppClientException exception) {
            ConsoleSupport.clientError(exception);
        }

        // Demonstrates network exception mapping with the fixed internal timeout/network behavior from dpp-client.
        ConsoleSupport.step("Network error from unreachable registry client");
        try {
            new HttpDppRegistryClient("http://localhost:65530").postNewDppToRegistry(registerRequest);
        } catch (dpp.registry.client.exception.DppClientException exception) {
            ConsoleSupport.clientError(exception);
        }

        // Demonstrates soft delete via DELETE /v1/dpps/{dppId}; lifecycle history remains a demo internal concern.
        ConsoleSupport.step("DeleteDPPById");
        ConsoleSupport.deleteResponse(repoClient.deleteDppById(dppId));

        ConsoleSupport.header("HTTP services demo complete");
    }

    String defaultRegistryDockerUrl() {
        return "http://dpp-registry-api:" + registryPort;
    }

    String defaultRegistryLocalUrl() {
        return "http://localhost:" + registryPort;
    }

    String defaultRepoDockerUrl() {
        return "http://dpp-repo-api:" + repoPort;
    }

    String defaultRepoLocalUrl() {
        return "http://localhost:" + repoPort;
    }

    private String resolveServiceUrl(
            String[] args,
            int index,
            String displayName,
            String moduleName,
            String dockerUrl,
            String localUrl
    ) {
        if (args.length > index && !args[index].startsWith("--")) {
            String explicitUrl = args[index];
            servicePreflight.verifyReachable(displayName, explicitUrl, moduleName);
            return explicitUrl;
        }
        return servicePreflight.resolveReachable(displayName, dockerUrl, localUrl, moduleName);
    }

    private void deleteStaleDemoDppIfPresent(HttpDppRepoClient repoClient, String productId) {
        try {
            JsonNode existingDpp = repoClient.readDppByProductId(productId).getPayload();
            ConsoleSupport.step("Cleanup stale demo DPP");
            String existingDppId = existingDpp.path("digitalProductPassportId").asText();
            System.out.println("dppId      : " + existingDppId);
            ConsoleSupport.deleteResponse(repoClient.deleteDppById(existingDppId));
        } catch (dpp.repo.client.exception.DppClientException exception) {
            if (!isNotFound(exception)) {
                throw exception;
            }
        }
    }

    private void validate(Dpp4Fun dpp) {
        var result = validator.validate(dpp);
        if (!result.isValid()) {
            throw new dppsdk.core.validation.ValidationException("Dpp4Fun validation failed: " + result.issues());
        }
    }

    private boolean isNotFound(dpp.repo.client.exception.DppClientException exception) {
        if (exception instanceof dpp.repo.client.exception.DppApiClientException apiException) {
            return apiException.httpStatus() == 404
                    || apiException.statusCode() == dpp.repo.payloads.DppStatusCode.ClientErrorResourceNotFound;
        }
        return exception instanceof dpp.repo.client.exception.DppHttpClientException httpException
                && httpException.statusCode() == 404;
    }

    private static int configuredPort(String key, int fallback) {
        String raw = System.getProperty(key, System.getenv(key));
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid numeric port configured for " + key + ": " + raw, exception);
        }
    }

}
