package dpp.repo.payloads;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FinalResultMessageContractTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void emitsSingularMessageAndAcceptsLegacyMessages() throws Exception {
        DppApiResponse<Void> response = new DppApiResponse<>();
        DppApiMessage message = new DppApiMessage(MessageType.Error, "invalid");
        response.setMessage(List.of(message));
        String wire = json.writeValueAsString(response);
        assertTrue(json.readTree(wire).has("message"));
        assertFalse(json.readTree(wire).has("messages"));
        var legacy = json.readValue("{\"messages\":[{\"messageType\":\"Error\",\"text\":\"old\"}]}",
                DppApiResponse.class);
        assertEquals("old", ((DppApiMessage) legacy.getMessage().get(0)).getText());
    }

    @Test
    void onlyOmittedRepresentationDefaultsToCompressed() {
        assertEquals(DppRepresentation.COMPRESSED, DppRepresentation.fromQuery(null));
        assertEquals(DppRepresentation.COMPRESSED, DppRepresentation.fromQuery("compressed"));
        assertEquals(DppRepresentation.FULL, DppRepresentation.fromQuery("full"));
        assertThrows(IllegalArgumentException.class, () -> DppRepresentation.fromQuery(""));
    }

    @Test
    void mapsEveryFinalMessageFieldAndCanonicalMessageArray() throws Exception {
        DppApiResponse<Void> response = new DppApiResponse<>();
        response.setStatusCode(DppStatusCode.ClientErrorBadRequest);
        DppApiMessage message = new DppApiMessage(MessageType.Error, "invalid field");
        message.setCode("BAD_FIELD");
        message.setCorrelationId("req-17");
        response.setMessage(List.of(message));

        String wire = json.writeValueAsString(response);
        DppApiResponse<?> parsed = json.readValue(wire, DppApiResponse.class);
        assertEquals(DppStatusCode.ClientErrorBadRequest, parsed.getStatusCode());
        assertEquals(1, parsed.getMessage().size());
        DppApiMessage mapped = parsed.getMessage().get(0);
        assertEquals(MessageType.Error, mapped.getMessageType());
        assertEquals("invalid field", mapped.getText());
        assertEquals("BAD_FIELD", mapped.getCode());
        assertEquals("req-17", mapped.getCorrelationId());
        var emitted = json.readTree(wire);
        assertTrue(emitted.has("message"));
        assertFalse(emitted.has("messages"));
    }

    @Test
    void rejectsMessagesWithoutRequiredFieldsButAllowsBlankTextAndNoMessages() throws Exception {
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class, () -> json.readValue(
                "{\"text\":\"details\"}", DppApiMessage.class));
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class, () -> json.readValue(
                "{\"messageType\":\"Error\"}", DppApiMessage.class));
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class, () -> json.readValue(
                "{\"messageType\":null,\"text\":\"details\"}", DppApiMessage.class));
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class, () -> json.readValue(
                "{\"messageType\":\"Error\",\"text\":null}", DppApiMessage.class));

        DppApiMessage blankText = json.readValue(
                "{\"messageType\":\"Info\",\"text\":\"\"}", DppApiMessage.class);
        assertEquals("", blankText.getText());
        assertNotNull(json.readValue("{}", DppApiResponse.class));
        assertEquals(0, ((DppApiResponse<?>) json.readValue(
                "{\"message\":[]}", DppApiResponse.class)).getMessage().size());
        assertThrows(NullPointerException.class, () -> new DppApiMessage(null, "details"));
        assertThrows(NullPointerException.class, () -> new DppApiMessage(MessageType.Info, null));
    }

    @Test
    void responseMessageArrayRejectsNullItemsButKeepsTheCollectionOptional() throws Exception {
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class, () -> json.readValue(
                "{\"statusCode\":\"Success\",\"message\":[null]}", DppApiResponse.class));

        assertNull(json.readValue("{\"statusCode\":\"Success\"}", DppApiResponse.class).getMessage());
        assertNull(json.readValue("{\"statusCode\":\"Success\",\"message\":null}",
                DppApiResponse.class).getMessage());
        assertTrue(json.readValue("{\"statusCode\":\"Success\",\"message\":[]}",
                DppApiResponse.class).getMessage().isEmpty());
    }
}
