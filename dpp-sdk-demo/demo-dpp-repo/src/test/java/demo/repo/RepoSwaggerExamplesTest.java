package demo.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dppsdk.core.transport.json.DppJsonCodec;
import dppsdk.core.validation.DppValidator;
import dppsdk.core.validation.format.DataFormatValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepoSwaggerExamplesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DppJsonCodec codec = new DppJsonCodec();

    @Test
    void createAndFullReadExamplesUseValidCanonicalDppJson() throws Exception {
        JsonNode createExample = objectMapper.readTree(RepoSwaggerExamples.CREATE_DPP_EXAMPLE);
        JsonNode fullReadExample = objectMapper.readTree(RepoSwaggerExamples.FULL_SUCCESS_EXAMPLE).path("payload");

        var created = codec.fromExpandedJson(objectMapper.writeValueAsString(createExample));
        var read = codec.fromExpandedJson(objectMapper.writeValueAsString(fullReadExample));
        assertTrue(new DppValidator().validate(created).isValid());
        assertTrue(new DataFormatValidator().validate(created).isValid());
        assertTrue(new DppValidator().validate(read).isValid());
        assertTrue(new DataFormatValidator().validate(read).isValid());
        assertEquals("urn:example:dpp:chair-001",
                createExample.path("digitalProductPassportId").asText());
        assertEquals("urn:example:dpp:chair-001", read.getDppHeader().getDigitalProductPassportId());
        assertEquals("exampleValue", read.getDataElements().get(0).getElementId());
        assertEquals("example", ((dppsdk.core.model.SingleValuedDataElement) read.getDataElements().get(0)).getValue());
    }
}
