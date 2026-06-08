package demo.registry;

import dpp.registry.payloads.DppApiResponse;
import dpp.registry.payloads.DppStatusCode;
import dpp.registry.payloads.RegisterDppRequest;
import dpp.registry.payloads.RegisterDppResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Exposes the mock EC registry metadata API.
 *
 * <p>The controller handles only registration metadata. It does not store or return full DPP JSON.
 * DPP existence is verified through the repository verifier and service registration flow before
 * metadata is stored.</p>
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "DPP Registry",
        description = "Mock registry metadata endpoints. The service stores metadata only and verifies DPP existence in the mock repository.")
class DppRegistryController {

    private static final String SEEDED_REGISTRY_ID = "8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc";
    private static final String SEEDED_REGISTRY_DPP_ID = "e7d64b7b-18f2-4d77-9c41-2fa1d1d6b8aa";

    private static final String REGISTER_DPP_EXAMPLE = """
            {
              "productIdentifier": "04012345678901",
              "dppIdentifier": "49192c87-20c8-4b6f-88de-48b56ca4c211",
              "operatorIdentifier": "operator-123",
              "repoUrl": "http://localhost:8080"
            }
            """;
    private static final String WRAPPED_SUCCESS_EXAMPLE = """
            {
              "statusCode": "Success",
              "payload": {
                "registryIdentifier": "8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc",
                "dppIdentifier": "e7d64b7b-18f2-4d77-9c41-2fa1d1d6b8aa",
                "productIdentifier": "04012345678902",
                "operatorIdentifier": "operator-123",
                "repoUrl": "http://localhost:8080"
              },
              "messages": []
            }
            """;
    private static final String ERROR_EXAMPLE = """
            {
              "statusCode": "ClientErrorBadRequest",
              "payload": null,
              "messages": [
                {
                  "messageType": "Error",
                  "code": "MALFORMED_JSON",
                  "text": "Malformed JSON payload",
                  "correlationId": "demo-correlation-id"
                }
              ]
            }
            """;

    private final DppRegistryService registryService;
    private final ApiResponseFactory responseFactory;

    DppRegistryController(DppRegistryService registryService, ApiResponseFactory responseFactory) {
        this.registryService = registryService;
        this.responseFactory = responseFactory;
    }

    @Operation(
            summary = "Register DPP metadata",
            description = "Stores DPP registry metadata after verifying the referenced DPP exists in the mock repository. The example references the seeded mock-repo DPP, so it works when both mock services are freshly started. This mock service does not store full DPP JSON.",
            tags = "DPP Registry"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Registry metadata request. `repoUrl` points to the mock repository public base URL. Change the DPP/product identifiers if you already registered the example DPP.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RegisterDppRequest.class),
                    examples = @ExampleObject(name = "Register DPP metadata", value = REGISTER_DPP_EXAMPLE))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registry metadata stored",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = WRAPPED_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Invalid request or malformed payload",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Referenced DPP was not found in the repo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "409", description = "DPP metadata already registered",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "502", description = "Mock repository verification failed or repository was unavailable",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected registry error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @PostMapping(value = "/registerDPP", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<RegisterDppResponse>> register(@RequestBody RegisterDppRequest request) {
        return responseFactory.success(HttpStatus.CREATED, DppStatusCode.SuccessCreated, registryService.register(request));
    }

    @Operation(
            summary = "Read registry metadata by registry id",
            description = "Returns mock registry metadata by registry identifier. The payload is metadata-only and never includes the full DPP document.",
            tags = "DPP Registry"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registry record found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = WRAPPED_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Registry record not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected registry error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @GetMapping("/registry/dpps/{registryId}")
    ResponseEntity<DppApiResponse<RegistryRecordPayload>> readByRegistryId(
            @Parameter(description = "Registry identifier. The example is seeded when the mock registry starts.",
                    example = SEEDED_REGISTRY_ID)
            @PathVariable String registryId) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, registryService.readByRegistryId(registryId));
    }

    @Operation(
            summary = "Read registry metadata by DPP id",
            description = "Returns mock registry metadata for a DPP identifier. The payload is metadata-only and never includes the full DPP document.",
            tags = "DPP Registry"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registry record found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = WRAPPED_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Registry record not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected registry error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @GetMapping("/registry/dpps/by-dpp-id/{dppId}")
    ResponseEntity<DppApiResponse<RegistryRecordPayload>> readByDppId(
            @Parameter(description = "DPP identifier registered in the mock registry. The example is seeded when the mock registry starts.",
                    example = SEEDED_REGISTRY_DPP_ID)
            @PathVariable String dppId) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, registryService.readByDppId(dppId));
    }

    @Operation(summary = "Registry health check",
            description = "Simple local mock-service health check.",
            tags = "DPP Registry")
    @GetMapping("/health")
    HealthPayload health() {
        return new HealthPayload("UP", "mock-eu-registry");
    }
}
