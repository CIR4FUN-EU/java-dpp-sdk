package dppsdk.dpp4fun.validation;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.validation.ValidationResult;
import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.validation.identifier.EconomicOperatorIdentifierMethod;
import dppsdk.core.validation.identifier.IdentifierValidationContext;
import dppsdk.core.validation.identifier.ProductIdentifierMethod;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {
    @Test void serviceReturnsSharedCoreResultAndComposesProfileRules() {
        ValidationResult result = new Dpp4FunValidationService().validate(TestDataFactory.validDpp());
        assertEquals(ValidationResult.Status.VALID, result.status(), result.issues().toString());
        assertTrue(result.issues().isEmpty());
    }

    @Test void identifierSchemesArePassedExplicitlyAndNeverInferred() {
        Dpp4Fun value = TestDataFactory.validDpp();
        var context = new IdentifierValidationContext(ProductIdentifierMethod.UNSUPPORTED,
                EconomicOperatorIdentifierMethod.UNSUPPORTED, null);
        ValidationResult result = new Dpp4FunValidationService().validate(value, context);
        assertTrue(result.issues().stream().anyMatch(issue -> issue.rule().equals("IDENTIFIER_METHOD_UNSUPPORTED")));
        assertFalse(new Dpp4FunValidationService().validate(value).issues().stream()
                .anyMatch(issue -> issue.rule().startsWith("IDENTIFIER_")));
    }

    @Test void serviceComposesLayersInStableOrderAndReturnsImmutableStructuredIssues() {
        DataElementCollection classification = new DataElementCollection.Builder().elementId("classification")
                .dataElements(List.of(new SingleValuedDataElement.Builder().elementId("sector")
                        .valueDataType("xsd:ID").value("Furniture").build())).build();
        DigitalProductPassport core = new DigitalProductPassport.Builder()
                .dppHeader(new dppsdk.core.model.DppHeader.Builder().build())
                .dataElements(List.<DataElement>of(classification)).build();
        Dpp4Fun value = Dpp4Fun.from(core);
        var context = new IdentifierValidationContext(ProductIdentifierMethod.UNSUPPORTED,
                EconomicOperatorIdentifierMethod.UNSUPPORTED, null);
        Dpp4FunValidationService service = new Dpp4FunValidationService();

        ValidationResult first = service.validate(value, context);
        ValidationResult second = service.validate(value, context);

        assertEquals(first.issues(), second.issues());
        assertEquals("EN18223_HEADER_DPP_ID_REQUIRED", first.issues().get(0).rule());
        assertTrue(indexOfRule(first, "EN18223_VALUE_DATATYPE_SUPPORTED")
                < indexOfRule(first, "IDENTIFIER_METHOD_UNSUPPORTED"));
        assertFalse(first.issues().stream().anyMatch(issue -> issue.rule().startsWith("DPP4FUN_")));
        assertEquals(first.issues().size(), first.issues().stream()
                .map(issue -> issue.path() + "|" + issue.rule()).distinct().count());
        assertThrows(UnsupportedOperationException.class,
                () -> first.issues().add(first.issues().get(0)));
    }

    private static int indexOfRule(ValidationResult result, String rule) {
        for (int index = 0; index < result.issues().size(); index++) {
            if (result.issues().get(index).rule().equals(rule)) return index;
        }
        return Integer.MAX_VALUE;
    }
}
