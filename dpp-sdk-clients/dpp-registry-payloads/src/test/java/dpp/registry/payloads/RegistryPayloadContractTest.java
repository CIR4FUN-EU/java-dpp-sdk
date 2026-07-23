package dpp.registry.payloads;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistryPayloadContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerRequestAndResponseRoundTripWithEn18222FieldNames() throws Exception {
        RegisterDppRequest request = new RegisterDppRequest("product-1", "dpp-1", "operator-1", "http://localhost:8082");

        String written = objectMapper.writeValueAsString(request);
        JsonNode writtenJson = objectMapper.readTree(written);

        assertEquals("product-1", writtenJson.get("uniqueProductIdentifier").textValue());
        assertEquals("dpp-1", writtenJson.get("digitalProductPassportId").textValue());
        assertEquals("operator-1", writtenJson.get("uniqueEconomicOperatorIdentifier").textValue());
        assertEquals("http://localhost:8082", writtenJson.get("dppApiEndpoint").textValue());

        RegisterDppResponse response = objectMapper.readValue(
                "{\"registrationId\":\"registry-1\"}",
                RegisterDppResponse.class
        );

        assertEquals("registry-1", response.getRegistrationId());
    }

    @Test
    void registryWrapperAndEnumsUseContractJsonSpelling() throws Exception {
        String json = """
                {
                  "statusCode":"SuccessCreated",
                  "payload":{"registrationId":"registry-1"},
                  "messages":[
                    {
                      "messageType":"Info",
                      "text":"created",
                      "code":"registry-created",
                      "correlationId":"corr-2"
                    }
                  ]
                }
                """;

        DppApiResponse<RegisterDppResponse> response = objectMapper.readValue(
                json,
                new TypeReference<DppApiResponse<RegisterDppResponse>>() {
                }
        );

        assertEquals(DppStatusCode.SuccessCreated, response.getStatusCode());
        assertEquals("registry-1", response.getPayload().getRegistrationId());
        assertEquals(MessageType.Info, response.getMessages().get(0).getMessageType());
        assertEquals("\"ServerNotImplemented\"", objectMapper.writeValueAsString(DppStatusCode.ServerNotImplemented));
        assertEquals("\"SuccessNoContent\"", objectMapper.writeValueAsString(DppStatusCode.SuccessNoContent));
        assertEquals("\"Info\"", objectMapper.writeValueAsString(MessageType.Info));
    }

    @Test
    void registryStatusCodeAliasesDeserializeToCurrentEnumValues() throws Exception {
        assertEquals(DppStatusCode.ClientNotAuthorized, objectMapper.readValue("\"ClientErrorNotAuthorized\"", DppStatusCode.class));
        assertEquals(DppStatusCode.ClientForbidden, objectMapper.readValue("\"ClientErrorForbidden\"", DppStatusCode.class));
    }

    @Test
    void registryResponseAcceptsLegacyFieldNameDuringTransition() throws Exception {
        RegisterDppResponse response = objectMapper.readValue(
                "{\"registryIdentifier\":\"registry-1\"}",
                RegisterDppResponse.class
        );

        assertEquals("registry-1", response.getRegistrationId());
    }
}
