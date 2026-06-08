package demo.repo;

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
import dpp.repo.payloads.UpdateDataElementRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Exposes the standard-style mock repository API.
 *
 * <p>This controller stays thin: it accepts HTTP payloads, delegates lifecycle behavior to the service,
 * and wraps responses in the shared API envelope.</p>
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "DPP Repository - Life Cycle API",
        description = "Mock lifecycle endpoints for full DPP create, read, update, delete, and existence checks.")
class DppRepoController {

    private static final String SEEDED_DPP_ID = "49192c87-20c8-4b6f-88de-48b56ca4c211";
    private static final String SEEDED_PRODUCT_ID = "04012345678901";
    private static final String WRAPPED_SUCCESS_EXAMPLE = """
            {
              "statusCode": "Success",
              "payload": {},
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
    private static final String CREATE_DPP_EXAMPLE = """
            {
              "passportMetadata": {
                "uniqueProductIdentifier": "22222222-2222-2222-2222-222222222222",
                "passportUpdateDates": ["2026-04-24"],
                "qrCodeOrDigitalTag": "https://demo.example/dpp/22222222-2222-2222-2222-222222222222",
                "externalDocumentationLink": "https://demo.example/docs/furniture"
              },
              "nameplate": {
                "gtinCode": "04012345678999",
                "internalArticleNumber": "C4F-DEMO-001",
                "batchNumber": "DEMO-2026-04",
                "customsTariffNumber": "940360",
                "uriOfTheProduct": "https://demo.example/products/c4f-demo-001",
                "manufacturer": {
                  "name": "Cir4Fun Furniture GmbH",
                  "gln": "4000001000005",
                  "uri": "https://demo.example/organizations/cir4fun-furniture-gmbh",
                  "role": "MANUFACTURER"
                },
                "supplier": {
                  "name": "Partner Supplier GmbH",
                  "gln": "4000001000005",
                  "uri": "https://demo.example/organizations/partner-supplier-gmbh",
                  "role": "SUPPLIER"
                }
              },
              "documentation": {
                "digitalInstructionsLink": "https://demo.example/docs/assembly",
                "safetyInstructionsLink": "https://demo.example/docs/safety",
                "downloadable": true,
                "availableForYears": 10,
                "paperCopyAvailableOnRequest": true
              },
              "classification": {
                "sector": "Furniture",
                "group": "Home and office furniture",
                "category": "Beds",
                "tags": ["cir4fun", "demo"]
              },
              "characteristics": {
                "productName": "Cir4Fun Platform Bed",
                "description": "Partner demo product passport",
                "brand": "Cir4Fun",
                "productType": "Bed",
                "dimensions": {
                  "width": 90.0,
                  "height": 80.0,
                  "depth": 120.0,
                  "unit": "cm"
                },
                "weight": 24.5,
                "color": "Warm oak",
                "features": ["repairable", "recyclable"]
              },
              "billOfMaterials": {
                "materials": [
                  {
                    "name": "FSC certified wood",
                    "mandatory": true,
                    "portion": 72.0,
                    "reference": "MAT-WOOD-001"
                  }
                ]
              }
            }
            """;
    private static final String READ_IDS_EXAMPLE = """
            {
              "productIdentifiers": ["04012345678901"],
              "limit": 10,
              "cursor": "0"
            }
            """;
    private static final String MERGE_PATCH_EXAMPLE = """
            {
              "characteristics": {
                "productName": "Cir4Fun Platform Bed - Updated Demo"
              },
              "documentation": {
                "safetyInstructionsLink": null
              }
            }
            """;
    private static final String CREATE_DPP_SUCCESS_EXAMPLE = """
            {
              "statusCode": "SuccessCreated",
              "payload": {
                "dppIdentifier": "22222222-2222-2222-2222-222222222222"
              },
              "messages": []
            }
            """;
    private static final String DATA_ELEMENT_UPDATE_EXAMPLE = """
            {
              "payload": "Cir4Fun Platform Bed - Fine Granular Update"
            }
            """;

    private final DppRepoService repoService;
    private final ApiResponseFactory responseFactory;

    DppRepoController(DppRepoService repoService, ApiResponseFactory responseFactory) {
        this.repoService = repoService;
        this.responseFactory = responseFactory;
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
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @PostMapping(value = "/dpps", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<CreateDppResponse>> create(@RequestBody String jsonPayload) {
        return responseFactory.success(HttpStatus.CREATED, DppStatusCode.SuccessCreated, repoService.create(jsonPayload));
    }

    @Operation(summary = "Read a DPP by id",
            description = "Returns the active full DPP JSON document for the requested DPP identifier.",
            tags = "DPP Repository - Life Cycle API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DPP found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class),
                            examples = @ExampleObject(value = WRAPPED_SUCCESS_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "DPP not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @GetMapping("/dpps/{dppId}")
    ResponseEntity<DppApiResponse<JsonNode>> readById(
            @Parameter(description = "DPP identifier to read. The example is seeded when the mock repo starts.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.readById(dppId));
    }

    @Operation(summary = "Verify active DPP existence",
            description = "HEAD endpoint used by the mock registry to verify that a DPP is active in the mock repository.",
            tags = "DPP Repository - Life Cycle API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DPP exists", content = @Content),
            @ApiResponse(responseCode = "404", description = "DPP not found", content = @Content)
    })
    @RequestMapping(value = "/dpps/{dppId}", method = RequestMethod.HEAD)
    ResponseEntity<Void> verifyById(
            @Parameter(description = "DPP identifier to verify. The example is seeded when the mock repo starts.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId) {
        return repoService.hasActiveDpp(dppId)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Read active DPP by product id",
            description = "Returns the active DPP JSON document for a product identifier.",
            tags = "DPP Repository - Life Cycle API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DPP found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product has no active DPP",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @GetMapping("/dppsByProductId/{productId}")
    ResponseEntity<DppApiResponse<JsonNode>> readByProductId(
            @Parameter(description = "Product identifier, for example a GTIN. The example is seeded when the mock repo starts.",
                    example = SEEDED_PRODUCT_ID)
            @PathVariable String productId) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.readByProductId(productId));
    }

    @Operation(summary = "Read historical DPP snapshot by product id and date",
            description = "Returns the repository snapshot that was active for a product at the requested ISO-8601 instant.",
            tags = "DPP Repository - Life Cycle API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historical snapshot found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date parameter",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "No matching snapshot found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @GetMapping("/dppsByProductIdAndDate/{productId}")
    ResponseEntity<DppApiResponse<JsonNode>> readByProductIdAndDate(
            @Parameter(description = "Product identifier, for example a GTIN. The example is seeded when the mock repo starts.",
                    example = SEEDED_PRODUCT_ID)
            @PathVariable String productId,
            @Parameter(description = "ISO-8601 instant used for historical snapshot lookup.", example = "2026-06-08T10:15:30Z")
            @RequestParam("date") String date
    ) {
        // Snapshot lookup is based on repository history, not on any version fields inside the DPP document.
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.readVersionByProductIdAndDate(productId, date));
    }

    @Operation(summary = "Read DPP identifiers by product identifiers",
            description = "Returns active DPP identifiers for a list of product identifiers, with optional limit/cursor paging. The example product identifier is seeded when the mock repo starts.",
            tags = "DPP Repository - Life Cycle API")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Product identifiers plus optional limit and cursor.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReadDppIdsRequest.class),
                    examples = @ExampleObject(name = "Batch product lookup", value = READ_IDS_EXAMPLE))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DPP identifiers returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request, limit, cursor, or product id list",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @PostMapping(value = "/dppsByProductIds", consumes = MediaType.APPLICATION_JSON_VALUE)
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
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid patch, immutable-field change, or validation failure",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "DPP not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @PatchMapping(value = "/dpps/{dppId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<JsonNode>> update(
            @Parameter(description = "DPP identifier to patch. Use the seeded example or the id returned by `POST /dpps`.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId,
            @RequestBody String patchJson) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success, repoService.updateById(dppId, patchJson));
    }

    @Operation(summary = "Read a fine-granular DPP element",
            description = "Reads one curated element path from the active DPP. Element paths are a supported subset, not arbitrary JSONPath.",
            tags = "DPP Repository - Fine Granular API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Element returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Unsupported element path",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "DPP not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @GetMapping("/dpps/{dppId}/elements/{elementPath}")
    ResponseEntity<DppApiResponse<JsonNode>> readDataElement(
            @Parameter(description = "DPP identifier to read from. The example is seeded when the mock repo starts.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId,
            @Parameter(description = "Supported element path.", example = "characteristics.productName")
            @PathVariable String elementPath
    ) {
        // Element paths are a deliberately small supported subset, not arbitrary JSONPath.
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.readDataElement(dppId, elementPath));
    }

    @Operation(summary = "Patch a fine-granular DPP element",
            description = "Updates one curated element path using a `{ \"payload\": ... }` body and returns the updated element value.",
            tags = "DPP Repository - Fine Granular API")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Element update wrapper. The `payload` value may be a scalar, object, or array accepted for the target element.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UpdateDataElementRequest.class),
                    examples = @ExampleObject(name = "Update product name", value = DATA_ELEMENT_UPDATE_EXAMPLE))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Element updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Unsupported element path or invalid payload",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "DPP not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @PatchMapping(value = "/dpps/{dppId}/elements/{elementPath}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DppApiResponse<JsonNode>> updateDataElement(
            @Parameter(description = "DPP identifier to update. Use the seeded example or the id returned by `POST /dpps`.",
                    example = SEEDED_DPP_ID)
            @PathVariable String dppId,
            @Parameter(description = "Supported element path.", example = "characteristics.productName")
            @PathVariable String elementPath,
            @RequestBody UpdateDataElementRequest request
    ) {
        return responseFactory.success(HttpStatus.OK, DppStatusCode.Success,
                repoService.updateDataElement(dppId, elementPath, request));
    }

    @Operation(summary = "Read DPP lifecycle events",
            description = "Returns lifecycle events recorded by the local mock repository for the requested DPP.",
            tags = "DPP Repository - Events")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lifecycle events returned",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "DPP not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @GetMapping("/dpps/{dppId}/events")
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
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "DPP not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected repository error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DppApiResponse.class)))
    })
    @DeleteMapping("/dpps/{dppId}")
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
        return new HealthPayload("UP", "mock-dpp-repo");
    }
}
