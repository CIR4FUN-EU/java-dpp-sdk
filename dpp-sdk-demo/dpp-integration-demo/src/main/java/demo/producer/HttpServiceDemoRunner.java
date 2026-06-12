package demo.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import demo.producer.support.DemoDppFactory;
import demo.producer.support.Dpp4FunDppCodecAdapter;
import demo.producer.support.Dpp4FunDppValidatorAdapter;
import dpp.registry.client.DppRegistryClient;
import dpp.registry.client.HttpDppRegistryClient;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.repo.client.HttpDppRepoClient;
import dppsdk.dpp4fun.model.Dpp4Fun;
import java.time.Instant;
import java.util.List;

/**
 * Demonstrates the SDK -> client -> mock service flow using the standard HTTP APIs.
 *
 * <p>This runner is for internal review and manual verification. It shows lifecycle, registry, and
 * fine-granular behaviors, but it is not production orchestration and its lifecycle events are only the
 * basic internal mock events rather than a full Track & Trace implementation.</p>
 */
class HttpServiceDemoRunner {

    private final DemoDppFactory factory = new DemoDppFactory();
    private final Dpp4FunDppCodecAdapter codec = new Dpp4FunDppCodecAdapter();
    private final Dpp4FunDppValidatorAdapter validator = new Dpp4FunDppValidatorAdapter();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
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
        String registryUrl = resolveServiceUrl(args, 0, "Registry", "mock-eu-registry",
                defaultRegistryDockerUrl(), defaultRegistryLocalUrl());
        String repoUrl = resolveServiceUrl(args, 1, "Repo", "mock-dpp-repo",
                defaultRepoDockerUrl(), defaultRepoLocalUrl());

        HttpDppRepoClient<Dpp4Fun> repoClient = new HttpDppRepoClient<>(repoUrl, codec, validator);
        DppRegistryClient registryClient = new HttpDppRegistryClient(registryUrl);
        MockRegistryLookupClient mockRegistryLookupClient = new MockRegistryLookupClient(registryUrl, objectMapper);

        ConsoleSupport.header("DPP4Fun SDK + dpp-sdk-clients Standards Demo");
        System.out.println("Repo     : " + repoUrl);
        System.out.println("Registry : " + registryUrl);
        System.out.println("Purpose  : standard-style lifecycle, registry, and fine-granular APIs");

        Dpp4Fun dppTemplate = samples == null ? factory.createValidBedDpp() : samples.validBedDpp();
        Dpp4Fun dpp = factory.withFreshDppId(dppTemplate);
        Dpp4Fun invalidDpp = samples == null
                ? factory.createDppWithWrongSupplierRole()
                : samples.invalidSupplierRoleDpp();
        String dppId = dpp.getDppId();
        String productId = dpp.getProductId();
        deleteStaleDemoDppIfPresent(repoClient, productId);

        // Demonstrates createDpp -> POST /dpps and the SuccessCreated wrapper payload with dppId.
        ConsoleSupport.step("CreateDPP");
        ConsoleSupport.createResponse(repoClient.createDpp(dpp));

        // Demonstrates readDppById -> GET /dpps/{dppId} using full DPP JSON decoded by the client codec.
        ConsoleSupport.step("ReadDPPById");
        ConsoleSupport.dpp(repoClient.readDppById(dppId));

        // Demonstrates readDppByProductId -> GET /dppsByProductId/{productId} for the active product mapping.
        ConsoleSupport.step("ReadDPPByProductId");
        ConsoleSupport.dpp(repoClient.readDppByProductId(productId));

        // Demonstrates updateDppById -> PATCH /dpps/{dppId} with a raw merge-patch payload.
        ConsoleSupport.step("UpdateDPPById");
        ObjectNode patch = objectMapper.createObjectNode();
        patch.putObject("characteristics").put("productName", "Updated via standards demo");
        ConsoleSupport.dpp(repoClient.updateDppById(dppId, patch));

        // Demonstrates fine-granular read/write helpers against the controlled element-path API.
        ConsoleSupport.step("ReadDataElement and UpdateDataElement");
        ConsoleSupport.jsonValue("readElement ", repoClient.readDataElement(dppId, "characteristics.productName"));
        ConsoleSupport.jsonValue("updatedElement", repoClient.updateDataElement(
                dppId,
                "characteristics.productName",
                objectMapper.getNodeFactory().textNode("Updated element value")
        ));

        // Demonstrates snapshot lookup by repository history time, not by any DPP metadata version field.
        ConsoleSupport.step("ReadDPPVersionByProductIdAndDate");
        ConsoleSupport.dpp(repoClient.readDppVersionByProductIdAndDate(productId, Instant.now()));

        // Demonstrates batched identifier lookup via POST /dppsByProductIds.
        ConsoleSupport.step("ReadDPPIdsByProductIds");
        ConsoleSupport.idsResponse(repoClient.readDppIdsByProductIds(List.of(productId), 10, "0"));

        // Demonstrates registry metadata registration and lookup; the registry verifies the repo reference by HEAD.
        ConsoleSupport.step("Register stored DPP in Registry");
        System.out.println("repoVerification : HEAD /dpps/" + dppId);
        RegisterDppRequest registerRequest = new RegisterDppRequest(
                productId,
                dppId,
                "operator-123",
                repoUrl
        );
        ConsoleSupport.registryResponse(registryClient.postNewDppToRegistry(registerRequest));
        mockRegistryLookupClient.readByDppId(dppId).ifPresent(ConsoleSupport::registryRecord);

        ConsoleSupport.step("Registry rejects missing repo DPP");
        try {
            registryClient.postNewDppToRegistry(new RegisterDppRequest(
                    productId,
                    "missing-dpp-id",
                    "operator-123",
                    repoUrl
            ));
        } catch (dpp.registry.client.exception.DppClientException exception) {
            ConsoleSupport.clientError(exception);
        }

        // Demonstrates client-side validation before any HTTP request is sent.
        ConsoleSupport.step("Invalid DPP rejected before send by client-side validation");
        try {
            repoClient.createDpp(invalidDpp);
        } catch (dpp.repo.client.exception.DppClientException exception) {
            ConsoleSupport.clientError(exception);
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

        // Demonstrates soft delete via DELETE /dpps/{dppId}; lifecycle history remains a mock internal concern.
        ConsoleSupport.step("DeleteDPPById");
        ConsoleSupport.deleteResponse(repoClient.deleteDppById(dppId));

        ConsoleSupport.header("HTTP services demo complete");
    }

    String defaultRegistryDockerUrl() {
        return "http://mock-eu-registry:" + registryPort;
    }

    String defaultRegistryLocalUrl() {
        return "http://localhost:" + registryPort;
    }

    String defaultRepoDockerUrl() {
        return "http://mock-dpp-repo:" + repoPort;
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

    private void deleteStaleDemoDppIfPresent(HttpDppRepoClient<Dpp4Fun> repoClient, String productId) {
        try {
            Dpp4Fun existingDpp = repoClient.readDppByProductId(productId);
            ConsoleSupport.step("Cleanup stale demo DPP");
            System.out.println("dppId      : " + existingDpp.getDppId());
            ConsoleSupport.deleteResponse(repoClient.deleteDppById(existingDpp.getDppId()));
        } catch (dpp.repo.client.exception.DppHttpClientException exception) {
            if (exception.statusCode() != 404) {
                throw exception;
            }
        }
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
