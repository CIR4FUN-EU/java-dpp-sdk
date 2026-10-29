package dppsdk.core.transport.json;

import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.semantic.DataElementKind;
import dppsdk.core.semantic.SemanticDefinition;
import dppsdk.core.semantic.SemanticPath;
import dppsdk.core.semantic.SemanticResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DppJsonCodecSemanticResolverTest {
    private static final String SPEC = "urn:example:content-spec:1";
    private final DppJsonCodec codec = new DppJsonCodec();

    @Test
    void resolvesCompressedKindsAndSemanticMetadataWithoutFalseExpansionIssues() {
        DigitalProductPassport passport = passportWithNestedText("original");
        String compressed = codec.toCompressedJson(passport);
        SemanticResolver resolver = (specifications, path) -> {
            assertEquals(List.of(SPEC), specifications);
            if (path.equals(SemanticPath.of("aspect"))) {
                return Optional.of(definition(DataElementKind.COLLECTION, "aspect",
                        "https://example.org/dictionary#Aspect", null));
            }
            if (path.equals(SemanticPath.of("aspect", "label"))) {
                return Optional.of(definition(DataElementKind.SINGLE, "label",
                        "https://example.org/dictionary#Label", "xsd:string"));
            }
            return Optional.empty();
        };

        CompressedExpansionResult result = codec.expandCompressedJsonWithResolver(compressed, resolver);

        assertTrue(result.issues().isEmpty());
        DataElementCollection aspect = assertInstanceOf(
                DataElementCollection.class, result.passport().getDataElements().get(0));
        SingleValuedDataElement label = assertInstanceOf(SingleValuedDataElement.class,
                aspect.getDataElements().get(0));
        assertEquals("https://example.org/dictionary#Label", label.getDictionaryReference());
        assertEquals("xsd:string", label.getValueDataType());
        assertEquals("original", label.getValue());
    }

    @Test
    void resolverMissKeepsCurrentBestEffortInferenceAndIssues() {
        String compressed = codec.toCompressedJson(passportWithNestedText("original"));

        CompressedExpansionResult result = codec.expandCompressedJsonWithResolver(
                compressed, (specifications, path) -> Optional.empty());

        assertFalse(result.issues().isEmpty());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.reason().contains("dictionaryReference")));
        assertInstanceOf(DataElementCollection.class, result.passport().getDataElements().get(0));
    }

    @Test
    void previousPassportAndResolverRejectConflictingFixedMetadata() {
        DigitalProductPassport previous = passportWithNestedText("original");
        String compressed = codec.toCompressedJson(previous);
        SemanticResolver resolver = (specifications, path) -> path.equals(SemanticPath.of("aspect", "label"))
                ? Optional.of(definition(DataElementKind.SINGLE, "label",
                        "https://example.org/dictionary#DifferentLabel", "xsd:string"))
                : Optional.empty();

        assertThrows(DppJsonException.class,
                () -> codec.expandCompressedJson(compressed, previous, resolver));
    }

    @Test
    void previousPassportRestoresMetadataNotSuppliedByACompatibleResolver() {
        DigitalProductPassport previous = passportWithNestedText("original");
        String compressed = codec.toCompressedJson(previous);
        SemanticResolver resolver = (specifications, path) -> {
            if (path.equals(SemanticPath.of("aspect"))) {
                return Optional.of(definition(DataElementKind.COLLECTION, "aspect", null, null));
            }
            if (path.equals(SemanticPath.of("aspect", "label"))) {
                return Optional.of(definition(DataElementKind.SINGLE, "label", null, null));
            }
            return Optional.empty();
        };

        CompressedExpansionResult result = codec.expandCompressedJson(compressed, previous, resolver);

        assertEquals(previous, result.passport());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.reason().contains("dictionaryReference")));
    }

    @Test
    void resolverCanConfirmMetadataIsNotDefinedAndClearFalseMissingIssues() {
        String compressed = codec.toCompressedJson(passportWithNestedText("original"));
        SemanticResolver resolver = (specifications, path) -> path.equals(SemanticPath.of("aspect"))
                ? Optional.of(knownAbsent(DataElementKind.COLLECTION, "aspect"))
                : path.equals(SemanticPath.of("aspect", "label"))
                ? Optional.of(knownAbsent(DataElementKind.SINGLE, "label")) : Optional.empty();

        CompressedExpansionResult result = codec.expandCompressedJsonWithResolver(compressed, resolver);

        assertTrue(result.issues().isEmpty());
        DataElementCollection aspect = assertInstanceOf(DataElementCollection.class,
                result.passport().getDataElements().get(0));
        SingleValuedDataElement label = assertInstanceOf(SingleValuedDataElement.class,
                aspect.getDataElements().get(0));
        assertNull(label.getDictionaryReference());
        assertNull(label.getValueDataType());
    }

    @Test
    void previousPassportConflictsWithResolvedAbsenceOfFixedMetadata() {
        DigitalProductPassport previous = passportWithNestedText("original");
        String compressed = codec.toCompressedJson(previous);
        SemanticResolver resolver = (specifications, path) -> path.equals(SemanticPath.of("aspect", "label"))
                ? Optional.of(knownAbsent(DataElementKind.SINGLE, "label")) : Optional.empty();

        assertThrows(DppJsonException.class, () -> codec.expandCompressedJson(compressed, previous, resolver));
    }

    @Test
    void resolverMetadataWinsOverMissingMetadataInPreviousPassport() {
        SingleValuedDataElement label = new SingleValuedDataElement.Builder().elementId("label")
                .value("original").build();
        DataElementCollection aspect = new DataElementCollection.Builder().elementId("aspect")
                .dataElements(List.of(label)).build();
        DigitalProductPassport previous = passportWithElements(List.of(aspect));
        String compressed = codec.toCompressedJson(previous);
        SemanticResolver resolver = (specifications, path) -> path.equals(SemanticPath.of("aspect"))
                ? Optional.of(definition(DataElementKind.COLLECTION, "aspect", "https://example.org/Aspect", null))
                : path.equals(SemanticPath.of("aspect", "label"))
                ? Optional.of(definition(DataElementKind.SINGLE, "label", "https://example.org/Label", "xsd:string"))
                : Optional.empty();

        CompressedExpansionResult result = codec.expandCompressedJson(compressed, previous, resolver);

        DataElementCollection expandedAspect = assertInstanceOf(DataElementCollection.class,
                result.passport().getDataElements().get(0));
        SingleValuedDataElement expandedLabel = assertInstanceOf(SingleValuedDataElement.class,
                expandedAspect.getDataElements().get(0));
        assertEquals("https://example.org/Label", expandedLabel.getDictionaryReference());
        assertEquals("xsd:string", expandedLabel.getValueDataType());
    }

    @Test
    void resolverSuppliesCanonicalIdsForCompressedRepeatedItems() {
        SingleValuedDataElement item = new SingleValuedDataElement.Builder().elementId("item_00")
                .dictionaryReference("https://example.org/dictionary#Item")
                .valueDataType("xsd:string").value("chair").build();
        MultiValuedDataElement items = new MultiValuedDataElement.Builder().elementId("items")
                .dictionaryReference("https://example.org/dictionary#Items").valueDataType("xsd:string")
                .dataElements(List.of(item)).build();
        DigitalProductPassport passport = passportWithElements(List.of(items));
        String compressed = codec.toCompressedJson(passport);
        SemanticResolver resolver = (specifications, path) -> {
            if (path.equals(SemanticPath.of("items"))) {
                return Optional.of(definition(DataElementKind.MULTI_VALUED, "items",
                        "https://example.org/dictionary#Items", "xsd:string"));
            }
            if (path.equals(SemanticPath.of("items", "0"))) {
                return Optional.of(definition(DataElementKind.SINGLE, "item_00",
                        "https://example.org/dictionary#Item", "xsd:string"));
            }
            return Optional.empty();
        };

        CompressedExpansionResult result = codec.expandCompressedJsonWithResolver(compressed, resolver);

        MultiValuedDataElement expanded = assertInstanceOf(
                MultiValuedDataElement.class, result.passport().getDataElements().get(0));
        assertEquals("item_00", expanded.getDataElements().get(0).getElementId());
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void fineGrainedExpansionUsesTheCompleteSemanticPath() {
        SemanticPath expectedPath = SemanticPath.of("aspect", "label");
        SemanticResolver resolver = (specifications, path) -> path.equals(expectedPath)
                ? Optional.of(definition(DataElementKind.SINGLE, "label",
                        "https://example.org/dictionary#Label", "xsd:string"))
                : Optional.empty();

        var result = codec.expandCompressedElementJsonWithIssues("label", "\"value\"", null,
                List.of(SPEC), resolver, expectedPath);

        SingleValuedDataElement label = assertInstanceOf(SingleValuedDataElement.class, result.element());
        assertEquals("https://example.org/dictionary#Label", label.getDictionaryReference());
        assertEquals("xsd:string", label.getValueDataType());
        assertTrue(result.issues().isEmpty());
    }

    private static SemanticDefinition definition(
            DataElementKind kind, String elementId, String dictionaryReference, String valueDataType) {
        return new SemanticDefinition(Optional.ofNullable(kind), Optional.ofNullable(elementId),
                Optional.ofNullable(dictionaryReference), Optional.ofNullable(valueDataType));
    }

    private static SemanticDefinition knownAbsent(DataElementKind kind, String elementId) {
        return new SemanticDefinition(Optional.of(kind), Optional.of(elementId), Optional.empty(), Optional.empty(), true, true);
    }

    private static DigitalProductPassport passportWithNestedText(String value) {
        SingleValuedDataElement label = new SingleValuedDataElement.Builder().elementId("label")
                .dictionaryReference("https://example.org/dictionary#Label")
                .valueDataType("xsd:string").value(value).build();
        DataElementCollection aspect = new DataElementCollection.Builder().elementId("aspect")
                .dictionaryReference("https://example.org/dictionary#Aspect")
                .dataElements(List.of(label)).build();
        return passportWithElements(List.of(aspect));
    }

    private static DigitalProductPassport passportWithElements(List<dppsdk.core.model.DataElement> elements) {
        DppHeader header = new DppHeader.Builder().digitalProductPassportId("urn:example:dpp:1")
                .uniqueProductIdentifier("urn:example:product:1").granularity(Granularity.MODEL)
                .dppSchemaVersion("1.0").dppStatus("active").lastUpdated(Instant.parse("2026-01-01T00:00:00Z"))
                .economicOperatorId("urn:example:operator:1").contentSpecificationIds(List.of(SPEC)).build();
        return new DigitalProductPassport.Builder().dppHeader(header).dataElements(elements).build();
    }
}
