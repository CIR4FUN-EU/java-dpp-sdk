package dppsdk.dpp4fun.semantic;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.semantic.SemanticPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Dpp4FunSemanticProfileTest {
    private final Dpp4FunSemanticProfile profile = Dpp4FunSemanticProfile.loadDefault();

    @Test
    void exposesSourceCardinalityAndLeavesUnspecifiedScalarsUnspecified() {
        var materialItem = profile.definitionFor(profile.contentSpecificationIds(),
                SemanticPath.of("billOfMaterials", "materialSet", "0")).orElseThrow();
        var mandatory = profile.definitionFor(profile.contentSpecificationIds(),
                SemanticPath.of("billOfMaterials", "materialSet", "0", "mandatory")).orElseThrow();

        assertEquals(new Dpp4FunElementDefinition.Occurrence(0, null), materialItem.sourceCardinality());
        assertNull(mandatory.sourceCardinality());
        assertEquals(new Dpp4FunElementDefinition.Occurrence(0, 1), mandatory.bindingOccurrence());
    }

    @Test
    void ownsJavaConceptSourceReferenceConflictAndRuleMetadata() {
        var professionalUseOnly = profile.definitions().stream()
                .filter(definition -> definition.path().equals("safety.professionalUseOnly"))
                .findFirst().orElseThrow();

        assertEquals("Safety.professionalUseOnly", professionalUseOnly.javaConcept());
        assertTrue(professionalUseOnly.sourceReferences().stream()
                .anyMatch(reference -> reference.figureNumber() == 17));
        assertEquals("xsd:boolean", professionalUseOnly.valueDataType());
        assertFalse(professionalUseOnly.conflictResolutionNote().isBlank());
        assertNotNull(professionalUseOnly.validationRules());

        var complianceStatus = profile.definitions().stream()
                .filter(definition -> definition.path().equals("durability.standardSet[*].complianceStatus"))
                .findFirst().orElseThrow();
        assertEquals("xsd:string", complianceStatus.valueDataType());
    }

    @Test
    void distinguishesResolvedAbsentMetadataFromUnresolvedMetadata() {
        var brand = profile.definitions().stream().filter(definition -> definition.path().equals("nameplate.brand"))
                .findFirst().orElseThrow();
        var surface = profile.definitions().stream()
                .filter(definition -> definition.path().equals("characteristics.surface"))
                .findFirst().orElseThrow();

        assertNull(brand.dictionaryReference());
        assertTrue(brand.dictionaryReferenceResolved(), "an authoritative absence is resolved metadata");
        assertTrue(brand.valueDataTypeResolved());
        assertFalse(surface.dictionaryReferenceResolved(), "Surface is an explicitly unresolved semantic identity");
    }

    @Test
    void profileContainsSourceBackedSubtypeMinimumsAndNamedConflictDecisions() {
        assertTrue(profile.semanticRules().stream().anyMatch(rule ->
                rule.id().equals("DPP4FUN_RECORD_STATUS_REQUIRED")
                        && rule.excludedPaths().contains("repairability.repairRecordSet[*]")));
        assertTrue(profile.conflictDecisions().stream().anyMatch(decision ->
                decision.id().equals("REPAIR_RECORD_STATUS_OMITTED")
                        && decision.status().equals("RESOLVED")));
        assertTrue(profile.conflictDecisions().stream().anyMatch(decision ->
                decision.id().equals("CHARACTERISTICS_SURFACE_DICTIONARY")
                        && decision.status().equals("RESOLVED_WITH_SOURCE_CONFLICT")));
    }

    @Test
    void rootElementIdsFollowProfileDefinitionOrder() {
        List<Dpp4FunElementDefinition> rootDefinitions = profile.definitions().stream()
                .filter(definition -> !definition.path().contains("."))
                .filter(definition -> definition.path().equals(definition.elementId()))
                .toList();
        List<Dpp4FunElementDefinition> reorderedRoots = new ArrayList<>(rootDefinitions);
        Collections.reverse(reorderedRoots);

        assertEquals(List.of("characteristics", "wasteManagement", "usage", "upgradability", "traceability",
                        "safety", "rightOfEndUsers", "reusability", "repairability", "remanufacturing",
                        "reliability", "refurbishment", "recyclability", "productRelationship", "nameplate",
                        "healthImpact", "environmentalImpact", "durability", "carbonFootprint", "billOfMaterials"),
                Dpp4FunSemanticProfile.rootElementIds(reorderedRoots));
    }
}
