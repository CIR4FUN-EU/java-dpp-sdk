package demo.producer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ConsoleSupportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jsonValuePrintsNestedPayloadAsIndentedJson() throws Exception {
        JsonNode payload = objectMapper.readTree(
                "{\"elements\":[{\"elementId\":\"brand\",\"value\":\"Cir4Fun\"},"
                        + "{\"elementId\":\"color\",\"value\":\"Graphite\"}]}"
        );

        String output = captureStandardOut(() -> ConsoleSupport.jsonValue("FULL representation", payload));

        assertTrue(output.contains("FULL representation"));
        assertTrue(output.contains("\n  \"elements\" : [\n    {"));
        assertTrue(output.contains("\n    },\n    {"));
        assertTrue(output.contains("\n      \"value\" : \"Cir4Fun\""));
    }

    @Test
    void compressedDppPrintsTheCompleteCompressedTree() throws Exception {
        JsonNode payload = objectMapper.readTree(
                "{\"digitalProductPassportId\":\"id-1\",\"nameplate\":{\"brand\":\"Cir4Fun\"},"
                        + "\"characteristics\":{\"color\":\"Graphite\"}}"
        );

        String output = captureStandardOut(() -> ConsoleSupport.compressedDpp(payload));

        assertTrue(output.contains("COMPRESSED"));
        assertTrue(output.contains("\n  \"nameplate\" : {"));
        assertTrue(output.contains("\n  \"characteristics\" : {"));
    }

    @Test
    void jsonPathValueIndentsObjectValuesAndPreservesScalarValues() throws Exception {
        JsonNode objectValue = objectMapper.readTree("{\"elementId\":\"brand\",\"value\":\"Cir4Fun\"}");
        JsonNode scalarValue = objectMapper.readTree("\"FSC certified wood\"");

        String output = captureStandardOut(() -> {
            ConsoleSupport.jsonPathValue("brand", "$.elements[1].elements[5]", objectValue);
            ConsoleSupport.jsonPathValue("material name", "$.elements[0].elements[0].value[0]", scalarValue);
        });

        assertTrue(output.contains("$.elements[1].elements[5]"));
        assertTrue(output.contains("{\n  \"elementId\" : \"brand\","));
        assertTrue(output.contains("$.elements[0].elements[0].value[0]"));
        assertTrue(output.contains("\"FSC certified wood\""));
    }

    private String captureStandardOut(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream replacement = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(replacement);
            action.run();
        } finally {
            System.setOut(original);
        }
        return captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
