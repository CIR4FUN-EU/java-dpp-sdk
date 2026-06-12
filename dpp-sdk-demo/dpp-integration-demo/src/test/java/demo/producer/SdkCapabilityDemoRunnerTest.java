package demo.producer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import demo.producer.support.DemoDppFactory;
import dppsdk.core.validation.ValidationException;
import dppsdk.dpp4fun.mapper.Dpp4FunMapper;
import dppsdk.dpp4fun.payload.Dpp4FunPayload;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import org.junit.jupiter.api.Test;

class SdkCapabilityDemoRunnerTest {

    private final SdkCapabilityDemoRunner runner = new SdkCapabilityDemoRunner();
    private final DemoDppFactory factory = new DemoDppFactory();
    private final Dpp4FunMapper mapper = new Dpp4FunMapper();
    private final Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validSamplesPassValidationAndInvalidSamplesFail() {
        DemoDppSamples samples = runner.createSamples();

        assertDoesNotThrow(() -> runner.validate(samples.validBedDpp()));
        assertDoesNotThrow(() -> runner.validate(samples.validChairDpp()));
        assertThrows(ValidationException.class, () -> runner.validate(samples.invalidSupplierRoleDpp()));
        assertThrows(ValidationException.class, () -> runner.validate(samples.invalidDocumentationDpp()));
    }

    @Test
    void stepByStepDppIsValidAndMissingRequiredFieldFailsFast() {
        assertDoesNotThrow(() -> runner.validate(runner.buildStepByStepDpp()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                runner::buildDppMissingRequiredClassification
        );
        assertTrue(exception.getMessage().contains("classification is required"));
    }

    @Test
    void mapperRoundTripPreservesDppIdentity() {
        DemoDppSamples samples = runner.createSamples();

        assertEquals(
                DemoDppFactory.idOf(samples.validBedDpp()),
                DemoDppFactory.idOf(runner.mapperRoundTrip(samples.validBedDpp()))
        );
    }

    @Test
    void jsonRoundTripPreservesDppIdentity() {
        DemoDppSamples samples = runner.createSamples();

        assertEquals(
                DemoDppFactory.idOf(samples.validBedDpp()),
                DemoDppFactory.idOf(runner.jsonRoundTrip(samples.validBedDpp()))
        );
    }

    @Test
    void immutableEditExamplesProduceValidDpps() {
        DemoDppSamples samples = runner.createSamples();

        assertDoesNotThrow(() -> runner.validate(runner.editProductCharacteristics(samples.validBedDpp())));
        assertDoesNotThrow(() -> runner.validate(runner.editCoreDocumentationLink(samples.validBedDpp())));
        assertDoesNotThrow(() -> runner.validate(runner.removeOptionalDocumentation(samples.validBedDpp())));
        assertDoesNotThrow(() -> runner.validate(runner.removeFirstBomMaterial(samples.validBedDpp())));

        assertNull(runner.removeOptionalDocumentation(samples.validBedDpp()).getDocumentation());
        assertEquals(
                samples.validBedDpp().getBillOfMaterials().getMaterials().size() - 1,
                runner.removeFirstBomMaterial(samples.validBedDpp()).getBillOfMaterials().getMaterials().size()
        );
    }

    @Test
    void mapperProducesNestedJavaPayloadAndCompatibilityAccessorsStillWork() {
        Dpp4FunPayload payload = mapper.toPayload(factory.createValidBedDpp());

        assertNotNull(payload.getCoreDpp());
        assertEquals(
                payload.getCoreDpp().getPassportMetadata().getUniqueProductIdentifier(),
                payload.getPassportMetadata().getUniqueProductIdentifier()
        );
        assertEquals(
                payload.getCoreDpp().getNameplate().getManufacturer().getName(),
                payload.getNameplate().getManufacturer().getName()
        );
        assertEquals(
                payload.getCoreDpp().getDocumentation().getDigitalInstructionsLink(),
                payload.getDocumentation().getDigitalInstructionsLink()
        );
    }

    @Test
    void codecKeepsFlatJsonOutputAndAcceptsFlatAndNestedInput() throws Exception {
        String flatJson = codec.toJson(factory.createValidBedDpp());
        JsonNode flatTree = objectMapper.readTree(flatJson);

        assertTrue(flatTree.has("passportMetadata"));
        assertTrue(flatTree.has("nameplate"));
        assertTrue(flatTree.has("documentation"));
        assertFalse(flatTree.has("coreDpp"));
        assertEquals(
                DemoDppFactory.BED_DPP_ID,
                DemoDppFactory.idOf(codec.fromJson(flatJson))
        );

        ObjectNode nestedTree = (ObjectNode) objectMapper.readTree(flatJson);
        JsonNode passportMetadata = nestedTree.remove("passportMetadata");
        JsonNode nameplate = nestedTree.remove("nameplate");
        JsonNode documentation = nestedTree.remove("documentation");
        ObjectNode coreDpp = objectMapper.createObjectNode();
        coreDpp.set("passportMetadata", passportMetadata);
        coreDpp.set("nameplate", nameplate);
        coreDpp.set("documentation", documentation);
        nestedTree.set("coreDpp", coreDpp);

        assertEquals(
                DemoDppFactory.BED_DPP_ID,
                DemoDppFactory.idOf(codec.fromJson(objectMapper.writeValueAsString(nestedTree)))
        );
    }

    @Test
    void codecPrefersNestedCoreDppWhenFlatAndNestedJsonAreBothPresent() throws Exception {
        ObjectNode bothTree = (ObjectNode) objectMapper.readTree(codec.toJson(factory.createValidBedDpp()));
        ObjectNode nestedPassportMetadata = ((ObjectNode) bothTree.get("passportMetadata").deepCopy())
                .put("uniqueProductIdentifier", DemoDppFactory.CHAIR_DPP_ID);
        ObjectNode nestedCoreDpp = objectMapper.createObjectNode();
        nestedCoreDpp.set("passportMetadata", nestedPassportMetadata);
        nestedCoreDpp.set("nameplate", bothTree.get("nameplate").deepCopy());
        nestedCoreDpp.set("documentation", bothTree.get("documentation").deepCopy());
        bothTree.set("coreDpp", nestedCoreDpp);

        assertEquals(
                DemoDppFactory.CHAIR_DPP_ID,
                DemoDppFactory.idOf(codec.fromJson(objectMapper.writeValueAsString(bothTree)))
        );
    }
}
