package dppsdk.dpp4fun.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.transport.json.DppJsonCodec;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import dppsdk.dpp4fun.model.internal.ProfiledAspectState;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidator;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Dpp4FunSurfaceInputTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Dpp4FunSemanticProfile PROFILE = Dpp4FunSemanticProfile.loadDefault();
    private static final Dpp4FunJsonCodec DPP4FUN_CODEC = new Dpp4FunJsonCodec();

    private static final List<String> PUBLISHED_SURFACES = List.of(
            "Smooth",
            "Woven matte",
            "Small-structured chenille fabric",
            "Impregnated decor paper",
            "Smooth semi-matte",
            "Small-structured chenille fabric, micro-textured metal");

    @Test
    void preservesPublishedSurfaceDescriptionsThroughFullAndCompressedJson() throws Exception {
        for (Dpp4FunProductVariant variant : Dpp4FunProductVariant.values()) {
            String description = PUBLISHED_SURFACES.get(variant.ordinal() % PUBLISHED_SURFACES.size());
            ProfiledAspectState constructed = ProfiledAspectState.builder(variant)
                    .field("surface", description).build();
            SingleValuedDataElement constructedSurface = assertInstanceOf(SingleValuedDataElement.class,
                    constructed.asDataElement().getDataElements().get(0));
            assertEquals(description, constructedSurface.getValue());
            assertEquals("xsd:string", constructedSurface.getValueDataType());

            DigitalProductPassport input = passport(variant, description, "xsd:string");
            DppJsonCodec coreCodec = new DppJsonCodec();
            Dpp4Fun fromExpanded = DPP4FUN_CODEC.fromExpandedJson(coreCodec.toExpandedJson(input));
            Dpp4Fun fromCompressed = DPP4FUN_CODEC.fromCompressedJson(coreCodec.toCompressedJson(input));

            assertCanonicalSurface(fromExpanded, description, variant);
            assertCanonicalSurface(fromCompressed, description, variant);
            assertStringWireValue(DPP4FUN_CODEC.toExpandedJson(fromExpanded), description, variant);
            assertStringWireValue(DPP4FUN_CODEC.toExpandedJson(fromCompressed), description, variant);
            assertEquals(fromExpanded, DPP4FUN_CODEC.fromJson(DPP4FUN_CODEC.toJson(fromExpanded)));
            assertEquals(fromCompressed, DPP4FUN_CODEC.fromCompressedJson(
                    DPP4FUN_CODEC.toCompressedJson(fromCompressed)));
            var expandedValidation = new Dpp4FunValidator().validate(fromExpanded);
            assertTrue(expandedValidation.isValid(), expandedValidation.issues().toString());
            var compressedValidation = new Dpp4FunValidator().validate(fromCompressed);
            assertTrue(compressedValidation.isValid(), compressedValidation.issues().toString());
        }

        // A numeric-looking string is still the source-declared String value.
        ProfiledAspectState numericText = ProfiledAspectState.builder(Dpp4FunProductVariant.CHAIR)
                .field("surface", "42.5").build();
        SingleValuedDataElement numericTextValue = assertInstanceOf(SingleValuedDataElement.class,
                numericText.asDataElement().getDataElements().get(0));
        assertEquals("42.5", numericTextValue.getValue());
        assertEquals("xsd:string", numericTextValue.getValueDataType());
    }

    @Test
    void numericMeasurementsAreNotCoercedIntoSurfaceDescriptions() {
        DigitalProductPassport input = passport(Dpp4FunProductVariant.CHAIR, 0.5d, "xsd:double");
        Dpp4Fun value = Dpp4Fun.from(input);

        SingleValuedDataElement surface = assertInstanceOf(SingleValuedDataElement.class,
                ((DataElementCollection) value.asDigitalProductPassport().getDataElements().get(0))
                        .getDataElements().get(0));
        assertEquals(0.5d, surface.getValue());
        assertEquals("xsd:double", surface.getValueDataType());
        var result = new Dpp4FunValidator().validate(value);
        assertFalse(result.isValid());
        assertTrue(result.issues().stream().anyMatch(issue ->
                issue.rule().equals("DPP4FUN_BINDING_DATATYPE_MISMATCH")));
    }

    private static DigitalProductPassport passport(
            Dpp4FunProductVariant variant, Object value, String valueDataType) {
        SingleValuedDataElement surface = new SingleValuedDataElement.Builder()
                .elementId("surface").value(value).valueDataType(valueDataType).build();
        DataElementCollection characteristics = new DataElementCollection.Builder()
                .elementId("characteristics")
                .dictionaryReference("https://www.cir4fun.eu/ontology/dpp4fun#Characteristics")
                .dataElements(List.of(surface)).build();
        var header = TestDataFactory.validHeader().toBuilder()
                .contentSpecificationIds(PROFILE.contentSpecificationIds(variant)).build();
        return new DigitalProductPassport.Builder().dppHeader(header).dataElements(List.of(characteristics)).build();
    }

    private static void assertCanonicalSurface(Dpp4Fun value, String source, Dpp4FunProductVariant variant) {
        DataElementCollection characteristics = assertInstanceOf(DataElementCollection.class,
                value.asDigitalProductPassport().getDataElements().get(0));
        SingleValuedDataElement surface = assertInstanceOf(SingleValuedDataElement.class,
                characteristics.getDataElements().get(0));
        assertEquals(source, surface.getValue());
        assertEquals("xsd:string", surface.getValueDataType());
        Object typedSurface = switch (variant) {
            case CHAIR -> value.getChairCharacteristics().getSurface();
            case CRIB -> value.getCribCharacteristics().getSurface();
            case FABRIC -> value.getFabricCharacteristics().getSurface();
            case LOCKER -> value.getLockerCharacteristics().getSurface();
            case PARTICLE_BOARD -> value.getParticleBoardCharacteristics().getSurface();
            case SOFA -> value.getSofaCharacteristics().getSurface();
        };
        assertEquals(source, typedSurface);
        assertEquals(variant, value.getCharacteristics().variant());
    }

    private static void assertStringWireValue(String json, String expected, Dpp4FunProductVariant variant)
            throws Exception {
        JsonNode root = JSON.readTree(json);
        JsonNode surface = root.path("elements").get(0).path("elements").get(0);
        assertEquals("xsd:string", surface.path("valueDataType").textValue(), json);
        assertTrue(surface.path("value").isTextual(), variant + " Surface must use a JSON string: " + json);
        assertEquals(expected, surface.path("value").textValue());
    }
}
