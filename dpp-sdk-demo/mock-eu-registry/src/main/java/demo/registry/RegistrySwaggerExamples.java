package demo.registry;

final class RegistrySwaggerExamples {

    static final String REGISTER_DPP_EXAMPLE = """
            {
              "uniqueProductIdentifier": "04012345678901",
              "digitalProductPassportId": "49192c87-20c8-4b6f-88de-48b56ca4c211",
              "uniqueEconomicOperatorIdentifier": "operator-123",
              "dppApiEndpoint": "http://localhost:8080"
            }
            """;
    static final String WRAPPED_SUCCESS_EXAMPLE = """
            {
              "statusCode": "SuccessCreated",
              "payload": {
                "registrationId": "8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc"
              },
              "messages": []
            }
            """;
    static final String INTERNAL_LOOKUP_SUCCESS_EXAMPLE = """
            {
              "statusCode": "Success",
              "payload": {
                "registryIdentifier": "8a5be5de-7c76-46ef-a1d5-4875d3f4a5dc",
                "dppIdentifier": "e7d64b7b-18f2-4d77-9c41-2fa1d1d6b8aa",
                "productIdentifier": "04012345678902",
                "operatorIdentifier": "operator-123",
                "repoUrl": "http://localhost:8080",
                "registeredAt": "2026-05-19T00:00:00Z",
                "lastUpdatedAt": "2026-05-19T00:00:00Z"
              },
              "messages": []
            }
            """;
    static final String BAD_REQUEST_EXAMPLE = """
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
    static final String REPO_DPP_NOT_FOUND_EXAMPLE = """
            {
              "statusCode": "ClientErrorResourceNotFound",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "REPO_DPP_NOT_FOUND",
                "text": "Referenced DPP was not found in the repository",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String REGISTRY_NOT_FOUND_EXAMPLE = """
            {
              "statusCode": "ClientErrorResourceNotFound",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "REGISTRY_NOT_FOUND",
                "text": "No registry record was found",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String CONFLICT_EXAMPLE = """
            {
              "statusCode": "ClientResourceConflict",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "REGISTRY_CONFLICT",
                "text": "A registry record already exists for the DPP id",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;
    static final String BAD_GATEWAY_EXAMPLE = """
            {
              "statusCode": "ServerErrorBadGateway",
              "payload": null,
              "messages": [{
                "messageType": "Error",
                "code": "REPO_VERIFICATION_FAILED",
                "text": "Repository verification failed",
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
                "text": "Unexpected registry error",
                "correlationId": "demo-correlation-id",
                "timestamp": "2026-07-13T10:00:00Z"
              }]
            }
            """;

    private RegistrySwaggerExamples() {
    }
}
