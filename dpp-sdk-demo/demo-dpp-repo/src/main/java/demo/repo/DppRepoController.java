package demo.repo;

import static demo.repo.RepoSwaggerExamples.*;

import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import dpp.repo.payloads.CreateDppResponse;
import dpp.repo.payloads.DppApiResponse;
import dpp.repo.payloads.DppStatusCode;
import dpp.repo.payloads.DppRepresentation;
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
 * Exposes the standard-style demo repository API.
 *
 * <p>This controller stays thin: it accepts HTTP payloads, delegates lifecycle behavior to the service,
 * and wraps responses in the shared API envelope.</p>
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
class DppRepoController {

    private static final String API_PREFIX = "/v1";
    private static final String INTERNAL_PREFIX = "/internal";

    private static final String SEEDED_DPP_ID = "49192c87-20c8-4b6f-88de-48b56ca4c211";
    private static final String SEEDED_PRODUCT_ID = "04012345678901";

    private final DppRepoService repoService;
    private final ApiResponseFactory responseFactory;

    DppRepoController(DppRepoService repoService, ApiResponseFactory responseFactory) {
        this.repoService = repoService;
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
            summary = "Create a DPP",
            description = "Stores EN 18223 compressed JSON by default or expanded JSON when representation=full, and returns the created DPP ID.",
            tags = "DPP Repository - Life Cycle API"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "DPP JSON in the selected representation, decoded and validated through the SDK.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Object.class),
                    examples = @ExampleObject(name = "Full DPP JSON", value = CREATE_DPP_EXAMPLE))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "DPP created", useReturnTypeSchema = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = CREATE_DPP_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Invalid DPP or malformed JSON",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "DPP ID or unique product ID already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = DPP_CONFLICT_EXAMPLE))),
            @ApiResponse(responseCode = "405", description = "HTTP method is not allowed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = METHOD_NOT_ALLOWED_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @PostMapping(value = API_PREFIX + "/dpps", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<CreateDppResponse>> create(
            @RequestBody String jsonPayload,
            @Parameter(description = "Request representation. Omission means compressed.",
                    schema = @Schema(allowableValues = {"compressed", "full"}, defaultValue = "compressed"))
            @RequestParam(value = "representation", required = false) String representation) {
        return responseFactory.success(HttpStatus.CREATED, DppStatusCode.SuccessCreated,
                repoService.create(jsonPayload, parseRepresentation(representation)));
    }

    @Operation(summary = "Read a DPP by ID",
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
            @ApiResponse(responseCode = "405", description = "HTTP method is not allowed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = METHOD_NOT_ALLOWED_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(API_PREFIX + "/dpps/{dppId}")
    ResponseEntity<DppApiResponse<JsonNode>> readById(
            @Parameter(description = "DPP ID to read. The example is seeded when the demo repo starts.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId,
            @Parameter(description = "Response representation. Omitted values default to compressed.",
                    schema = @Schema(allowableValues = {"compressed", "full"}, defaultValue = "compressed"),
                    example = "compressed")
            @RequestParam(value = "representation", required = false) String representation) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.readById(dppId, parseRepresentation(representation)));
    }

    @Operation(summary = "Read all active DPP IDs",
            description = "Internal demo helper endpoint that lists the IDs of all currently active DPPs in the local repository.",
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

    @Operation(summary = "Read active DPP by product ID",
            description = "Returns the active DPP associated with the supplied unique product ID, compressed by default or full when representation=full.",
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
            @ApiResponse(responseCode = "405", description = "HTTP method is not allowed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = METHOD_NOT_ALLOWED_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(API_PREFIX + "/dppsByProductId/{productId}")
    ResponseEntity<DppApiResponse<JsonNode>> readByProductId(
            @Parameter(description = "Unique product ID, for example a GTIN. The example is seeded when the demo repo starts.",
                    example = SEEDED_PRODUCT_ID)
            @PathVariable String productId,
            @Parameter(description = "Response representation. Omitted values default to compressed.",
                    schema = @Schema(allowableValues = {"compressed", "full"}, defaultValue = "compressed"),
                    example = "compressed")
            @RequestParam(value = "representation", required = false) String representation) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.readByProductId(productId, parseRepresentation(representation)));
    }

    @Operation(summary = "Read historical DPP snapshot by DPP ID and date",
            description = "Returns the repository snapshot active at the requested UTC instant, compressed by default or full when representation=full.",
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
            @ApiResponse(responseCode = "405", description = "HTTP method is not allowed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = METHOD_NOT_ALLOWED_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(API_PREFIX + "/dppsByIdAndDate/{dppId}")
    ResponseEntity<DppApiResponse<JsonNode>> readByIdAndDate(
            @Parameter(description = "DPP ID whose historical snapshot should be resolved.",
                    example = PostmanSeedData.HISTORICAL_SWAGGER_DPP_ID)
            @PathVariable String dppId,
            @Parameter(description = "UTC ISO-8601 instant used for historical snapshot lookup.",
                    schema = @Schema(type = "string", format = "date-time"),
                    example = PostmanSeedData.HISTORICAL_SWAGGER_QUERY_AT)
            @RequestParam("date") String date,
            @Parameter(description = "Response representation. Omitted values default to compressed.",
                    schema = @Schema(allowableValues = {"compressed", "full"}, defaultValue = "compressed"),
                    example = "compressed")
            @RequestParam(value = "representation", required = false) String representation
    ) {
        // Snapshot lookup is based on repository history, not on any version fields inside the DPP document.
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.readVersionByDppIdAndDate(dppId, date, parseRepresentation(representation)));
    }

    @Operation(summary = "Find DPPs by product IDs",
            description = "Returns the IDs of active DPPs matching the supplied product IDs. Supports optional limit and cursor pagination. The example product ID is seeded when the demo repo starts.",
            tags = "DPP Repository - Life Cycle API")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Unique product IDs to match against active DPP records, plus optional limit and cursor. `productIdentifiers` is required and must contain at least one item.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReadDppIdsRequest.class),
                    examples = @ExampleObject(name = "Batch product lookup", value = READ_IDS_EXAMPLE))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching DPP IDs returned", useReturnTypeSchema = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = READ_IDS_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "400", description = "Invalid request, limit, cursor, or product id list",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = EMPTY_PRODUCT_IDENTIFIERS_EXAMPLE))),
            @ApiResponse(responseCode = "405", description = "HTTP method is not allowed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = METHOD_NOT_ALLOWED_EXAMPLE))),
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

    @Operation(summary = "Patch a DPP",
            description = "Applies a JSON Merge Patch in the selected representation and returns the updated DPP in that representation.",
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
            @ApiResponse(responseCode = "405", description = "HTTP method is not allowed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = METHOD_NOT_ALLOWED_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @PatchMapping(value = API_PREFIX + "/dpps/{dppId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<JsonNode>> update(
            @Parameter(description = "DPP ID to patch. Use the seeded example or the ID returned by `POST /v1/dpps`.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId,
            @RequestBody String patchJson,
            @Parameter(description = "Request/response representation. Omission means compressed.",
                    schema = @Schema(allowableValues = {"compressed", "full"}, defaultValue = "compressed"))
            @RequestParam(value = "representation", required = false) String representation) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.updateById(dppId, patchJson, parseRepresentation(representation)));
    }

    @Operation(summary = "Read a fine-granular DPP element",
            description = "Reads one DataElement in compressed form by default or expanded form when representation=full. Supports the bounded singular JSONPath subset: $, dot members, quoted bracket members, and non-negative indexes. Malformed paths return 400; unsupported RFC features return a bodyless 501.",
            tags = "DPP Repository - Fine Granular API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Element returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "Default compressed representation", value = ELEMENT_READ_COMPRESSED_SUCCESS_EXAMPLE),
                                    @ExampleObject(name = "Full representation", value = ELEMENT_READ_FULL_SUCCESS_EXAMPLE)
                            })),
            @ApiResponse(responseCode = "400", description = "Malformed elementIdPath",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INVALID_ELEMENT_PATH_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "DPP or selected data element not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = ELEMENT_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "501", description = "Valid RFC 9535 feature outside the bounded singular subset", content = @Content),
            @ApiResponse(responseCode = "405", description = "HTTP method is not allowed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = METHOD_NOT_ALLOWED_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @GetMapping(API_PREFIX + "/dpps/{dppId}/elements/{elementIdPath}")
    ResponseEntity<DppApiResponse<JsonNode>> readDataElement(
            @Parameter(description = "DPP ID to read from. The example is seeded when the demo repo starts.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId,
            @Parameter(description = "Bounded singular JSONPath selecting a complete canonical serialized DataElement.",
                    example = "$.elements[1].elements[7]")
            @PathVariable String elementIdPath,
            @Parameter(description = "Response representation. Omission means compressed.",
                    schema = @Schema(allowableValues = {"compressed", "full"}, defaultValue = "compressed"))
            @RequestParam(value = "representation", required = false) String representation
    ) {
        // Evaluation is intentionally limited to singular JSONPath selectors in the service layer.
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.readDataElement(dppId, elementIdPath, parseRepresentation(representation)));
    }

    @Operation(summary = "Patch a fine-granular DPP element",
            description = "Updates one DataElement in compressed form by default or expanded form when representation=full. The resulting complete DPP is validated before persistence. Optional information can be removed using JSON Merge Patch nulls. Unsupported RFC JSONPath features return a bodyless 501.",
            tags = "DPP Repository - Fine Granular API")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "The JSON value follows the selected representation: compressed by default, or an expanded DataElement object when representation=full. Merge Patch may amend or remove optional information; the resulting element and DPP must validate.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema,
                    examples = {
                            @ExampleObject(name = "Default compressed value", value = DATA_ELEMENT_COMPRESSED_UPDATE_EXAMPLE),
                            @ExampleObject(name = "Expanded DataElement (set representation=full)", value = DATA_ELEMENT_UPDATE_EXAMPLE)
                    })
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Element updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "Default compressed representation", value = ELEMENT_UPDATE_COMPRESSED_SUCCESS_EXAMPLE),
                                    @ExampleObject(name = "Full representation", value = ELEMENT_UPDATE_FULL_SUCCESS_EXAMPLE)
                            })),
            @ApiResponse(responseCode = "400", description = "Malformed elementIdPath, root replacement, or invalid payload",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INVALID_ELEMENT_PATH_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "DPP or selected data element not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = ELEMENT_NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "501", description = "Valid RFC 9535 feature outside the bounded singular subset", content = @Content),
            @ApiResponse(responseCode = "405", description = "HTTP method is not allowed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = METHOD_NOT_ALLOWED_EXAMPLE))),
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
            @Parameter(description = "DPP ID to update. Use the seeded example or the ID returned by `POST /v1/dpps`.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId,
            @Parameter(description = "Bounded RFC 9535-compatible singular JSONPath over the canonical DPP JSON document.",
                    example = "$.elements[1].elements[7]")
            @PathVariable String elementIdPath,
            @Parameter(description = "Request/response representation. Omission means compressed.",
                    schema = @Schema(allowableValues = {"compressed", "full"}, defaultValue = "compressed"))
            @RequestParam(value = "representation", required = false) String representation,
            @RequestBody JsonNode dataElement
    ) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.updateDataElement(dppId, elementIdPath, dataElement,
                        parseRepresentation(representation)));
    }

    @Operation(summary = "Read DPP lifecycle events",
            description = "Returns lifecycle events recorded by the local demo repository for the requested DPP.",
            tags = "DPP Repository - Internal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lifecycle events returned", useReturnTypeSchema = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
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
            @Parameter(description = "DPP ID whose lifecycle events should be returned.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.readEvents(dppId));
    }

    @Operation(summary = "Delete an active DPP",
            description = "Soft-deletes the active DPP while preserving lifecycle history and historical snapshots.",
            tags = "DPP Repository - Life Cycle API")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "DPP soft-deleted; no response body", content = @Content),
            @ApiResponse(responseCode = "404", description = "DPP not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = NOT_FOUND_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Concurrent DPP version conflict",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = VERSION_CONFLICT_EXAMPLE))),
            @ApiResponse(responseCode = "405", description = "HTTP method is not allowed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = METHOD_NOT_ALLOWED_EXAMPLE))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = INTERNAL_ERROR_EXAMPLE)))
    })
    @DeleteMapping(API_PREFIX + "/dpps/{dppId}")
    ResponseEntity<DppApiResponse<Void>> delete(
            @Parameter(description = "DPP ID to soft-delete. This example is seeded only for DELETE so the read/query examples stay queryable.",
                    example = PostmanSeedData.DELETE_EXAMPLE_DPP_ID)
            @PathVariable String dppId) {
        // Deletes are soft deletes so lifecycle history and historical snapshots remain queryable.
        repoService.deleteById(dppId);
        return responseFactory.successNoContent();
    }

    @Operation(summary = "Repository health check",
            description = "Simple local demo-service health check.",
            tags = "DPP Repository - Internal")
    @GetMapping("/health")
    HealthPayload health() {
        return new HealthPayload("UP", "dpp-repo-api");
    }

    private DppRepresentation parseRepresentation(String value) {
        try {
            return DppRepresentation.fromQuery(value);
        } catch (IllegalArgumentException exception) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest,
                    "INVALID_REPRESENTATION", exception.getMessage());
        }
    }
}
