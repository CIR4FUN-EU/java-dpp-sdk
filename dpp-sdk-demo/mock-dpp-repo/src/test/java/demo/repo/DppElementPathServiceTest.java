package demo.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dpp.repo.payloads.DppStatusCode;
import org.junit.jupiter.api.Test;

class DppElementPathServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DppElementPathService service = new DppElementPathService();

    @Test
    void readsRootDotBracketAndArraySelectorsFromCanonicalTransportJson() throws Exception {
        ObjectNode root = sampleDpp();

        assertEquals(root, service.read(root, "$"));
        assertEquals("dpp-123", service.read(root, "$.passportMetadata.uniqueProductIdentifier").asText());
        assertEquals("product-123", service.read(root, "$['nameplate'][\"gtinCode\"]").asText());
        assertEquals("Wood", service.read(root, "$.billOfMaterials.materials[0].name").asText());
    }

    @Test
    void returnsNotFoundForSupportedPathsWithNoMatchingNode() throws Exception {
        ObjectNode root = sampleDpp();

        assertStatus(DppStatusCode.ClientErrorResourceNotFound,
                () -> service.read(root, "$.characteristics.missing"));
        assertStatus(DppStatusCode.ClientErrorResourceNotFound,
                () -> service.read(root, "$.billOfMaterials.materials[4]"));
        assertStatus(DppStatusCode.ClientErrorResourceNotFound,
                () -> service.read(root, "$.characteristics.productName.value"));
    }

    @Test
    void distinguishesMalformedPathsFromValidUnsupportedRfc9535Features() throws Exception {
        ObjectNode root = sampleDpp();

        assertStatus(DppStatusCode.ClientErrorBadRequest,
                () -> service.read(root, "characteristics.productName"));
        assertStatus(DppStatusCode.ClientErrorBadRequest,
                () -> service.read(root, "$.billOfMaterials.materials[01]"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$.billOfMaterials.materials[*]"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$..productName"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$.billOfMaterials.materials[-1]"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$.billOfMaterials.materials[0,1]"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$.billOfMaterials.materials[0:1]"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$.billOfMaterials.materials[?@.name]"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$..['productName']"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$['nameplate','characteristics']"));
    }

    @Test
    void enforcesRfc9535QuotedMemberEscapeGrammar() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("quote\"member", "double");
        root.put("astral🁁member", "surrogate-pair");

        assertEquals("double", service.read(root, "$['quote\"member']").asText());
        assertEquals("surrogate-pair", service.read(root, "$['astral\\uD83C\\uDC41member']").asText());
        assertStatus(DppStatusCode.ClientErrorBadRequest,
                () -> service.read(root, "$['quote\\\"member']"));
        assertStatus(DppStatusCode.ClientErrorBadRequest,
                () -> service.read(root, "$['lone\\uD800surrogate']"));
    }

    @Test
    void rejectsExcessivePathLengthAndDepth() throws Exception {
        ObjectNode root = sampleDpp();

        assertStatus(DppStatusCode.ClientErrorBadRequest,
                () -> service.read(root, "$" + ".member".repeat(65)));
        assertStatus(DppStatusCode.ClientErrorBadRequest,
                () -> service.read(root, "$['" + "a".repeat(4_100) + "']"));
    }

    @Test
    void rejectsRootReplacementAndLeavesTheTreeUntouched() throws Exception {
        ObjectNode root = sampleDpp();

        assertStatus(DppStatusCode.ClientErrorBadRequest,
                () -> service.update(root, "$", objectMapper.readTree("{\"replacement\":true}")));

        assertEquals("Bed", root.path("characteristics").path("productName").asText());
    }

    private ObjectNode sampleDpp() throws Exception {
        return (ObjectNode) objectMapper.readTree("""
                {
                  "passportMetadata": {"uniqueProductIdentifier": "dpp-123"},
                  "nameplate": {"gtinCode": "product-123"},
                  "characteristics": {"productName": "Bed"},
                  "billOfMaterials": {"materials": [{"name": "Wood"}]}
                }
                """);
    }

    private static void assertStatus(DppStatusCode expected, ThrowingRunnable action) {
        RepoApiException exception = assertThrows(RepoApiException.class, action::run);
        assertEquals(expected, exception.statusCode());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        JsonNode run() throws Exception;
    }
}
