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
        assertEquals("dpp-123", service.read(root, "$.digitalProductPassportId").asText());
        assertEquals("product-123", service.read(root, "$.elements[0][\"elements\"][0].value").asText());
        assertEquals("Bed", service.read(root, "$.elements[1].elements[0].value").asText());
        assertEquals("Wood", service.read(root, "$.elements[2].elements[0].elements[0].elements[0].value").asText());
    }

    @Test
    void returnsNotFoundForSupportedPathsWithNoMatchingNode() throws Exception {
        ObjectNode root = sampleDpp();

        assertStatus(DppStatusCode.ClientErrorResourceNotFound,
                () -> service.read(root, "$.elements[1].elements[4]"));
        assertStatus(DppStatusCode.ClientErrorResourceNotFound,
                () -> service.read(root, "$.elements[2].elements[0].elements[4]"));
        assertStatus(DppStatusCode.ClientErrorResourceNotFound,
                () -> service.read(root, "$.elements[1].elements[0].value.value"));
    }

    @Test
    void distinguishesMalformedPathsFromValidUnsupportedRfc9535Features() throws Exception {
        ObjectNode root = sampleDpp();

        assertStatus(DppStatusCode.ClientErrorBadRequest,
                () -> service.read(root, "elements[1].elements[0].value"));
        assertStatus(DppStatusCode.ClientErrorBadRequest,
                () -> service.read(root, "$.elements[2].elements[0].elements[01]"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$.elements[2].elements[0].elements[*]"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$..productName"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$.elements[2].elements[0].elements[-1]"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$.elements[2].elements[0].elements[0,1]"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$.elements[2].elements[0].elements[0:1]"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$.elements[2].elements[0].elements[?@.name]"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$..['value']"));
        assertStatus(DppStatusCode.ServerNotImplemented,
                () -> service.read(root, "$['digitalProductPassportId','uniqueProductIdentifier']"));
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

        assertEquals("Bed", root.path("elements").path(1).path("elements").path(0).path("value").asText());
    }

    private ObjectNode sampleDpp() throws Exception {
        return (ObjectNode) objectMapper.readTree("""
                {
                  "digitalProductPassportId": "dpp-123",
                  "uniqueProductIdentifier": "product-123",
                  "granularity": "model",
                  "dppSchemaVersion": "1.0",
                  "elements": [
                    {"elementId":"nameplate","objectType":"DataElementCollection","elements":[
                      {"elementId":"gtinCode","objectType":"SingleValuedDataElement","value":"product-123"}
                    ]},
                    {"elementId":"characteristics","objectType":"DataElementCollection","elements":[
                      {"elementId":"brand","objectType":"SingleValuedDataElement","value":"Bed"}
                    ]},
                    {"elementId":"billOfMaterials","objectType":"DataElementCollection","elements":[
                      {"elementId":"materials","objectType":"MultiValuedDataElement","elements":[
                        {"elementId":"material-1","objectType":"DataElementCollection","elements":[
                          {"elementId":"name","objectType":"SingleValuedDataElement","value":"Wood"}
                        ]}
                      ]}
                    ]}
                  ]
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
