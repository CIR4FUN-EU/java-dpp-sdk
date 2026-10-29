package dppsdk.dpp4fun.validation;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.internal.ProfiledAspectState;
import dppsdk.dpp4fun.model.internal.KnownProfileAspect;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Dpp4FunSemanticMinimumTest {
    private final Dpp4FunValidator validator = new Dpp4FunValidator();

    @Test
    void instructionRequiresNameDescriptionAndReference() {
        Dpp4Fun valid = dpp(ProfiledAspectState.builder(KnownProfileAspect.REPAIRABILITY)
                .field("repairInstructionSet[0].name", "Repair wheels")
                .field("repairInstructionSet[0].description", "Replace damaged wheels")
                .field("repairInstructionSet[0].reference", "https://example.org/repair.pdf")
                .build());
        assertFalse(hasRule(validator.validate(valid), "DPP4FUN_INSTRUCTION_NAME_REQUIRED"));
        assertFalse(hasRule(validator.validate(valid), "DPP4FUN_INSTRUCTION_DESCRIPTION_REQUIRED"));
        assertFalse(hasRule(validator.validate(valid), "DPP4FUN_INSTRUCTION_REFERENCE_REQUIRED"));

        Dpp4Fun invalid = dpp(ProfiledAspectState.builder(KnownProfileAspect.REPAIRABILITY)
                .field("repairInstructionSet[0].name", "Repair wheels")
                .field("repairInstructionSet[0].description", "Replace damaged wheels")
                .build());
        assertTrue(hasRule(validator.validate(invalid), "DPP4FUN_INSTRUCTION_REFERENCE_REQUIRED"));
    }

    @Test
    void assessmentRequiresNameAndDescription() {
        Dpp4Fun valid = dpp(ProfiledAspectState.builder(KnownProfileAspect.DURABILITY)
                .field("scoringSet[0].name", "Durability")
                .field("scoringSet[0].description", "Assessment description")
                .build());
        assertFalse(hasRule(validator.validate(valid), "DPP4FUN_ASSESSMENT_NAME_REQUIRED"));
        assertFalse(hasRule(validator.validate(valid), "DPP4FUN_ASSESSMENT_DESCRIPTION_REQUIRED"));

        Dpp4Fun invalid = dpp(ProfiledAspectState.builder(KnownProfileAspect.DURABILITY)
                .field("scoringSet[0].name", "Durability").build());
        assertTrue(hasRule(validator.validate(invalid), "DPP4FUN_ASSESSMENT_DESCRIPTION_REQUIRED"));
    }

    @Test
    void recordMinimumRequiresStatusOnlyWhenTheFrozenSubtypeDefinesIt() {
        Dpp4Fun repair = dpp(ProfiledAspectState.builder(KnownProfileAspect.REPAIRABILITY)
                .field("repairRecordSet[0].name", "Wheel replacement")
                .field("repairRecordSet[0].description", "Replaced two wheels")
                .field("repairRecordSet[0].operator", "Service team")
                .field("repairRecordSet[0].time", "2026-06-01T12:00:00Z")
                .build());
        assertFalse(hasRule(validator.validate(repair), "DPP4FUN_RECORD_NAME_REQUIRED"));
        assertFalse(hasRule(validator.validate(repair), "DPP4FUN_RECORD_STATUS_REQUIRED"));

        Dpp4Fun repairMissingStatus = dpp(ProfiledAspectState.builder(KnownProfileAspect.REPAIRABILITY)
                .field("repairRecordSet[0].name", "Wheel replacement")
                .field("repairRecordSet[0].description", "Replaced two wheels")
                .field("repairRecordSet[0].operator", "Service team")
                .field("repairRecordSet[0].time", "2026-06-01T12:00:00Z")
                .build());
        assertFalse(hasRule(validator.validate(repairMissingStatus), "DPP4FUN_RECORD_STATUS_REQUIRED"),
                "D4.1 RepairRecord has no status field");
        assertThrows(IllegalArgumentException.class, () -> ProfiledAspectState.builder(KnownProfileAspect.REPAIRABILITY)
                .field("repairRecordSet[0].status", "completed"));

        Dpp4Fun design = dpp(ProfiledAspectState.builder(KnownProfileAspect.TRACEABILITY)
                .field("designRecordSet[0].name", "Design release")
                .field("designRecordSet[0].description", "Released design")
                .field("designRecordSet[0].operator", "Engineering")
                .field("designRecordSet[0].time", "2026-06-01T12:00:00Z")
                .build());
        assertTrue(hasRule(validator.validate(design), "DPP4FUN_RECORD_STATUS_REQUIRED"),
                "DesignRecord requires the source-backed status field");
    }

    private static Dpp4Fun dpp(ProfiledAspectState aspect) {
        return Dpp4Fun.from(new DigitalProductPassport.Builder().dppHeader(TestDataFactory.validHeader())
                .dataElements(java.util.List.of(aspect.asDataElement())).build());
    }

    private static boolean hasRule(dppsdk.core.validation.ValidationResult result, String rule) {
        return result.issues().stream().anyMatch(issue -> rule.equals(issue.rule()));
    }
}
