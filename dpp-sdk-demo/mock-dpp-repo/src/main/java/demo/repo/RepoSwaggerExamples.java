package demo.repo;

final class RepoSwaggerExamples {

    static final String COMPRESSED_SUCCESS_EXAMPLE = """
            {
              "statusCode": "Success",
              "payload": {
                "representation": "compressed",
                "dppId": "49192c87-20c8-4b6f-88de-48b56ca4c211",
                "productId": "04012345678901",
                "productName": "Cir4Fun Platform Bed",
                "productCategory": "Beds",
                "manufacturerName": "Cir4Fun Furniture GmbH",
                "availableRepresentations": ["full", "compressed"]
              },
              "messages": []
            }
            """;
    static final String FULL_SUCCESS_EXAMPLE = """
            {
              "statusCode": "Success",
              "payload": {
                "passportMetadata": {
                  "uniqueProductIdentifier": "49192c87-20c8-4b6f-88de-48b56ca4c211"
                },
                "nameplate": {
                  "gtinCode": "04012345678901"
                },
                "characteristics": {
                  "productName": "Cir4Fun Platform Bed"
                }
              },
              "messages": []
            }
            """;
    static final String ERROR_EXAMPLE = """
            {
              "statusCode": "ClientErrorBadRequest",
              "payload": null,
              "messages": [
                {
                  "messageType": "Error",
                  "code": "MALFORMED_JSON",
                  "text": "Malformed JSON payload",
                  "correlationId": "demo-correlation-id",
                  "timestamp": "2026-07-13T10:00:00Z"
                }
              ]
            }
            """;
    static final String NOT_FOUND_EXAMPLE = """
            {
              "statusCode": "ClientErrorResourceNotFound",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "DPP_NOT_FOUND",
                "text": "No DPP found for the supplied id",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String PRODUCT_NOT_FOUND_EXAMPLE = """
            {
              "statusCode": "ClientErrorResourceNotFound",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "PRODUCT_NOT_FOUND",
                "text": "No active DPP found for the supplied product id",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String DPP_VERSION_NOT_FOUND_EXAMPLE = """
            {
              "statusCode": "ClientErrorResourceNotFound",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "DPP_VERSION_NOT_FOUND",
                "text": "No DPP version found for the supplied id and instant",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String INVALID_REPRESENTATION_EXAMPLE = """
            {
              "statusCode": "ClientErrorBadRequest",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "INVALID_REPRESENTATION",
                "text": "representation must be full or compressed",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String INVALID_DATE_EXAMPLE = """
            {
              "statusCode": "ClientErrorBadRequest",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "INVALID_DATE",
                "text": "Invalid UTC timestamp",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String EMPTY_PRODUCT_IDENTIFIERS_EXAMPLE = """
            {
              "statusCode": "ClientErrorBadRequest",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "EMPTY_PRODUCT_IDENTIFIERS",
                "text": "productIdentifiers must not be empty",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String DPP_CONFLICT_EXAMPLE = """
            {
              "statusCode": "ClientResourceConflict",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "DPP_CONFLICT",
                "text": "A DPP with the supplied id already exists",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String INVALID_PATCH_EXAMPLE = """
            {
              "statusCode": "ClientErrorBadRequest",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "INVALID_PATCH",
                "text": "Patch must be a JSON object",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String VERSION_CONFLICT_EXAMPLE = """
            {
              "statusCode": "ClientResourceConflict",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "DPP_VERSION_CONFLICT",
                "text": "The DPP was modified concurrently",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String INVALID_ELEMENT_PATH_EXAMPLE = """
            {
              "statusCode": "ClientErrorBadRequest",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "INVALID_ELEMENT_ID_PATH",
                "text": "elementIdPath must start with $",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String ELEMENT_NOT_FOUND_EXAMPLE = """
            {
              "statusCode": "ClientErrorResourceNotFound",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "ELEMENT_NOT_FOUND",
                "text": "No element found for the supplied path",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String NOT_IMPLEMENTED_EXAMPLE = """
            {
              "statusCode": "ServerNotImplemented",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "JSONPATH_FEATURE_NOT_IMPLEMENTED",
                "text": "The requested JSONPath feature is outside the supported subset",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String INTERNAL_ERROR_EXAMPLE = """
            {
              "statusCode": "ServerInternalError",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "UNEXPECTED_ERROR",
                "text": "Unexpected repository error",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String CREATE_DPP_EXAMPLE = """
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
    static final String READ_IDS_EXAMPLE = """
            {
              "productIdentifiers": ["04012345678901"],
              "limit": 10,
              "cursor": "0"
            }
            """;
    static final String MERGE_PATCH_EXAMPLE = """
            {
              "characteristics": {
                "productName": "Cir4Fun Platform Bed - Updated Demo"
              },
              "documentation": {
                "safetyInstructionsLink": null
              }
            }
            """;
    static final String CREATE_DPP_SUCCESS_EXAMPLE = """
            {
              "statusCode": "SuccessCreated",
              "payload": {
                "dppId": "22222222-2222-2222-2222-222222222222"
              },
              "messages": []
            }
            """;
    static final String HISTORICAL_COMPRESSED_SUCCESS_EXAMPLE = """
            {
              "statusCode": "Success",
              "payload": {
                "representation": "compressed",
                "dppId": "77777777-7777-7777-7777-777777777777",
                "productId": "04012345678992",
                "productName": "Cir4Fun Historical Swagger Desk",
                "productCategory": "Desks",
                "manufacturerName": "Cir4Fun Furniture GmbH",
                "availableRepresentations": ["full", "compressed"]
              },
              "messages": []
            }
            """;
    static final String READ_IDS_SUCCESS_EXAMPLE = """
            {
              "statusCode": "Success",
              "payload": {
                "dppIdentifiers": ["49192c87-20c8-4b6f-88de-48b56ca4c211"],
                "nextCursor": null
              },
              "messages": []
            }
            """;
    static final String ELEMENT_READ_SUCCESS_EXAMPLE = """
            {
              "statusCode": "Success",
              "payload": "Cir4Fun Platform Bed",
              "messages": []
            }
            """;
    static final String ELEMENT_UPDATE_SUCCESS_EXAMPLE = """
            {
              "statusCode": "Success",
              "payload": "Cir4Fun Platform Bed - Fine Granular Update",
              "messages": []
            }
            """;
    static final String EVENTS_SUCCESS_EXAMPLE = """
            {
              "statusCode": "Success",
              "payload": [{
                "eventId": "demo-event-id",
                "dppId": "49192c87-20c8-4b6f-88de-48b56ca4c211",
                "eventType": "DPP_CREATED",
                "occurredAt": "2026-07-13T10:00:00Z",
                "data": {"productId": "04012345678901"}
              }],
              "messages": []
            }
            """;
    static final String DELETE_SUCCESS_EXAMPLE = """
            {
              "statusCode": "SuccessNoContent",
              "payload": null,
              "messages": []
            }
            """;
    static final String DATA_ELEMENT_UPDATE_EXAMPLE = """
            "Cir4Fun Platform Bed - Fine Granular Update"
            """;

    private RepoSwaggerExamples() {
    }
}
