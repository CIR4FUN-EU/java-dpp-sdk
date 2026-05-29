package dppsdk.dpp4fun.payload;

import com.fasterxml.jackson.databind.ObjectMapper;
import dppsdk.core.payload.PassportMetadataPayload;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jacksonRoundTripPreservesNestedPayloadStructure() throws Exception {
        Dpp4FunPayload payload = TestDataFactory.validDppPayload();

        String json = objectMapper.writeValueAsString(payload);
        Dpp4FunPayload roundTripped = objectMapper.readValue(json, Dpp4FunPayload.class);

        assertNotNull(roundTripped.getCoreDpp());
        assertNotNull(roundTripped.getPassportMetadata());
        assertNotNull(roundTripped.getClassification());
        assertNotNull(roundTripped.getCharacteristics());
        assertNotNull(roundTripped.getNameplate());
        assertNotNull(roundTripped.getBillOfMaterials());
        assertNotNull(roundTripped.getDocumentation());
        assertTrue(json.contains("\"coreDpp\""));

        assertEquals(payload.getPassportMetadata().getUniqueProductIdentifier(),
                roundTripped.getPassportMetadata().getUniqueProductIdentifier());
        assertEquals(payload.getClassification().getTags(), roundTripped.getClassification().getTags());
        assertEquals(payload.getNameplate().getManufacturer().getRole(),
                roundTripped.getNameplate().getManufacturer().getRole());
        assertEquals(payload.getBillOfMaterials().getMaterials().size(),
                roundTripped.getBillOfMaterials().getMaterials().size());
    }

    @Test
    void passportMetadataPayloadSerializesTransportStringsAsJsonStrings() throws Exception {
        PassportMetadataPayload payload = TestDataFactory.validDppPayload().getPassportMetadata();

        String json = objectMapper.writeValueAsString(payload);

        assertTrue(json.contains("\"uniqueProductIdentifier\""));
        assertTrue(json.contains(payload.getUniqueProductIdentifier()));
        assertTrue(json.contains(payload.getPassportUpdateDates().get(0)));
    }

    @Test
    void compatibilityAccessorsStayInSyncWithCoreDpp() {
        Dpp4FunPayload payload = new Dpp4FunPayload();

        payload.setPassportMetadata(TestDataFactory.createPayloadWithInvalidUuid());

        assertNotNull(payload.getCoreDpp());
        assertEquals(payload.getCoreDpp().getPassportMetadata(), payload.getPassportMetadata());
    }
}

