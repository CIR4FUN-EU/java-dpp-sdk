package dppsdk.core.model;

import dppsdk.core.support.CoreTestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DppCoreBuilderTest {

    @Test
    void build_validCore_preservesFields() {
        PassportMetadata metadata = CoreTestDataFactory.validPassportMetadata();
        Nameplate nameplate = CoreTestDataFactory.validNameplate();
        Documentation documentation = CoreTestDataFactory.validDocumentation();

        DppCore core = new DppCore.Builder()
                .passportMetadata(metadata)
                .nameplate(nameplate)
                .documentation(documentation)
                .build();

        assertEquals(metadata, core.getPassportMetadata());
        assertEquals(nameplate, core.getNameplate());
        assertEquals(documentation, core.getDocumentation());
    }

    @Test
    void build_nullDocumentation_passes() {
        DppCore core = new DppCore.Builder()
                .passportMetadata(CoreTestDataFactory.validPassportMetadata())
                .nameplate(CoreTestDataFactory.validNameplate())
                .build();

        assertNull(core.getDocumentation());
    }

    @Test
    void build_missingPassportMetadata_fails() {
        assertThrows(IllegalArgumentException.class, () ->
                new DppCore.Builder()
                        .nameplate(CoreTestDataFactory.validNameplate())
                        .build());
    }

    @Test
    void build_missingNameplate_fails() {
        assertThrows(IllegalArgumentException.class, () ->
                new DppCore.Builder()
                        .passportMetadata(CoreTestDataFactory.validPassportMetadata())
                        .build());
    }

    @Test
    void toBuilder_preservesFields() {
        DppCore core = CoreTestDataFactory.validDppCore();

        assertEquals(core, core.toBuilder().build());
    }

    @Test
    void convenienceGetters_delegateToCoreSubmodels() {
        DppCore core = CoreTestDataFactory.validDppCore();

        assertEquals(core.getPassportMetadata().getUniqueProductIdentifier(), core.getUniqueProductIdentifier());
        assertEquals(core.getPassportMetadata().getPassportUpdateDates(), core.getPassportUpdateDates());
        assertEquals(core.getPassportMetadata().getQrCodeOrDigitalTag(), core.getQrCodeOrDigitalTag());
        assertEquals(core.getPassportMetadata().getExternalDocumentationLink(), core.getExternalDocumentationLink());
        assertEquals(core.getNameplate().getGtinCode(), core.getGtinCode());
        assertEquals(core.getNameplate().getManufacturer(), core.getManufacturer());
        assertEquals(core.getNameplate().getSupplier(), core.getSupplier());
        assertEquals(core.getDocumentation().getDigitalInstructionsLink(), core.getDigitalInstructionsLink());
        assertEquals(core.getDocumentation().getSafetyInstructionsLink(), core.getSafetyInstructionsLink());
    }

    @Test
    void documentationConvenienceGetters_nullDocumentation_returnNull() {
        DppCore core = new DppCore.Builder()
                .passportMetadata(CoreTestDataFactory.validPassportMetadata())
                .nameplate(CoreTestDataFactory.validNameplate())
                .build();

        assertNull(core.getDigitalInstructionsLink());
        assertNull(core.getSafetyInstructionsLink());
    }
}
