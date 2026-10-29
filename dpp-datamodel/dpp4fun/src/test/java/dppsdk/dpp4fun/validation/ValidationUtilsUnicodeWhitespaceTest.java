package dppsdk.dpp4fun.validation;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.validation.ValidationException;

import dppsdk.dpp4fun.model.composite.Address;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.support.TestDataFactory;
import dppsdk.core.validation.ValidationUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsUnicodeWhitespaceTest {

    @Test
    void addressWhitespaceIsNotAProfileValidationRule() {
        for (String whitespace : List.of(
                " ", "\t", "\n", "\u00a0", "\u202f", "\u2003")) {
            Address address = new Address.Builder().country(whitespace).town("Town").build();
            var contact = Contact.builder().address(address).build();
            Dpp4Fun dpp = TestDataFactory.validDpp().toBuilder().nameplate(
                    TestDataFactory.validNameplate().toBuilder().contact(contact).build()).build();
            assertTrue(new Dpp4FunValidator().validate(dpp).isValid());
        }
    }

    @Test
    void zeroWidthSpaceAndMixedVisibleTextRemainNonBlank() {
        for (String value : List.of("\u200b", "\u00a0visible\u2003")) {
            Address address = new Address.Builder().country(value).town("Town").build();
            var contact = Contact.builder().address(address).build();
            Dpp4Fun dpp = TestDataFactory.validDpp().toBuilder().nameplate(
                    TestDataFactory.validNameplate().toBuilder().contact(contact).build()).build();
            assertFalse(new Dpp4FunValidator().validate(dpp).issues().stream()
                    .anyMatch(issue -> issue.path().endsWith("address.country")));
        }
    }

    @Test
    void semanticListValidationUsesFrozenWhitespaceAndTrimTable() {
        assertThrows(
                ValidationException.class,
                () -> ValidationUtils.requireCleanStringList(
                        List.of("\u202f"), "ProductClassification.tags"));
        assertDoesNotThrow(
                () -> ValidationUtils.requireCleanStringList(
                        List.of("\u200b"), "ProductClassification.tags"));
        assertThrows(
                ValidationException.class,
                () -> ValidationUtils.requireCleanStringList(
                        List.of("Chair", "\u00a0chair\u2003"), "ProductClassification.tags"));
    }

    @Test
    void duplicateComparisonIsIndependentOfDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThrows(
                    ValidationException.class,
                    () -> ValidationUtils.requireCleanStringList(
                            List.of("I", "i"), "ProductClassification.tags"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void numericSemanticChecksRejectNonFiniteValues() {
        assertDoesNotThrow(() -> ValidationUtils.requireNonNegative(0.0, "value"));
        assertDoesNotThrow(() -> ValidationUtils.requireNonNegative(1.25e2, "value"));
        for (double invalid : new double[] {
                -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(
                    ValidationException.class,
                    () -> ValidationUtils.requireNonNegative(invalid, "value"));
        }
    }
}
