package dppsdk.dpp4fun.integration;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.validation.ValidationResult;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DppValidationFlowTest {
    private final Dpp4FunValidationService service = new Dpp4FunValidationService();

    @Test void serviceComposesCoreAndProfileValidation() {
        assertEquals(ValidationResult.Status.VALID, service.validate(TestDataFactory.validDpp()).status());
    }

    @Test void profileValidationDoesNotInferIdentifierMethodFromGtin() {
        var result = service.validate(TestDataFactory.validDpp());
        assertFalse(result.issues().stream().anyMatch(issue -> issue.rule().startsWith("IDENTIFIER_")));
    }
}
