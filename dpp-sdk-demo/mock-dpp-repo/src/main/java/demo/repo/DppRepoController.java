package demo.repo;

import static demo.repo.RepoSwaggerExamples.*;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import dpp.repo.payloads.CreateDppResponse;
import dpp.repo.payloads.DppApiResponse;
import dpp.repo.payloads.DppStatusCode;
import dpp.repo.payloads.ReadDppIdsRequest;
import dpp.repo.payloads.ReadDppIdsResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Exposes the standard-style mock repository API.
 *
 * <p>This controller stays thin: it accepts HTTP payloads, delegates lifecycle behavior to the service,
 * and wraps responses in the shared API envelope.</p>
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
class DppRepoController {

    private static final String API_PREFIX = "/v1";
    private static final String INTERNAL_PREFIX = "/internal";

    private static final String LANDING_PAGE = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <title>DPP Repository API</title>
            </head>
            <body>
              <h1>DPP Repository API</h1>
              <p>Service is running.</p>
              <p><a href="/swagger-ui/index.html">Open Swagger UI</a></p>
            </body>
            </html>
            """;

    private static final String SEEDED_DPP_ID = "49192c87-20c8-4b6f-88de-48b56ca4c211";
    private static final String SEEDED_PRODUCT_ID = "04012345678901";

    private final DppRepoService repoService;
    private final ApiResponseFactory responseFactory;

    DppRepoController(DppRepoService repoService, ApiResponseFactory responseFactory) {
        this.repoService = repoService;
        this.responseFactory = responseFactory;
    }

    @Hidden
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<String> landingPage() {
        return ResponseEntity.ok(LANDING_PAGE);
    }

    @Operation(
            summary = "Create a full DPP",
            description = "Stores a full DPP JSON document in the local mock repository and returns the created DPP identifier. The example is editable: change `passportMetadata.uniqueProductIdentifier` and `nameplate.gtinCode` when creating additional demo DPPs.",
            tags = "DPP Repository - Life Cycle API"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Full DPP JSON payload accepted by the mock repo and validated through the demo SDK codec/validator.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Object.class),
                    examples = @ExampleObject(name = "Full DPP JSON", value = CREATE_DPP_EXAMPLE))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "DPP created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = CREATE_DPP_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Invalid DPP or malformed JSON",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "DPP id or product id already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = DPP_CONFLICT_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @PostMapping(value = API_PREFIX + "/dpps", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<CreateDppResponse>> create(@RequestBody String jsonPayload) {
        return responseFactory.success(HttpStatus.CREATED, DppStatusCode.SuccessCreated, repoService.create(jsonPayload));
    }

    @Operation(summary = "Read a DPP by id",
            description = "Returns compressed by default or the full stored DPP when representation=full. See the official standards for payload requirements.",
            tags = "DPP Repository - Life Cycle API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DPP found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "Default compressed representation", value = COMPRESSED_SUCCESS_EXAMPLE),
                                    @ExampleObject(name = "Full representation", value = FULL_SUCCESS_EXAMPLE)
                            })),
            @ApiResponse(responseCode = "400", description = "Invalid representation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INVALID_REPRESENTATION_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "DPP not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(API_PREFIX + "/dpps/{dppId}")
    ResponseEntity<DppApiResponse<JsonNode>> readById(
            @Parameter(description = "DPP identifier to read. The example is seeded when the mock repo starts.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId,
            @Parameter(description = "Response representation. Omitted values default to compressed.",
                    schema = @Schema(allowableValues = {"compressed", "full"}, defaultValue = "compressed"),
                    example = "compressed")
            @RequestParam(value = "representation", required = false) String representation) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.readById(dppId, DppRepresentation.parse(representation)));
    }

    @Operation(summary = "Read all active DPP ids",
            description = "Internal/mock helper endpoint that lists all currently active DPP identifiers in the local repository.",
            tags = "DPP Repository - Internal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active DPP ids returned"),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(INTERNAL_PREFIX + "/dpps")
    ResponseEntity<DppApiResponse<List<String>>> readAllActiveDppIds() {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.readAllActiveDppIds());
    }

    @Operation(summary = "Verify active DPP existence",
            description = "HEAD endpoint used by the mock registry to verify that a DPP is active in the mock repository.",
            tags = "DPP Repository - Internal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DPP exists", content = @Content),
            @ApiResponse(responseCode = "404", description = "DPP not found", content = @Content)
    })
    @RequestMapping(value = INTERNAL_PREFIX + "/dpps/{dppId}", method = RequestMethod.HEAD)
    ResponseEntity<Void> verifyById(
            @Parameter(description = "DPP identifier to verify. The example is seeded when the mock repo starts.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId) {
        return repoService.hasActiveDpp(dppId)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Read active DPP by product id",
            description = "Returns compressed by default or the full stored DPP when representation=full. See the official standards for payload requirements.",
            tags = "DPP Repository - Life Cycle API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DPP found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "Default compressed representation", value = COMPRESSED_SUCCESS_EXAMPLE),
                                    @ExampleObject(name = "Full representation", value = FULL_SUCCESS_EXAMPLE)
                            })),
            @ApiResponse(responseCode = "400", description = "Invalid representation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INVALID_REPRESENTATION_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Product has no active DPP",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = PRODUCT_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(API_PREFIX + "/dppsByProductId/{productId}")
    ResponseEntity<DppApiResponse<JsonNode>> readByProductId(
            @Parameter(description = "Product identifier, for example a GTIN. The example is seeded when the mock repo starts.",
                    example = SEEDED_PRODUCT_ID)
            @PathVariable String productId,
            @Parameter(description = "Response representation. Omitted values default to compressed.",
                    schema = @Schema(allowableValues = {"compressed", "full"}, defaultValue = "compressed"),
                    example = "compressed")
            @RequestParam(value = "representation", required = false) String representation) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.readByProductId(productId, DppRepresentation.parse(representation)));
    }

    @Operation(summary = "Read historical DPP snapshot by DPP id and date",
            description = "Returns the repository snapshot active at the requested instant, compressed by default or full when representation=full. See the official standards for payload requirements.",
            tags = "DPP Repository - Life Cycle API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historical snapshot found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = HISTORICAL_COMPRESSED_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Invalid date or representation parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INVALID_DATE_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "No matching snapshot found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = DPP_VERSION_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(API_PREFIX + "/dppsByIdAndDate/{dppId}")
    ResponseEntity<DppApiResponse<JsonNode>> readByIdAndDate(
            @Parameter(description = "DPP identifier whose historical snapshot should be resolved.",
                    example = PostmanSeedData.HISTORICAL_SWAGGER_DPP_ID)
            @PathVariable String dppId,
            @Parameter(description = "ISO-8601 instant used for historical snapshot lookup.",
                    example = PostmanSeedData.HISTORICAL_SWAGGER_QUERY_AT)
            @RequestParam("date") String date,
            @Parameter(description = "Response representation. Omitted values default to compressed.",
                    schema = @Schema(allowableValues = {"compressed", "full"}, defaultValue = "compressed"),
                    example = "compressed")
            @RequestParam(value = "representation", required = false) String representation
    ) {
        // Snapshot lookup is based on repository history, not on any version fields inside the DPP document.
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.readVersionByDppIdAndDate(dppId, date, DppRepresentation.parse(representation)));
    }

    @Operation(summary = "Read DPP identifiers by product identifiers",
            description = "Returns active DPP identifiers for a list of product identifiers, with optional limit/cursor paging. The example product identifier is seeded when the mock repo starts.",
            tags = "DPP Repository - Life Cycle API")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Product identifiers plus optional limit and cursor. productIdentifiers is required and must not be empty.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReadDppIdsRequest.class),
                    examples = @ExampleObject(name = "Batch product lookup", value = READ_IDS_EXAMPLE))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DPP identifiers returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = READ_IDS_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Invalid request, limit, cursor, or product id list",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = EMPTY_PRODUCT_IDENTIFIERS_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @PostMapping(value = API_PREFIX + "/dppsByProductIds", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<ReadDppIdsResponse>> readIdsByProductIds(
            @RequestBody ReadDppIdsRequest request
    ) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.readIdsByProductIds(request));
    }

    @Operation(summary = "Patch a full DPP",
            description = "Applies a JSON Merge Patch to the full DPP and returns the updated full DPP JSON.",
            tags = "DPP Repository - Life Cycle API")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "JSON Merge Patch object. DPP id and product id are immutable.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Object.class),
                    examples = @ExampleObject(name = "Merge patch", value = MERGE_PATCH_EXAMPLE))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DPP updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = FULL_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Invalid patch, immutable-field change, or validation failure",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INVALID_PATCH_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "DPP not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Concurrent DPP version conflict",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = VERSION_CONFLICT_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @PatchMapping(value = API_PREFIX + "/dpps/{dppId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<JsonNode>> update(
            @Parameter(description = "DPP identifier to patch. Use the seeded example or the id returned by `POST /v1/dpps`.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId,
            @RequestBody String patchJson) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.updateById(dppId, patchJson));
    }

    @Operation(summary = "Read a fine-granular DPP element",
            description = "Reads one value through the bounded RFC 9535-compatible singular subset: $, dot members, quoted bracket members, and non-negative indexes. Malformed paths return 400, a supported path with no match returns 404, and unsupported RFC features return 501. Full RFC 9535 JSONPath is not implemented.",
            tags = "DPP Repository - Fine Granular API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Element returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = ELEMENT_READ_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Malformed elementIdPath",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INVALID_ELEMENT_PATH_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "DPP or selected data element not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = ELEMENT_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "501", description = "Valid RFC 9535 feature outside the bounded singular subset",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = NOT_IMPLEMENTED_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(API_PREFIX + "/dpps/{dppId}/elements/{elementIdPath}")
    ResponseEntity<DppApiResponse<JsonNode>> readDataElement(
            @Parameter(description = "DPP identifier to read from. The example is seeded when the mock repo starts.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId,
            @Parameter(description = "Bounded RFC 9535-compatible singular JSONPath. Example: $.characteristics.productName.", example = "$.characteristics.productName")
            @PathVariable String elementIdPath
    ) {
        // Evaluation is intentionally limited to singular JSONPath selectors in the service layer.
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.readDataElement(dppId, elementIdPath));
    }

    @Operation(summary = "Patch a fine-granular DPP element",
            description = "Updates one existing singular target using the data element directly. The resulting full DPP is decoded and validated before persistence; failed updates do not persist. Malformed paths return 400, no match returns 404, and unsupported RFC features return 501. Full RFC 9535 JSONPath is not implemented.",
            tags = "DPP Repository - Fine Granular API")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Direct JSON data element value, not a payload wrapper. It may be a scalar, object, or array accepted for the existing singular target.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Object.class),
                    examples = @ExampleObject(name = "Update product name", value = DATA_ELEMENT_UPDATE_EXAMPLE))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Element updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = ELEMENT_UPDATE_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Malformed elementIdPath, root replacement, or invalid payload",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INVALID_ELEMENT_PATH_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "DPP or selected data element not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = ELEMENT_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "501", description = "Valid RFC 9535 feature outside the bounded singular subset",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = NOT_IMPLEMENTED_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Concurrent DPP version conflict",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = VERSION_CONFLICT_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @PatchMapping(value = API_PREFIX + "/dpps/{dppId}/elements/{elementIdPath}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<JsonNode>> updateDataElement(
            @Parameter(description = "DPP identifier to update. Use the seeded example or the id returned by `POST /v1/dpps`.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId,
            @Parameter(description = "Bounded RFC 9535-compatible singular JSONPath. Example: $.characteristics.productName.", example = "$.characteristics.productName")
            @PathVariable String elementIdPath,
            @RequestBody JsonNode dataElement
    ) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.updateDataElement(dppId, elementIdPath, dataElement));
    }

    @Operation(summary = "Read DPP lifecycle events",
            description = "Returns lifecycle events recorded by the local mock repository for the requested DPP.",
            tags = "DPP Repository - Internal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lifecycle events returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = EVENTS_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "DPP not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(INTERNAL_PREFIX + "/dpps/{dppId}/events")
    ResponseEntity<DppApiResponse<List<LifecycleEventRecord>>> readEvents(
            @Parameter(description = "DPP identifier whose lifecycle events should be returned.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.readEvents(dppId));
    }

    @Operation(summary = "Delete an active DPP",
            description = "Soft-deletes the active DPP while preserving lifecycle history and historical snapshots.",
            tags = "DPP Repository - Life Cycle API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DPP soft-deleted",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = DELETE_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "DPP not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Concurrent DPP version conflict",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = VERSION_CONFLICT_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @DeleteMapping(API_PREFIX + "/dpps/{dppId}")
    ResponseEntity<DppApiResponse<Void>> delete(
            @Parameter(description = "DPP identifier to soft-delete. This example is seeded only for DELETE so the read/query examples stay queryable.",
                    example = PostmanSeedData.DELETE_EXAMPLE_DPP_ID)
            @PathVariable String dppId) {
        // Deletes are soft deletes so lifecycle history and historical snapshots remain queryable.
        repoService.deleteById(dppId);
        return responseFactory.successNoContent();
    }

    @Operation(summary = "Repository health check",
            description = "Simple local mock-service health check.",
            tags = "DPP Repository - Life Cycle API")
    @GetMapping("/health")
    HealthPayload health() {
        return new HealthPayload("UP", "dpp-repo-api");
    }
}
