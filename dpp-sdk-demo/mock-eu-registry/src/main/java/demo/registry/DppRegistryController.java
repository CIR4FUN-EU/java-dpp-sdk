package demo.registry;

import static demo.registry.RegistrySwaggerExamples.*;

import java.net.URI;

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

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;

/**
 * Exposes the mock EC registry metadata API.
 *
 * <p>The controller handles only registration metadata. It does not store or return full DPP JSON.
 * DPP existence is verified through the repository verifier and service registration flow before
 * metadata is stored.</p>
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
class DppRegistryController {

    private static final String INTERNAL_PREFIX = "/internal";

    private static final String SEEDED_REGISTRY_ID = "8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc";
    private static final String SEEDED_REGISTRY_DPP_ID = "e7d64b7b-18f2-4d77-9c41-2fa1d1d6b8aa";

    private final DppRegistryService registryService;
    private final ApiResponseFactory responseFactory;

    DppRegistryController(DppRegistryService registryService, ApiResponseFactory responseFactory) {
        this.registryService = registryService;
        this.responseFactory = responseFactory;
    }

    @Hidden
    @GetMapping("/")
    ResponseEntity<Void> root() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/swagger-ui/index.html"))
                .build();
    }

    @Operation(
            summary = "Register DPP metadata",
            description = "Stores DPP registry metadata after verifying the referenced DPP exists in the mock repository. The example references the seeded mock-repo DPP, so it works when both mock services are freshly started. This mock service does not store full DPP JSON.",
            tags = "DPP Registry"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Registry metadata request. `dppApiEndpoint` points to the mock repository public base URL. Change the DPP/product identifiers if you already registered the example DPP.",
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
                            examples = @ExampleObject(value = BAD_REQUEST_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Referenced DPP was not found in the repo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = REPO_DPP_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "DPP metadata already registered",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = CONFLICT_EXAMPLE))),
            @ApiResponse(responseCode = "502", description = "Mock repository verification failed or repository was unavailable",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = BAD_GATEWAY_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected registry error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @PostMapping(value = "/v1/registerDPP", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<RegisterDppResponse>> register(@RequestBody RegisterDppRequest request) {
        return responseFactory.success(HttpStatus.CREATED, DppStatusCode.SuccessCreated, registryService.register(request));
    }

    @Operation(
            summary = "Read registry metadata by registry id",
            description = "Returns mock registry metadata by registry identifier. The payload is metadata-only and never includes the full DPP document.",
            tags = "DPP Registry - Internal"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registry record found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_LOOKUP_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Registry record not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = REGISTRY_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected registry error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(INTERNAL_PREFIX + "/dpps/{registryId}")
    ResponseEntity<DppApiResponse<RegistryRecordPayload>> readByRegistryId(
            @Parameter(description = "Registry identifier. The example is seeded when the mock registry starts.",
                    example = SEEDED_REGISTRY_ID)
            @PathVariable String registryId) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, registryService.readByRegistryId(registryId));
    }

    @Operation(summary = "List registered DPP ids",
            description = "Internal/mock helper that lists DPP ids registered in this metadata-only mock registry.",
            tags = "DPP Registry - Internal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registered DPP ids returned"),
            @ApiResponse(responseCode = "500", description = "Unexpected registry error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(INTERNAL_PREFIX + "/dpps")
    ResponseEntity<DppApiResponse<List<String>>> readAllRegisteredDppIds() {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, registryService.readAllRegisteredDppIds());
    }

    @Operation(
            summary = "Read registry metadata by DPP id",
            description = "Returns mock registry metadata for a DPP identifier. The payload is metadata-only and never includes the full DPP document.",
            tags = "DPP Registry - Internal"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registry record found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_LOOKUP_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Registry record not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = REGISTRY_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected registry error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(INTERNAL_PREFIX + "/dpps/by-dpp-id/{dppId}")
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
        return new HealthPayload("UP", "dpp-registry-api");
    }
}
