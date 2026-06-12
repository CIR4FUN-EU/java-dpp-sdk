package demo.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class DppElementPathServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DppElementPathService service = new DppElementPathService();

    @Test
    void readsNestedAndAliasedPaths() throws Exception {
        ObjectNode root = (ObjectNode) objectMapper.readTree("""
                {
                  "passportMetadata": {
                    "uniqueProductIdentifier": "dpp-123",
                    "externalDocumentationLink": "https://demo.example/docs"
                  },
                  "nameplate": {
                    "gtinCode": "product-123"
                  },
                  "billOfMaterials": {
                    "materials": [
                      {
                        "name": "Wood"
                      }
                    ]
                  }
                }
                """);

        assertEquals("dpp-123", service.read(root, "coreDpp.passportMetadata.uniqueProductIdentifier").asText());
        assertEquals("Wood", service.read(root, "billOfMaterials.materials[0].name").asText());
    }

    @Test
    void rejectsUnsupportedPaths() throws Exception {
        ObjectNode root = (ObjectNode) objectMapper.readTree("{\"characteristics\":{\"productName\":\"Bed\"}}");

        assertThrows(RepoApiException.class, () -> service.read(root, "characteristics.unknown"));
    }
}
