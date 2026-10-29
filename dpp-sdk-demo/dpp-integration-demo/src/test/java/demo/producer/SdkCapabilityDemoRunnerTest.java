package demo.producer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import demo.producer.support.DemoDppFactory;
import dppsdk.core.validation.ValidationException;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import org.junit.jupiter.api.Test;

class SdkCapabilityDemoRunnerTest {

    private final SdkCapabilityDemoRunner runner = new SdkCapabilityDemoRunner();
    private final DemoDppFactory factory = new DemoDppFactory();
    private final Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();

    @Test
    void validSamplesPassValidationAndInvalidSamplesFail() {
        DemoDppSamples samples = runner.createSamples();

        assertDoesNotThrow(() -> runner.validate(samples.validBedDpp()));
        assertDoesNotThrow(() -> runner.validate(samples.validChairDpp()));
        assertThrows(ValidationException.class, () -> runner.validate(samples.incompleteDpp()));
    }

    @Test
    void stepByStepDppUsesSourceBackedChairAndNoLegacyClassification() {
        assertDoesNotThrow(() -> runner.validate(runner.buildStepByStepDpp()));

        Dpp4Fun dpp = runner.buildStepByStepDpp();
        assertNotNull(dpp.getCharacteristics());
        assertEquals("Cir4Fun", dpp.getNameplate().getBrand());
    }

    @Test
    void typedFacadeRoundTripPreservesCanonicalPassportAndIdentity() {
        DemoDppSamples samples = runner.createSamples();

        assertEquals(
                DemoDppFactory.idOf(samples.validBedDpp()),
                DemoDppFactory.idOf(runner.facadeRoundTrip(samples.validBedDpp()))
        );
        assertEquals(samples.validBedDpp().asDigitalProductPassport(),
                runner.facadeRoundTrip(samples.validBedDpp()).asDigitalProductPassport());
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
        assertDoesNotThrow(() -> runner.validate(runner.removeFirstBomMaterial(samples.validBedDpp())));

        Dpp4Fun edited = runner.editProductCharacteristics(samples.validBedDpp());
        assertEquals("Cir4Fun", samples.validBedDpp().getNameplate().getBrand());
        assertEquals("Edited graphite", ((ChairCharacteristics) edited.getCharacteristics()).getColor());
        assertEquals(
                samples.validBedDpp().getBillOfMaterials().getMaterials().size() - 1,
                runner.removeFirstBomMaterial(samples.validBedDpp()).getBillOfMaterials().getMaterials().size()
        );
    }

    @Test
    void codecUsesCanonicalHeaderAndDataElements() throws Exception {
        String canonicalJson = codec.toJson(factory.createValidBedDpp());
        JsonNode tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(canonicalJson);

        assertTrue(tree.has("digitalProductPassportId"));
        assertTrue(tree.has("uniqueProductIdentifier"));
        assertTrue(tree.has("elements"));
        assertFalse(tree.has("passportMetadata"));
        assertFalse(tree.has("nameplate"));
        assertFalse(tree.has("coreDpp"));
        assertEquals(
                DemoDppFactory.BED_DPP_ID,
                DemoDppFactory.idOf(codec.fromJson(canonicalJson))
        );
    }

    @Test
    void sdkRunnerCompletesMalformedJsonRejectionExample() {
        assertDoesNotThrow(runner::run);
    }
}
