package dppsdk.dpp4fun.integration;

import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.core.model.Documentation;
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.model.PassportMetadata;
import dppsdk.support.TestDataFactory;
import dppsdk.core.validation.ValidationException;
import dppsdk.dpp4fun.validation.Dpp4FunValidator;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for the full DPP validation flow.
 *
 * These tests exercise the full object graph from construction through validation.
 * They are intentionally few and high-value only.
 */
class DppValidationFlowTest {

    private final Dpp4FunValidator validator = new Dpp4FunValidator();
    private final Dpp4FunValidationService service = new Dpp4FunValidationService();

    @Test
    void fullValidDpp_passesValidation() {
        Dpp4Fun dpp = TestDataFactory.validDpp();
        assertDoesNotThrow(() -> validator.validate(dpp));
    }

    @Test
    void fullValidDpp_passesViaService() {
        Dpp4Fun dpp = TestDataFactory.validDpp();
        assertDoesNotThrow(() -> service.validate(dpp));
    }

    @Test
    void validDppWithSupplierOnly_passesValidation() {
        Dpp4Fun dpp = new Dpp4Fun.Builder()
                .coreDpp(new DppCore.Builder()
                        .passportMetadata(new PassportMetadata.Builder()
                                .uniqueProductIdentifier(UUID.randomUUID())
                                .addPassportUpdateDate(LocalDate.now())
                                .build())
                        .nameplate(new Nameplate.Builder()
                                .gtinCode("GTIN-SUPPLIER-ONLY")
                                .supplier(TestDataFactory.validSupplier())
                                .build())
                        .build())
                .classification(TestDataFactory.validProductClassification())
                .characteristics(TestDataFactory.validCharacteristics())
                .build();
        assertDoesNotThrow(() -> validator.validate(dpp));
    }

    @Test
    void dppWithWrongRoleInNameplate_failsValidation() {
        Organization wrongRole = new Organization.Builder()
                .name("Wrong Co")
                .role(OrganizationRole.MANUFACTURER)
                .build();

        Dpp4Fun dpp = new Dpp4Fun.Builder()
                .coreDpp(new DppCore.Builder()
                        .passportMetadata(TestDataFactory.validPassportMetadata())
                        .nameplate(new Nameplate.Builder()
                                .gtinCode("GTIN-001")
                                .supplier(wrongRole)
                                .build())
                        .build())
                .classification(TestDataFactory.validProductClassification())
                .characteristics(TestDataFactory.validCharacteristics())
                .build();

        assertThrows(ValidationException.class, () -> validator.validate(dpp));
    }

    @Test
    void dppWithFutureDateInMetadata_failsValidation() {
        Dpp4Fun dpp = new Dpp4Fun.Builder()
                .coreDpp(new DppCore.Builder()
                        .passportMetadata(new PassportMetadata.Builder()
                                .uniqueProductIdentifier(UUID.randomUUID())
                                .addPassportUpdateDate(LocalDate.now().plusDays(1))
                                .build())
                        .nameplate(TestDataFactory.validNameplate())
                        .build())
                .classification(TestDataFactory.validProductClassification())
                .characteristics(TestDataFactory.validCharacteristics())
                .build();

        assertThrows(ValidationException.class, () -> validator.validate(dpp));
    }

    @Test
    void dppWithDownloadableDocumentationAndNoLink_failsValidation() {
        Dpp4Fun dpp = new Dpp4Fun.Builder()
                .coreDpp(new DppCore.Builder()
                        .passportMetadata(TestDataFactory.validPassportMetadata())
                        .nameplate(TestDataFactory.validNameplate())
                        .documentation(new Documentation.Builder()
                                .downloadable(true)
                                .build())
                        .build())
                .classification(TestDataFactory.validProductClassification())
                .characteristics(TestDataFactory.validCharacteristics())
                .build();

        assertThrows(ValidationException.class, () -> validator.validate(dpp));
    }
}

