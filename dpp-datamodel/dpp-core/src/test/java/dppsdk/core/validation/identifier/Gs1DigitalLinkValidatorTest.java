package dppsdk.core.validation.identifier;

import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Gs1DigitalLinkValidatorTest {

    @Test
    void acceptsModelBatchAndItemGranularityFromGs1Keys() {
        assertValid("https://id.gs1.org/01/09506000134352", Granularity.MODEL);
        assertValid("https://id.gs1.org/01/09506000134352/10/LOT-1", Granularity.BATCH);
        assertValid("https://id.gs1.org/01/09506000134352/21/SERIAL-1", Granularity.ITEM);
        assertValid("https://id.gs1.org/01/09506000134352/22/VARIANT/10/LOT/21/SERIAL",
                Granularity.ITEM);
        assertValid("https://id.gs1.org/01/09506000134352/235/TPX-SERIAL", Granularity.ITEM);
    }

    @Test
    void requiresHttpsForGs1DigitalLinkProductIdentifiers() {
        assertIssue(validate("http://example.com/01/09506000134352", Granularity.MODEL),
                "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_URL_INVALID");
        assertIssue(validate("HTTP://example.com/01/09506000134352", Granularity.MODEL),
                "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_URL_INVALID");
    }

    @Test
    void acceptsOptionalPathSegmentsBeforeTheGs1Key() {
        assertValid("https://example.com/brand/passport/01/09506000134352/10/LOT-1",
                Granularity.BATCH);
        assertValid("https://example.com/01/09506000134352/brand/01/09506000134352",
                Granularity.MODEL);
        assertValid("https://example.com/01/09506000134352/10/LOT%2F1", Granularity.BATCH);
    }

    @Test
    void rejectsInvalidGtinCheckDigit() {
        ValidationResult result = validate("https://id.gs1.org/01/09506000134353", Granularity.MODEL);

        assertEquals(ValidationResult.Status.INVALID, result.status());
        assertIssue(result, "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_KEY_INVALID");
        assertIssue(validate("https://id.gs1.org/01/%30%39%35%30%36%30%30%30%31%33%34%33%35%32",
                Granularity.MODEL), "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_KEY_INVALID");
    }

    @Test
    void identifiesOtherValidGs1ProductKeysAsUnsupportedRatherThanMalformed() {
        ValidationResult result = validate("https://id.gs1.org/8013/MODEL-123", Granularity.MODEL);

        assertEquals(ValidationResult.Status.UNSUPPORTED, result.status());
        assertIssue(result, "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_KEY_UNSUPPORTED");
    }

    @Test
    void rejectsMalformedUriAndPercentEncoding() {
        assertIssue(validate("https://id.gs1.org/01/09506000134352/10/LOT%2", Granularity.BATCH),
                "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_URL_INVALID");
        assertIssue(validate("ftp://id.gs1.org/01/09506000134352", Granularity.MODEL),
                "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_URL_INVALID");
    }

    @Test
    void rejectsDuplicateAndMisorderedQualifiers() {
        assertIssue(validate("https://id.gs1.org/01/09506000134352/10/A/10/B", Granularity.BATCH),
                "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_QUALIFIER_INVALID");
        assertIssue(validate("https://id.gs1.org/01/09506000134352/21/S/10/L", Granularity.ITEM),
                "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_QUALIFIER_INVALID");
        assertIssue(validate("https://id.gs1.org/01/09506000134352/235/TPX/10/LOT", Granularity.ITEM),
                "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_QUALIFIER_INVALID");
    }

    @Test
    void rejectsGranularityThatConflictsWithTheEncodedKey() {
        ValidationResult result = validate("https://id.gs1.org/01/09506000134352/10/LOT-1",
                Granularity.MODEL);

        assertIssue(result, "header.granularity", "IDENTIFIER_GRANULARITY_MISMATCH");
    }

    @Test
    void consumerProductVariantDoesNotChangeTheModelGranularity() {
        assertValid("https://id.gs1.org/01/09506000134352/22/VARIANT-1", Granularity.MODEL);
    }

    @Test
    void reportsUnvalidatedNumericQueryAttributesAsUnsupportedAndRejectsMalformedQuery() {
        ValidationResult queryAttribute = validate("https://id.gs1.org/01/09506000134352?17=260101",
                Granularity.MODEL);
        assertEquals(ValidationResult.Status.UNSUPPORTED, queryAttribute.status());
        assertIssue(queryAttribute, "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_QUERY_UNSUPPORTED");
        assertEquals(ValidationResult.Status.UNSUPPORTED,
                validate("https://id.gs1.org/01/09506000134352?236=x", Granularity.MODEL).status());
        assertIssue(validate("https://id.gs1.org/01/09506000134352#passport", Granularity.MODEL),
                "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_URL_INVALID");
        assertIssue(validate("https://id.gs1.org/01/09506000134352?bad", Granularity.MODEL),
                "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_URL_INVALID");
    }

    @Test
    void treatsSemicolonAsPartOfGs1QueryExtensionValue() {
        assertValid("https://id.gs1.org/01/09506000134352?foo=bar;baz", Granularity.MODEL);
        assertEquals(ValidationResult.Status.UNSUPPORTED,
                validate("https://id.gs1.org/01/09506000134352?17=260101;foo=bar", Granularity.MODEL)
                        .status());
    }

    @Test
    void qualifierUsesTheGs1XcharSetAndPercentEncoding() {
        assertValid("https://id.gs1.org/01/09506000134352/10/LOT%2F1", Granularity.BATCH);
        assertIssue(validate("https://id.gs1.org/01/09506000134352/10/LOT&1", Granularity.BATCH),
                "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_QUALIFIER_INVALID");
        assertIssue(validate("https://id.gs1.org/01/09506000134352/10/LOT%41", Granularity.BATCH),
                "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_QUALIFIER_INVALID");
        assertIssue(validate("https://id.gs1.org/01/09506000134352/10/LOT%5B1", Granularity.BATCH),
                "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_QUALIFIER_INVALID");
    }

    private static void assertValid(String identifier, Granularity granularity) {
        ValidationResult result = validate(identifier, granularity);
        assertEquals(ValidationResult.Status.VALID, result.status(), () -> result.issues().toString());
    }

    private static ValidationResult validate(String identifier, Granularity granularity) {
        DppHeader header = new DppHeader.Builder()
                .uniqueProductIdentifier(identifier)
                .granularity(granularity)
                .economicOperatorId("4171234567890128")
                .build();
        return new IdentifierValidator().validate(header,
                new IdentifierValidationContext(ProductIdentifierMethod.GS1_DIGITAL_LINK,
                        EconomicOperatorIdentifierMethod.GS1_GLN, null));
    }

    private static void assertIssue(ValidationResult result, String path, String rule) {
        assertTrue(result.issues().stream().anyMatch(issue -> issue.path().equals(path)
                && issue.rule().equals(rule)), () -> "Expected " + rule + " at " + path
                + " but found " + result.issues());
    }
}
