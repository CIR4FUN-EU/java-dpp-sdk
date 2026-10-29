package dppsdk.core.validation.identifier;

import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentificationLinkValidatorTest {

    @Test
    void acceptsStructuredModelBatchAndItemLinks() {
        assertValid("https://example.com/product?.P=MODEL1", Granularity.MODEL);
        assertValid("https://example.com/product?.1P=MODEL1", Granularity.MODEL);
        assertValid("https://example.com/product?.P=MODEL1&.T=LOT1", Granularity.BATCH);
        assertValid("https://example.com/product?.1P=MODEL1&.1T=LOT1", Granularity.BATCH);
        assertIssue(validate("https://example.com/product?%2E1P=MODEL1&%2E1T=LOT1", Granularity.BATCH),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_URL_INVALID");
        assertValid("https://example.com/product?.1P=MODEL1&.S=SERIAL1", Granularity.ITEM);
    }

    @Test
    void itemGranularityIsPreservedWhenARecognizedBatchIdentifierIsAlsoPresent() {
        assertValid("https://example.com/product?.S=SERIAL1&.1T=LOT1", Granularity.ITEM);
    }

    @Test
    void acceptsUnstructuredItemLinkAndSOnlyItemLink() {
        assertValid("https://example.com/item/unique-123", Granularity.ITEM);
        assertValid("https://example.invalid", Granularity.ITEM);
        assertValid("https://example.com/item?.S=SERIAL1", Granularity.ITEM);
    }

    @Test
    void acceptsTheStandardPermittedOmittedSchemeAndWwwForms() {
        assertValid("example.invalid/item/unique-123", Granularity.ITEM);
        assertValid("www.example.invalid/item/unique-123", Granularity.ITEM);
        assertValid("example.invalid:8443/item/unique-123", Granularity.ITEM);
        assertValid("https://example.invalid/item/unique-123", Granularity.ITEM);
    }

    @Test
    void rejectsPunycodeDomainLabelsWithSpecialUrlSemantics() {
        assertIssue(validate("https://xn--bcher-kva.invalid/item", Granularity.ITEM),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_URL_INVALID");
    }

    @Test
    void enforcesMandatoryLengthAndCharacterRulesWithoutTurningRecommendationsIntoErrors() {
        String overRecommendedLength = "https://example.invalid/" + "a".repeat(60);
        assertTrue(overRecommendedLength.length() > 72);
        assertValid(overRecommendedLength, Granularity.ITEM);

        String maxLength = "https://e.invalid/" + "a".repeat(255 - "https://e.invalid/".length());
        assertEquals(255, maxLength.length());
        assertValid(maxLength, Granularity.ITEM);
        assertIssue(validate(maxLength + "a", Granularity.ITEM),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_URL_INVALID");

        assertIssue(validate("https://example.invalid/item%2Fpart", Granularity.ITEM),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_URL_INVALID");
        assertIssue(validate("https://münchen.invalid/item", Granularity.ITEM),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_URL_INVALID");
        assertIssue(validate("https://example.invalid:service/item", Granularity.ITEM),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_URL_INVALID");
        assertIssue(validate("https://@/item", Granularity.ITEM),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_URL_INVALID");
    }

    @Test
    void rejectsInvalidUrlAndMalformedStructuredSyntax() {
        assertIssue(validate("not a url", Granularity.ITEM),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_URL_INVALID");
        assertIssue(validate("https://example.com/product?.1P", Granularity.MODEL),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_STRUCTURED_SYNTAX_INVALID");
        assertIssue(validate("https://example.com/product?.1p=lowercase", Granularity.MODEL),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_STRUCTURED_SYNTAX_INVALID");
        assertIssue(validate("https://example.com/product?internal&.1P=MODEL-1", Granularity.MODEL),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_STRUCTURED_SYNTAX_INVALID");
        assertIssue(validate("https://example.com/product?.1P=MODEL-1&", Granularity.MODEL),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_STRUCTURED_SYNTAX_INVALID");
    }

    @Test
    void requiresProductOrItemIdentifierAndBatchCode() {
        assertIssue(validate("https://example.com/product?.1T=LOT1", Granularity.BATCH),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_REQUIRED_IDENTIFIER_MISSING");
        assertIssue(validate("https://example.com/product?.1P=MODEL1&.1T=", Granularity.BATCH),
                "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_REQUIRED_IDENTIFIER_MISSING");
    }

    @Test
    void acceptsDataIdentifierValuesAllowedByTheSelectedEditionAndIlUrlRules() {
        assertValid("https://example.com/product?.P=model-1", Granularity.MODEL);
        assertValid("https://example.com/product?.T=Lot-1&.P=Model-1", Granularity.BATCH);
        assertValid("https://example.com/product?.S=Serial-1", Granularity.ITEM);
    }

    @Test
    void parameterOrderIsIrrelevantAndGranularityMustMatch() {
        assertValid("https://example.com/product?.1T=LOT1&.1P=MODEL1", Granularity.BATCH);
        assertIssue(validate("https://example.com/product?.1P=MODEL1&.1T=LOT1", Granularity.MODEL),
                "header.granularity", "IDENTIFIER_GRANULARITY_MISMATCH");
        assertIssue(validate("https://example.com/item/unique-123", Granularity.MODEL),
                "header.granularity", "IDENTIFIER_GRANULARITY_MISMATCH");
    }

    @Test
    void unknownStructuredDataIdentifierSemanticsRemainUnsupported() {
        ValidationResult result = validate("https://example.com/product?.2Q=VALUE", Granularity.MODEL);

        assertEquals(ValidationResult.Status.UNSUPPORTED, result.status());
        assertIssue(result, "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_DI_UNSUPPORTED");
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
                new IdentifierValidationContext(ProductIdentifierMethod.IDENTIFICATION_LINK,
                        EconomicOperatorIdentifierMethod.GS1_GLN, null));
    }

    private static void assertIssue(ValidationResult result, String path, String rule) {
        assertTrue(result.issues().stream().anyMatch(issue -> issue.path().equals(path)
                && issue.rule().equals(rule)), () -> "Expected " + rule + " at " + path
                + " but found " + result.issues());
    }
}
