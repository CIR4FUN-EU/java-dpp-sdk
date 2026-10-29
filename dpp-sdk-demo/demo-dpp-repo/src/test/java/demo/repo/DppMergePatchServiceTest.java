package demo.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class DppMergePatchServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DppMergePatchService service = new DppMergePatchService();

    @Test
    void mergesObjectsAndRemovesNullFields() throws Exception {
        ObjectNode target = (ObjectNode) objectMapper.readTree("""
                {
                  "documentation": {
                    "digitalInstructionsLink": "https://demo.example/docs/assembly",
                    "safetyInstructionsLink": "https://demo.example/docs/safety"
                  }
                }
                """);
        ObjectNode patch = (ObjectNode) objectMapper.readTree("""
                {
                  "documentation": {
                    "safetyInstructionsLink": null
                  }
                }
                """);

        ObjectNode merged = (ObjectNode) service.merge(target, patch);

        assertEquals("https://demo.example/docs/assembly",
                merged.get("documentation").get("digitalInstructionsLink").asText());
        assertFalse(merged.get("documentation").has("safetyInstructionsLink"));
    }

    @Test
    void rejectsNonObjectPatchRoots() throws Exception {
        ObjectNode target = (ObjectNode) objectMapper.readTree("{\"a\":1}");

        assertThrows(RepoApiException.class, () -> service.merge(target, objectMapper.readTree("\"bad\"")));
    }
}
