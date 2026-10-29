package demo.repo;

/** Neutral EN JSON examples for the model-independent repository HTTP API. */
final class RepoSwaggerExamples {
    private static final String FULL_DPP = "{\"" + "digitalProductPassportId\":\"urn:example:dpp:chair-001\","
            + "\"uniqueProductIdentifier\":\"urn:example:product:chair-001\","
            + "\"granularity\":\"model\",\"dppSchemaVersion\":\"1.0\","
            + "\"dppStatus\":\"active\",\"lastUpdated\":\"2026-06-08T10:00:00Z\","
            + "\"economicOperatorId\":\"urn:example:operator:manufacturer\",\"elements\":["
            + "{\"elementId\":\"exampleValue\",\"objectType\":\"SingleValuedDataElement\","
            + "\"valueDataType\":\"xsd:string\",\"value\":\"example\"}]}";

    static final String COMPRESSED_SUCCESS_EXAMPLE = """
            {"statusCode":"Success","payload":{
              "digitalProductPassportId":"urn:example:dpp:chair-001",
              "uniqueProductIdentifier":"urn:example:product:chair-001","granularity":"model",
              "dppSchemaVersion":"1.0","dppStatus":"active","lastUpdated":"2026-06-08T10:00:00Z",
              "economicOperatorId":"urn:example:operator:manufacturer","exampleValue":"example"
            },"message":null}
            """;
    static final String FULL_SUCCESS_EXAMPLE = """
            {"statusCode":"Success","payload":{
              "digitalProductPassportId":"urn:example:dpp:chair-001",
              "uniqueProductIdentifier":"urn:example:product:chair-001","granularity":"model",
              "dppSchemaVersion":"1.0","dppStatus":"active","lastUpdated":"2026-06-08T10:00:00Z",
              "economicOperatorId":"urn:example:operator:manufacturer","elements":[
                {"elementId":"exampleValue","objectType":"SingleValuedDataElement",
                 "valueDataType":"xsd:string","value":"example"}
              ]
            },"message":null}
            """;
    static final String ERROR_EXAMPLE = """
            {"statusCode":"ClientErrorBadRequest","payload":null,"message":[{
              "messageType":"Error","text":"Malformed DPP JSON","code":"MALFORMED_JSON",
              "correlationId":"demo-correlation-id","timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String METHOD_NOT_ALLOWED_EXAMPLE = """
            {"statusCode":"ClientMethodNotAllowed","payload":null,"message":[{
              "messageType":"Error","text":"HTTP method is not allowed for this repository endpoint",
              "code":"METHOD_NOT_ALLOWED","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String NOT_FOUND_EXAMPLE = """
            {"statusCode":"ClientErrorResourceNotFound","payload":null,"message":[{
              "messageType":"Error","text":"No DPP found for the supplied identifier",
              "code":"DPP_NOT_FOUND","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String PRODUCT_NOT_FOUND_EXAMPLE = """
            {"statusCode":"ClientErrorResourceNotFound","payload":null,"message":[{
              "messageType":"Error","text":"No DPP is active for the supplied product identifier",
              "code":"PRODUCT_NOT_FOUND","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String DPP_CONFLICT_EXAMPLE = """
            {"statusCode":"ClientResourceConflict","payload":null,"message":[{
              "messageType":"Error","text":"An active DPP already exists for the product identifier",
              "code":"DPP_CONFLICT","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String VERSION_CONFLICT_EXAMPLE = """
            {"statusCode":"ClientResourceConflict","payload":null,"message":[{
              "messageType":"Error","text":"DPP changed since it was read",
              "code":"DPP_VERSION_CONFLICT","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String DPP_VERSION_NOT_FOUND_EXAMPLE = """
            {"statusCode":"ClientErrorResourceNotFound","payload":null,"message":[{
              "messageType":"Error","text":"No historical DPP version exists at the requested time",
              "code":"DPP_VERSION_NOT_FOUND","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String INVALID_DATE_EXAMPLE = """
            {"statusCode":"ClientErrorBadRequest","payload":null,"message":[{
              "messageType":"Error","text":"date must be a valid UTC date-time",
              "code":"INVALID_DATE","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String INVALID_REPRESENTATION_EXAMPLE = """
            {"statusCode":"ClientErrorBadRequest","payload":null,"message":[{
              "messageType":"Error","text":"representation must be compressed or full",
              "code":"INVALID_REPRESENTATION","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String INVALID_ELEMENT_PATH_EXAMPLE = """
            {"statusCode":"ClientErrorBadRequest","payload":null,"message":[{
              "messageType":"Error","text":"elementIdPath is malformed",
              "code":"INVALID_ELEMENT_PATH","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String ELEMENT_NOT_FOUND_EXAMPLE = """
            {"statusCode":"ClientErrorResourceNotFound","payload":null,"message":[{
              "messageType":"Error","text":"No DataElement matched the supplied element path",
              "code":"ELEMENT_NOT_FOUND","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String INVALID_PATCH_EXAMPLE = """
            {"statusCode":"ClientErrorBadRequest","payload":null,"message":[{
              "messageType":"Error","text":"The merge patch is invalid for this DPP",
              "code":"INVALID_PATCH","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String EMPTY_PRODUCT_IDENTIFIERS_EXAMPLE = """
            {"statusCode":"ClientErrorBadRequest","payload":null,"message":[{
              "messageType":"Error","text":"productIdentifiers must contain at least one identifier",
              "code":"EMPTY_PRODUCT_IDENTIFIERS","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String INTERNAL_ERROR_EXAMPLE = """
            {"statusCode":"ServerInternalError","payload":null,"message":[{
              "messageType":"Error","text":"Unexpected repository error",
              "code":"INTERNAL_ERROR","correlationId":"demo-correlation-id",
              "timestamp":"2026-06-08T10:00:00Z"}]}
            """;
    static final String CREATE_DPP_EXAMPLE = FULL_DPP;
    static final String CREATE_DPP_SUCCESS_EXAMPLE = """
            {"statusCode":"SuccessCreated","payload":{
              "digitalProductPassportId":"urn:example:dpp:chair-001"},"message":null}
            """;
    static final String DELETE_EXAMPLE = """
            {"statusCode":"SuccessNoContent","payload":null,"message":null}
            """;
    static final String READ_IDS_EXAMPLE = """
            {"productIdentifiers":["urn:example:product:chair-001"]}
            """;
    static final String READ_IDS_SUCCESS_EXAMPLE = """
            {"statusCode":"Success","payload":{"dppIdentifiers":["urn:example:dpp:chair-001"],
              "nextCursor":null},"message":null}
            """;
    static final String HISTORICAL_COMPRESSED_SUCCESS_EXAMPLE = """
            {"statusCode":"Success","payload":{
              "digitalProductPassportId":"urn:example:dpp:historical-001",
              "uniqueProductIdentifier":"urn:example:product:historical-001","granularity":"model",
              "dppSchemaVersion":"1.0","dppStatus":"active","lastUpdated":"2026-06-08T10:00:00Z",
              "economicOperatorId":"urn:example:operator:manufacturer","exampleValue":"example"
            },"message":null}
            """;
    static final String ELEMENT_READ_COMPRESSED_SUCCESS_EXAMPLE = """
            {"statusCode":"Success","payload":"Cir4Fun","message":null}
            """;
    static final String ELEMENT_READ_FULL_SUCCESS_EXAMPLE = """
            {"statusCode":"Success","payload":{"elementId":"brand","objectType":"SingleValuedDataElement",
              "valueDataType":"xsd:string","value":"Cir4Fun"},"message":null}
            """;
    static final String ELEMENT_UPDATE_COMPRESSED_SUCCESS_EXAMPLE = """
            {"statusCode":"Success","payload":"Updated Cir4Fun","message":null}
            """;
    static final String ELEMENT_UPDATE_FULL_SUCCESS_EXAMPLE = """
            {"statusCode":"Success","payload":{"elementId":"brand","objectType":"SingleValuedDataElement",
              "valueDataType":"xsd:string","value":"Updated Cir4Fun"},"message":null}
            """;
    static final String DATA_ELEMENT_UPDATE_EXAMPLE = """
            {"elementId":"brand","objectType":"SingleValuedDataElement",
              "valueDataType":"xsd:string","value":"Updated Cir4Fun"}
            """;
    static final String DATA_ELEMENT_COMPRESSED_UPDATE_EXAMPLE = """
            "Updated Cir4Fun"
            """;
    static final String MERGE_PATCH_EXAMPLE = """
            {"elements":[{"elementId":"exampleValue","objectType":"SingleValuedDataElement",
              "valueDataType":"xsd:string","value":"updated"}]}
            """;
    static final String EVENTS_SUCCESS_EXAMPLE = """
            {"statusCode":"Success","payload":[{"eventId":"event-001","dppId":"urn:example:dpp:chair-001",
              "eventType":"DPP_CREATED",
              "occurredAt":"2026-06-08T10:00:00Z"}],"message":null}
            """;

    private RepoSwaggerExamples() { }
}
