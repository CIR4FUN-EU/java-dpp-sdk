package dppsdk.dpp4fun.semantic;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.semantic.DataElementKind;
import dppsdk.core.semantic.SemanticPath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Dpp4FunSemanticProfileCoverageTest {
    private final Dpp4FunSemanticProfile catalog = Dpp4FunSemanticProfile.loadDefault();
    private final Dpp4FunSemanticResolver resolver = new Dpp4FunSemanticResolver(catalog);

    @Test
    void bundledCatalogCoversTheFrozenRowsAndEveryCharacteristicsVariant() {
        assertEquals(488, catalog.definitions().size());
        assertEquals(6, catalog.variantContentSpecificationIds().size());
        assertEquals("DPP4FUN_COMMON_V1", catalog.contentSpecificationId());
        assertEquals("DPP4FUN_CHAIR_V1", catalog.variantContentSpecificationIds().get("chair"));
        assertEquals(List.of("general", "characteristics-chair"),
                catalog.applicabilityFor(catalog.contentSpecificationIds(Dpp4FunProductVariant.CHAIR)));
        assertEquals(19, catalog.definitions().stream()
                .filter(entry -> entry.path().equals(entry.elementId()) && entry.appliesTo().contains("general"))
                .count());
    }

    @Test
    void resolverUsesKnownCharacteristicsVariantAndLeavesUnknownContextUnresolved() {
        var definition = resolver.resolve(catalog.contentSpecificationIds(Dpp4FunProductVariant.CHAIR),
                SemanticPath.of("characteristics", "color")).orElseThrow();

        assertEquals(DataElementKind.SINGLE, definition.kind().orElseThrow());
        assertEquals("color", definition.elementId().orElseThrow());
        assertEquals("https://www.cir4fun.eu/ontology/dpp4fun#Color",
                definition.dictionaryReference().orElseThrow());
        assertEquals("xsd:string", definition.valueDataType().orElseThrow());
        assertTrue(resolver.resolve(List.of("urn:unknown:content-spec"),
                SemanticPath.of("characteristics", "color")).isEmpty());
    }

    @Test
    void resolverGeneratesTheCatalogRepeatedItemIdFromArrayPosition() {
        var definition = resolver.resolve(catalog.contentSpecificationIds(null),
                SemanticPath.of("billOfMaterials", "materialSet", "0")).orElseThrow();

        assertEquals(DataElementKind.COLLECTION, definition.kind().orElseThrow());
        assertEquals("material_00", definition.elementId().orElseThrow());
        assertEquals("http://www.w3id.org/pdto#MaterialInformation",
                definition.dictionaryReference().orElseThrow());
        var hundredth = resolver.resolve(catalog.contentSpecificationIds(null),
                SemanticPath.of("billOfMaterials", "materialSet", "100")).orElseThrow();
        assertEquals("material_100", hundredth.elementId().orElseThrow());
    }

    @Test
    void recordedSourceConflictsResolveToTheFrozenCanonicalTypesAndIris() {
        assertBinding("billOfMaterials.materialSet[*].portion", "xsd:double", "http://www.w3id.org/pdto#hasPortion");
        assertBinding("billOfMaterials.materialSet[*].mandatory", "xsd:boolean", "http://www.w3id.org/pdto#isMandatory");
        assertBinding("billOfMaterials.componentSet[*].mandatory", "xsd:boolean", "http://www.w3id.org/pdto#isMandatory");
        assertBinding("billOfMaterials.partSet[*].mandatory", "xsd:boolean", "http://www.w3id.org/pdto#isMandatory");
        assertBinding("carbonFootprint.carbonFootprintCalculationSet[*].quantityOfMeasurementForCalculation",
                "xsd:string", null);
        assertBinding("environmentalImpact.indicatorSet[*].value", "xsd:string", null);
        assertBinding("healthImpact.indicatorSet[*].value", "xsd:string", null);
        assertBinding("productRelationship.collectionMemberSet[*].collection", "xsd:string", null);
        assertBinding("nameplate.uriOfSupplier", "xsd:anyURI", null);
        assertBinding("nameplate.glnOfManufacturer", "xsd:string", null);
        assertBinding("nameplate.glnOfSupplier", "xsd:string", null);
        assertBinding("nameplate.hsCodeOfTheWCO", "xsd:string", null);
        assertKind("nameplate.manufacturerProductDescription", DataElementKind.MULTI_LANGUAGE);
        assertKind("nameplate.supplierProductDescription", DataElementKind.MULTI_LANGUAGE);
        assertBinding("recyclability.recycledContent", "xsd:double", null);
        assertBinding("repairability.repairRecordSet[*].time", "xsd:string", null);
        assertTrue(catalog.definitionFor(catalog.contentSpecificationIds(null),
                SemanticPath.of("repairability", "repairRecordSet", "0", "status")).isEmpty(),
                "RepairRecord status is not present in the frozen D4.1/Pinned AAS subtype");
        assertBinding("reusability.secondHand", "xsd:boolean", null);
        assertBinding("safety.professionalUseOnly", "xsd:boolean", null);
        assertBinding("wasteManagement.fireRisk", "xsd:boolean", null);
        assertBinding("wasteManagement.toxicityRisk", "xsd:boolean", null);
        assertBinding("wasteManagement.electricityRisk", "xsd:boolean", null);
        for (Dpp4FunProductVariant variant : Dpp4FunProductVariant.values()) {
            assertBinding("characteristics.surface", "xsd:string", null, variant);
        }
        assertBinding("characteristics.flameRetardant", "xsd:boolean",
                "https://www.cir4fun.eu/ontology/dpp4fun#FlameRetardant", Dpp4FunProductVariant.FABRIC);
        assertBinding("characteristics.colorFastness", "xsd:integer",
                "https://www.cir4fun.eu/ontology/dpp4fun#ColorFastness", Dpp4FunProductVariant.FABRIC);
        assertBinding("characteristics.matelasIncluded", "xsd:boolean",
                "https://www.cir4fun.eu/ontology/dpp4fun#MatelasIncluded", Dpp4FunProductVariant.CRIB);
        var mattress = catalog.definitions().stream().filter(entry -> entry.path().equals("characteristics.matelasIncluded"))
                .findFirst().orElseThrow();
        assertTrue(mattress.aliases().contains("mattressIncluded"));
        var manufacturing = catalog.definitions().stream()
                .filter(entry -> entry.path().equals("traceability.manufacturingRecordSet[*]"))
                .findFirst().orElseThrow();
        assertTrue(manufacturing.aliases().stream().anyMatch(alias -> alias.contains("ManufacturingStateRecord")));
        assertTrue(manufacturing.aliases().contains("ManufacturingRecord"));
        assertBinding("characteristics.volume", "xsd:double",
                "https://www.cir4fun.eu/ontology/dpp4fun#Volumn", Dpp4FunProductVariant.CHAIR);
        var surface = catalog.definitions().stream().filter(entry -> entry.path().equals("characteristics.surface"))
                .findFirst().orElseThrow();
        assertEquals("RESOLVED_WITHOUT_DICTIONARY_REFERENCE", surface.source().get("resolutionStatus"));
        var unresolvedSurface = resolver.resolve(catalog.contentSpecificationIds(Dpp4FunProductVariant.FABRIC),
                SemanticPath.of("characteristics", "surface")).orElseThrow();
        assertFalse(unresolvedSurface.dictionaryReferenceResolved());
        assertTrue(unresolvedSurface.valueDataTypeResolved());
        assertTrue(unresolvedSurface.dictionaryReference().isEmpty());
        assertEquals("xsd:string", unresolvedSurface.valueDataType().orElseThrow());
    }

    @Test
    void sourceCardinalityAndSdkBindingOccurrenceRemainDistinct() {
        var materialSet = catalog.definitions().stream()
                .filter(entry -> entry.path().equals("billOfMaterials.materialSet"))
                .findFirst().orElseThrow();
        var materialItem = catalog.definitions().stream()
                .filter(entry -> entry.path().equals("billOfMaterials.materialSet[*]"))
                .findFirst().orElseThrow();

        assertNull(materialSet.sourceCardinality());
        assertEquals(new Dpp4FunElementDefinition.Occurrence(0, 1), materialSet.bindingOccurrence());
        assertEquals(new Dpp4FunElementDefinition.Occurrence(0, null), materialItem.sourceCardinality());
        assertEquals(new Dpp4FunElementDefinition.Occurrence(0, null), materialItem.bindingOccurrence());
        var scalar = catalog.definitions().stream()
                .filter(entry -> entry.path().equals("billOfMaterials.materialSet[*].portion"))
                .findFirst().orElseThrow();
        assertNull(scalar.sourceCardinality(), "unspecified scalar cardinality must remain unresolved");
        assertEquals(new Dpp4FunElementDefinition.Occurrence(0, 1), scalar.bindingOccurrence());
        assertFalse(catalog.definitions().stream().anyMatch(entry -> entry.dictionaryReference() != null
                && (entry.dictionaryReference().equals(catalog.contentSpecificationId())
                        || catalog.variantContentSpecificationIds().containsValue(entry.dictionaryReference()))));
    }

    private void assertBinding(String path, String type, String dictionaryReference) {
        assertBinding(path, type, dictionaryReference, null);
    }

    private void assertBinding(String path, String type, String dictionaryReference,
            Dpp4FunProductVariant variant) {
        List<String> specifications = catalog.contentSpecificationIds(variant);
        var binding = catalog.definitions().stream()
                .filter(entry -> entry.path().equals(path))
                .filter(entry -> entry.appliesTo().stream().anyMatch(
                        catalog.applicabilityFor(specifications)::contains))
                .findFirst().orElseThrow(() -> new AssertionError("Missing frozen binding: " + path));
        assertEquals(type, binding.valueDataType(), path);
        assertEquals(dictionaryReference, binding.dictionaryReference(), path);
    }

    private void assertKind(String path, DataElementKind kind) {
        var binding = catalog.definitions().stream().filter(entry -> entry.path().equals(path))
                .findFirst().orElseThrow(() -> new AssertionError("Missing frozen binding: " + path));
        assertEquals(kind, binding.kind(), path);
    }
}
