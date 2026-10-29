package dppsdk.core.validation.identifier;

import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentifierValidatorApiTest {

    @Test
    void unsupportedMethodsAreNotReportedAsInvalidSchemeSyntax() {
        DppHeader header = header("did:example:product", "did:example:operator", null);
        IdentifierValidationContext context = new IdentifierValidationContext(
                ProductIdentifierMethod.UNSUPPORTED,
                EconomicOperatorIdentifierMethod.UNSUPPORTED,
                null);

        ValidationResult result = new IdentifierValidator().validate(header, context);

        assertEquals(ValidationResult.Status.UNSUPPORTED, result.status());
        assertFalse(result.isValid());
        assertEquals(2, result.issues().size());
        assertEquals(List.of("header.uniqueProductIdentifier", "header.economicOperatorId"),
                result.issues().stream().map(issue -> issue.path()).toList());
    }

    @Test
    void methodSelectionDoesNotInferAProfileFromTheIdentifierText() {
        DppHeader header = header("https://id.gs1.org/01/09506000134352", "4171234567890128", null);
        IdentifierValidationContext context = new IdentifierValidationContext(
                ProductIdentifierMethod.UNSUPPORTED,
                EconomicOperatorIdentifierMethod.UNSUPPORTED,
                null);

        ValidationResult result = new IdentifierValidator().validate(header, context);

        assertEquals(ValidationResult.Status.UNSUPPORTED, result.status());
        assertEquals(2, result.issues().size());
    }

    @Test
    void missingValidationContextIsAnExplicitInvalidConfiguration() {
        ValidationResult result = new IdentifierValidator().validate(
                header("https://example.invalid/product", "4171234567890128", null), null);

        assertEquals(ValidationResult.Status.INVALID, result.status());
        assertEquals("context", result.issues().get(0).path());
        assertEquals("IDENTIFIER_VALIDATION_CONTEXT_REQUIRED", result.issues().get(0).rule());
    }

    @Test
    void aPresentFacilityWithoutSelectedMethodIsUnsupported() {
        ValidationResult result = new IdentifierValidator().validate(
                header("https://example.invalid/product", "4171234567890128", "4141234567890128"),
                new IdentifierValidationContext(ProductIdentifierMethod.UNSUPPORTED,
                        EconomicOperatorIdentifierMethod.GS1_GLN, null));

        assertEquals(ValidationResult.Status.UNSUPPORTED, result.status());
        assertEquals("header.facilityId", result.issues().get(1).path());
        assertEquals("IDENTIFIER_METHOD_UNSUPPORTED", result.issues().get(1).rule());
    }

    @Test
    void malformedGs1ValueDoesNotFallThroughToIdentificationLink() {
        DppHeader header = header("https://id.gs1.org/01/not-a-gtin", "4171234567890128", null);
        IdentifierValidationContext context = new IdentifierValidationContext(
                ProductIdentifierMethod.GS1_DIGITAL_LINK,
                EconomicOperatorIdentifierMethod.GS1_GLN,
                null);

        ValidationResult result = new IdentifierValidator().validate(header, context);

        assertEquals(ValidationResult.Status.INVALID, result.status());
        assertEquals("GS1_DIGITAL_LINK_KEY_INVALID", result.issues().get(0).rule());
    }

    @Test
    void didLikeTextWithSelectedGs1MethodIsInvalidGs1Syntax() {
        ValidationResult result = new IdentifierValidator().validate(
                header("did:example:product", "4171234567890128", null),
                new IdentifierValidationContext(ProductIdentifierMethod.GS1_DIGITAL_LINK,
                        EconomicOperatorIdentifierMethod.GS1_GLN, null));

        assertEquals(ValidationResult.Status.INVALID, result.status());
        assertEquals("GS1_DIGITAL_LINK_URL_INVALID", result.issues().get(0).rule());
    }

    @Test
    void identifierValidationResultIssuesAreImmutable() {
        DppHeader header = header("not-a-link", "4171234567890128", null);
        ValidationResult result = new IdentifierValidator().validate(header,
                new IdentifierValidationContext(ProductIdentifierMethod.UNSUPPORTED,
                        EconomicOperatorIdentifierMethod.UNSUPPORTED, null));

        assertThrows(UnsupportedOperationException.class,
                () -> result.issues().add(result.issues().get(0)));
    }

    @Test
    void collectsIndependentProfileErrorsAtStableHeaderPaths() {
        DppHeader header = new DppHeader.Builder()
                .uniqueProductIdentifier("https://id.gs1.org/01/09506000134353")
                .economicOperatorId("1234567890129")
                .facilityId("4171234567890128")
                .granularity(Granularity.MODEL)
                .build();

        ValidationResult result = new IdentifierValidator().validate(header,
                new IdentifierValidationContext(ProductIdentifierMethod.GS1_DIGITAL_LINK,
                        EconomicOperatorIdentifierMethod.GS1_GLN,
                        FacilityIdentifierMethod.GS1_GLN));

        assertEquals(ValidationResult.Status.INVALID, result.status());
        assertEquals(List.of(
                        "header.uniqueProductIdentifier:GS1_DIGITAL_LINK_KEY_INVALID",
                        "header.economicOperatorId:GS1_GLN_INVALID",
                        "header.facilityId:GS1_GLN_ROLE_MISMATCH"),
                result.issues().stream().map(issue -> issue.path() + ":" + issue.rule()).toList());
    }

    private static DppHeader header(String productId, String operatorId, String facilityId) {
        return new DppHeader.Builder()
                .uniqueProductIdentifier(productId)
                .economicOperatorId(operatorId)
                .facilityId(facilityId)
                .granularity(Granularity.MODEL)
                .build();
    }
}
