package dpp.repo.payloads;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepoPayloadContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void wrapperAndMessageDtosRoundTripWithContractFieldNames() throws Exception {
        String json = """
                {
                  "statusCode":"Success",
                  "payload":{"dppId":"dpp-1"},
                  "messages":[
                    {
                      "messageType":"Error",
                      "text":"duplicate",
                      "code":"repo-conflict",
                      "correlationId":"corr-1"
                    }
                  ]
                }
                """;

        DppApiResponse<CreateDppResponse> response = objectMapper.readValue(
                json,
                new TypeReference<DppApiResponse<CreateDppResponse>>() {
                }
        );

        assertEquals(DppStatusCode.Success, response.getStatusCode());
        assertEquals("dpp-1", response.getPayload().getDigitalProductPassportId());
        assertEquals("dpp-1", response.getPayload().getDigitalProductPassportId());
        assertEquals(MessageType.Error, response.getMessage().get(0).getMessageType());
        assertEquals("repo-conflict", response.getMessage().get(0).getCode());

        String written = objectMapper.writeValueAsString(response);
        JsonNode writtenJson = objectMapper.readTree(written);

        assertEquals("Success", writtenJson.get("statusCode").textValue());
        assertEquals("Error", writtenJson.get("message").get(0).get("messageType").textValue());
        assertEquals("dpp-1", writtenJson.get("payload").get("digitalProductPassportId").textValue());
        assertEquals(false, writtenJson.get("payload").has("dppId"));
        assertEquals(false, writtenJson.has("messages"));
    }

    @Test
    void repoStatusCodeAliasesDeserializeToCurrentEnumValues() throws Exception {
        assertEquals(DppStatusCode.ClientNotAuthorized, objectMapper.readValue("\"ClientErrorNotAuthorized\"", DppStatusCode.class));
        assertEquals(DppStatusCode.ClientForbidden, objectMapper.readValue("\"ClientErrorForbidden\"", DppStatusCode.class));
        assertEquals(DppStatusCode.ServerNotImplemented, objectMapper.readValue("\"ServerNotImplemented\"", DppStatusCode.class));
        assertEquals("\"SuccessCreated\"", objectMapper.writeValueAsString(DppStatusCode.SuccessCreated));
    }

    @Test
    void requestAndResponseDtosSerializeWithCurrentContractShape() throws Exception {
        ReadDppIdsRequest request = new ReadDppIdsRequest(List.of("product-1", "product-2"), 2, "cursor-1");
        ReadDppIdsResponse response = new ReadDppIdsResponse();
        response.setDppIdentifiers(List.of("dpp-1", "dpp-2"));
        response.setNextCursor("cursor-2");
        DeleteDppResponse deleteResponse = new DeleteDppResponse(DppStatusCode.SuccessNoContent, List.of());

        assertEquals(
                "{\"productIdentifiers\":[\"product-1\",\"product-2\"],\"limit\":2,\"cursor\":\"cursor-1\"}",
                objectMapper.writeValueAsString(request)
        );
        assertEquals(
                "{\"dppIdentifiers\":[\"dpp-1\",\"dpp-2\"],\"nextCursor\":\"cursor-2\"}",
                objectMapper.writeValueAsString(response)
        );
        assertEquals(
                "{\"statusCode\":\"SuccessNoContent\",\"message\":[]}",
                objectMapper.writeValueAsString(deleteResponse)
        );
    }
}
