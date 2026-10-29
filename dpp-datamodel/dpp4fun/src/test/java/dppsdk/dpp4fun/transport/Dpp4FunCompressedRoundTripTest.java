package dppsdk.dpp4fun.transport;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.internal.ProfiledAspectState;
import dppsdk.dpp4fun.model.internal.KnownProfileAspect;
import dppsdk.dpp4fun.model.unit.Dpp4FunQuantity;
import dppsdk.dpp4fun.model.unit.LengthUnit;
import dppsdk.core.model.DataElement;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile;
import dppsdk.dpp4fun.semantic.Dpp4FunElementDefinition;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Dpp4FunCompressedRoundTripTest {
    @Test
    void everyGeneralAspectAndCharacteristicsVariantRoundTripsWithResolver() {
        Dpp4FunSemanticProfile catalog = Dpp4FunSemanticProfile.loadDefault();
        Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();

        for (Dpp4FunProductVariant variant : Dpp4FunProductVariant.values()) {
            java.util.ArrayList<DataElement> elements = new java.util.ArrayList<>();
            for (KnownProfileAspect type : KnownProfileAspect.values()) {
                elements.add(aspectWithOneSourceValue(catalog, type.elementId(), "general").asDataElement());
            }
            elements.add(aspectWithOneSourceValue(catalog, "characteristics",
                    "characteristics-" + variant.id()).asDataElement());
            Dpp4Fun original = dpp(TestDataFactory.validHeader().toBuilder()
                    .contentSpecificationIds(catalog.contentSpecificationIds(variant)).build(), elements);
            var expansion = codec.expandCompressedJson(codec.toCompressedJson(original));
            String expanded = codec.toExpandedJson(original);
            String compressed = codec.toCompressedJson(original);
            Dpp4Fun fromExpanded = codec.fromExpandedJson(expanded);
            Dpp4Fun fromCompressed = codec.fromCompressedJson(compressed);

            assertTrue(expansion.issues().isEmpty(), variant + ": " + expansion.issues());
            assertEquals(original.asDigitalProductPassport(), expansion.passport(), variant.toString());
            assertEquals(original.asDigitalProductPassport(), fromExpanded.asDigitalProductPassport(), variant.toString());
            assertEquals(original.asDigitalProductPassport(), fromCompressed.asDigitalProductPassport(), variant.toString());
            assertTypedAspectViews(fromExpanded, variant);
            assertJsonEquals(compressed, codec.toCompressedJson(codec.fromExpandedJson(expanded)), variant.toString());
            String compressedToCanonicalCompressed = codec.toCompressedJson(codec.fromExpandedJson(
                    codec.toExpandedJson(codec.fromCompressedJson(compressed))));
            assertJsonEquals(compressed, compressedToCanonicalCompressed, variant.toString());
            assertEquals(catalog.contentSpecificationIds(variant),
                    expansion.passport().getDppHeader().getContentSpecificationIds(), variant.toString());
        }
    }

    @Test
    void catalogRestoresCanonicalMetadataAfterFullCompressedFullRoundTrip() {
        Dpp4FunSemanticProfile catalog = Dpp4FunSemanticProfile.loadDefault();
        ProfiledAspectState billOfMaterials = ProfiledAspectState.builder(KnownProfileAspect.BILL_OF_MATERIALS)
                        .field("materialSet[0].name", "Foam")
                        .field("materialSet[0].mandatory", true)
                        .build();
        ProfiledAspectState characteristics = ProfiledAspectState.builder(Dpp4FunProductVariant.CHAIR)
                        .field("color", "blue")
                        .quantity("length", new Dpp4FunQuantity<>(125, LengthUnit.CENTIMETER))
                        .build();
        Dpp4Fun value = dpp(TestDataFactory.validHeader().toBuilder()
                        .digitalProductPassportId("urn:dpp:1").uniqueProductIdentifier("urn:product:1")
                        .contentSpecificationIds(catalog.contentSpecificationIds(Dpp4FunProductVariant.CHAIR)).build(),
                List.of(billOfMaterials.asDataElement(), characteristics.asDataElement()));

        Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();
        String expanded = codec.toExpandedJson(value);
        String compressed = codec.toCompressedJson(value);
        var expansion = codec.expandCompressedJson(compressed);

        assertTrue(expansion.issues().isEmpty(), expansion.issues().toString());
        assertEquals(value.asDigitalProductPassport(), expansion.passport());
        assertEquals(value, codec.fromExpandedJson(expanded));
        assertEquals(value, codec.fromCompressedJson(compressed));
    }

    @Test
    void unknownContentSpecificationsRetainGenericBestEffortExpansion() {
        String compressed = """
                {"digitalProductPassportId":"urn:dpp:2","uniqueProductIdentifier":"urn:product:2",
                 "granularity":"ITEM","dppSchemaVersion":"1.0","dppStatus":"active",
                 "lastUpdated":"2026-10-07T00:00:00Z","economicOperatorId":"urn:operator:2",
                 "contentSpecificationIds":["urn:unknown"],
                 "characteristics":{"color":"blue"}}
                """;
        var expansion = new Dpp4FunJsonCodec().expandCompressedJson(compressed);
        assertFalse(expansion.issues().isEmpty());
        assertEquals("characteristics", expansion.passport().getDataElements().get(0).getElementId());
    }

    @Test
    void unresolvedSurfaceSemanticsWarnWithoutInventingMetadata() {
        Dpp4FunSemanticProfile catalog = Dpp4FunSemanticProfile.loadDefault();
        Dpp4Fun value = dpp(TestDataFactory.validHeader().toBuilder()
                        .contentSpecificationIds(catalog.contentSpecificationIds(Dpp4FunProductVariant.FABRIC)).build(),
                List.of(ProfiledAspectState.builder(Dpp4FunProductVariant.FABRIC)
                        .field("surface", "Woven matte").build().asDataElement()));

        var expansion = new Dpp4FunJsonCodec().expandCompressedJson(
                new Dpp4FunJsonCodec().toCompressedJson(value));

        assertTrue(expansion.issues().stream().anyMatch(issue -> issue.path().contains("surface")
                && issue.rule().equals("EN18223_COMPRESSED_SEMANTICS_UNRESOLVED")
                && issue.severity() == dppsdk.core.validation.ValidationIssue.Severity.WARNING
                && issue.reason().toLowerCase(java.util.Locale.ROOT).contains("dictionary")),
                expansion.issues().toString());
        var characteristics = (dppsdk.core.model.DataElementCollection) expansion.passport().getDataElements().stream()
                .filter(element -> element.getElementId().equals("characteristics")).findFirst().orElseThrow();
        var surface = (dppsdk.core.model.SingleValuedDataElement) characteristics.getDataElements().stream()
                .filter(element -> element.getElementId().equals("surface")).findFirst().orElseThrow();
        assertEquals("Woven matte", surface.getValue());
        assertEquals("xsd:string", surface.getValueDataType());
        assertNull(surface.getDictionaryReference());
    }

    private static ProfiledAspectState aspectWithOneSourceValue(
            Dpp4FunSemanticProfile catalog, String rootId, String applicability) {
        Dpp4FunElementDefinition binding = catalog.definitions().stream()
                .filter(entry -> entry.appliesTo().contains(applicability))
                .filter(entry -> entry.path().startsWith(rootId + "."))
                .filter(entry -> entry.valueDataType() != null)
                .filter(entry -> entry.kind() == dppsdk.core.semantic.DataElementKind.SINGLE
                        || entry.kind() == dppsdk.core.semantic.DataElementKind.MULTI_LANGUAGE)
                .min(java.util.Comparator.comparingInt(entry -> entry.path().length()))
                .orElseThrow(() -> new AssertionError("No source-backed scalar field for " + rootId + " / " + applicability));
        String relativePath = binding.path().substring(rootId.length() + 1).replace("[*]", "[0]");
        ProfiledAspectState.Builder builder = "characteristics".equals(rootId)
                ? ProfiledAspectState.builder(Dpp4FunProductVariant.valueOf(applicability.substring("characteristics-".length())
                        .replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase()))
                : ProfiledAspectState.builder(KnownProfileAspect.valueOf(java.util.Arrays.stream(KnownProfileAspect.values())
                        .filter(type -> type.elementId().equals(rootId)).findFirst().orElseThrow().name()));

        if (binding.kind() == dppsdk.core.semantic.DataElementKind.MULTI_LANGUAGE) {
            builder.languages(relativePath, Map.of("en", "sample"));
        } else {
            builder.field(relativePath, sampleValue(binding.valueDataType()));
        }
        return builder.build();
    }

    private static Dpp4Fun dpp(dppsdk.core.model.DppHeader header, List<DataElement> elements) {
        return Dpp4Fun.from(new DigitalProductPassport.Builder().dppHeader(header).dataElements(elements).build());
    }

    private static Object sampleValue(String valueDataType) {
        return switch (valueDataType) {
            case "xsd:string" -> "sample";
            case "xsd:anyURI" -> "https://example.org/resource";
            case "xsd:boolean" -> true;
            case "xsd:double", "xsd:decimal" -> 1.25d;
            case "xsd:integer" -> 1;
            case "xsd:dateTime" -> "2026-10-07T00:00:00Z";
            default -> throw new AssertionError("Unsupported source datatype in catalog test: " + valueDataType);
        };
    }

    private static void assertJsonEquals(String expected, String actual, String context) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            assertEquals(mapper.readTree(expected), mapper.readTree(actual), context);
        } catch (java.io.IOException exception) {
            throw new AssertionError("Could not compare canonical JSON fixtures", exception);
        }
    }

    private static void assertTypedAspectViews(Dpp4Fun value, Dpp4FunProductVariant variant) {
        assertNotNull(value.getNameplate());
        assertNotNull(value.getBillOfMaterials());
        assertNotNull(value.getCarbonFootprint());
        assertNotNull(value.getRepairability());
        assertNotNull(value.getRightOfEndUsers());
        assertNotNull(value.getTraceability());
        assertNotNull(value.getCharacteristics());
        assertEquals(variant, value.getCharacteristics().variant());
    }
}
