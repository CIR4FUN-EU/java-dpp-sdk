package dppsdk.core.validation.identifier;

import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Gs1GlnValidatorTest {

    @Test
    void acceptsPartyAndPhysicalLocationGlnRepresentationsInTheirRoles() {
        assertValid("4171234567890128", null, true);
        assertValid("https://id.gs1.org/417/1234567890128", null, true);
        assertValid("http://example.com/417/1234567890128", null, true);
        assertValid("https://example.com/company/identity/417/1234567890128", null, true);
        assertValid("4171234567890128", "4141234567890128", true);
        assertValid("4171234567890128", "https://id.gs1.org/414/1234567890128", true);
        assertValid("4171234567890128", "http://example.com/414/1234567890128", true);
        assertValid("4171234567890128", "https://id.gs1.org/414/1234567890128/254/BLDG-A", true);
        assertValid("4171234567890128", "https://id.gs1.org/414/1234567890128/254/BLDG%2FA", true);
        assertValid("4171234567890128", "https://example.com/sites/current/414/1234567890128", true);
        assertValid("4171234567890128",
                "https://example.com/414/1234567890128/company/414/1234567890128", true);
    }

    @Test
    void rejectsInvalidStructureAndCheckDigit() {
        assertIssue(validate("1234567890128", null, true),
                "header.economicOperatorId", "GS1_GLN_INVALID");
        assertIssue(validate("123456789012", null, true),
                "header.economicOperatorId", "GS1_GLN_INVALID");
        assertIssue(validate("1234567890129", null, true),
                "header.economicOperatorId", "GS1_GLN_INVALID");
    }

    @Test
    void validatesGlnExtensionWithGs1XcharAndEncodingRules() {
        assertIssue(validate("4171234567890128",
                        "https://id.gs1.org/414/1234567890128/254/BLDG&A", true),
                "header.facilityId", "GS1_GLN_INVALID");
        assertIssue(validate("4171234567890128",
                        "https://id.gs1.org/414/1234567890128/254/BLDG%5BA", true),
                "header.facilityId", "GS1_GLN_INVALID");
        assertIssue(validate("4171234567890128",
                        "https://id.gs1.org/414/1234567890128/254/BLDG%41", true),
                "header.facilityId", "GS1_GLN_INVALID");
    }

    @Test
    void rejectsRoleMarkedPartyAndLocationMisuse() {
        assertIssue(validate("4141234567890128", null, true),
                "header.economicOperatorId", "GS1_GLN_ROLE_MISMATCH");
        assertIssue(validate("4171234567890128", "4171234567890128", true),
                "header.facilityId", "GS1_GLN_ROLE_MISMATCH");
    }

    @Test
    void facilityIdentifierAndMethodAreOptionalWhenNoFacilityIdExists() {
        ValidationResult result = validate("4171234567890128", null, false);

        assertEquals(ValidationResult.Status.VALID, result.status());
        assertTrue(result.issues().isEmpty());
    }

    private static void assertValid(String operatorId, String facilityId, boolean withFacilityMethod) {
        assertEquals(ValidationResult.Status.VALID,
                validate(operatorId, facilityId, withFacilityMethod).status());
    }

    private static ValidationResult validate(String operatorId, String facilityId,
                                             boolean withFacilityMethod) {
        DppHeader header = new DppHeader.Builder()
                .uniqueProductIdentifier("https://id.gs1.org/01/09506000134352")
                .granularity(Granularity.MODEL)
                .economicOperatorId(operatorId)
                .facilityId(facilityId)
                .build();
        return new IdentifierValidator().validate(header,
                new IdentifierValidationContext(ProductIdentifierMethod.GS1_DIGITAL_LINK,
                        EconomicOperatorIdentifierMethod.GS1_GLN,
                        withFacilityMethod ? FacilityIdentifierMethod.GS1_GLN : null));
    }

    private static void assertIssue(ValidationResult result, String path, String rule) {
        assertTrue(result.issues().stream().anyMatch(issue -> issue.path().equals(path)
                && issue.rule().equals(rule)), () -> "Expected " + rule + " at " + path
                + " but found " + result.issues());
    }
}
