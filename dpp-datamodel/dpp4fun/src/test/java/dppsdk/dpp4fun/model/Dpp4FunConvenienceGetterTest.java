package dppsdk.dpp4fun.model;

import dppsdk.core.model.Dpp;
import dppsdk.core.model.DppCore;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

class Dpp4FunConvenienceGetterTest {

    @Test
    void commonConvenienceGetters_delegateToCoreDpp() {
        Dpp4Fun dpp = TestDataFactory.validDpp();

        assertEquals(dpp.getCoreDpp().getUniqueProductIdentifier(), dpp.getUniqueProductIdentifier());
        assertEquals(dpp.getCoreDpp().getPassportUpdateDates(), dpp.getPassportUpdateDates());
        assertEquals(dpp.getCoreDpp().getQrCodeOrDigitalTag(), dpp.getQrCodeOrDigitalTag());
        assertEquals(dpp.getCoreDpp().getExternalDocumentationLink(), dpp.getExternalDocumentationLink());
        assertEquals(dpp.getCoreDpp().getGtinCode(), dpp.getGtinCode());
        assertEquals(dpp.getUniqueProductIdentifier().toString(), dpp.getDppId());
        assertEquals(dpp.getGtinCode(), dpp.getProductId());
        assertEquals(dpp.getCoreDpp().getManufacturer(), dpp.getManufacturer());
        assertEquals(dpp.getCoreDpp().getSupplier(), dpp.getSupplier());
        assertEquals(dpp.getCoreDpp().getDigitalInstructionsLink(), dpp.getDigitalInstructionsLink());
        assertEquals(dpp.getCoreDpp().getSafetyInstructionsLink(), dpp.getSafetyInstructionsLink());
    }

    @Test
    void cir4FunConvenienceGetters_delegateToSpecificSubmodels() {
        Dpp4Fun dpp = TestDataFactory.validDpp();

        assertEquals(dpp.getClassification().getSector(), dpp.getSector());
        assertEquals(dpp.getClassification().getGroup(), dpp.getGroup());
        assertEquals(dpp.getClassification().getCategory(), dpp.getCategory());
        assertEquals(dpp.getClassification().getSubCategory(), dpp.getSubCategory());
        assertEquals(dpp.getClassification().getTags(), dpp.getTags());
        assertEquals(dpp.getCharacteristics().getProductName(), dpp.getProductName());
        assertEquals(dpp.getCharacteristics().getDescription(), dpp.getDescription());
        assertEquals(dpp.getCharacteristics().getBrand(), dpp.getBrand());
        assertEquals(dpp.getCharacteristics().getProductType(), dpp.getProductType());
        assertEquals(dpp.getCharacteristics().getDimensions(), dpp.getDimensions());
        assertEquals(dpp.getCharacteristics().getWeight(), dpp.getWeight());
        assertEquals(dpp.getCharacteristics().getColor(), dpp.getColor());
        assertEquals(dpp.getCharacteristics().getFeatures(), dpp.getFeatures());
    }

    @Test
    void documentationConvenienceGetters_nullDocumentation_returnNull() {
        Dpp4Fun dpp = TestDataFactory.validDpp().toBuilder()
                .coreDpp(TestDataFactory.validDppCore().toBuilder()
                        .documentation(null)
                        .build())
                .build();

        assertNull(dpp.getDigitalInstructionsLink());
        assertNull(dpp.getSafetyInstructionsLink());
    }

    @Test
    void identifierConvenienceGetters_throwWhenRequiredIdentifierIsMissing() {
        Dpp missingDppId = new Dpp() {
            @Override
            public DppCore getCoreDpp() {
                return TestDataFactory.validDppCore();
            }

            @Override
            public String getPassportType() {
                return "Test";
            }

            @Override
            public java.util.UUID getUniqueProductIdentifier() {
                return null;
            }
        };

        Dpp missingProductId = new Dpp() {
            @Override
            public DppCore getCoreDpp() {
                return TestDataFactory.validDppCore();
            }

            @Override
            public String getPassportType() {
                return "Test";
            }

            @Override
            public String getGtinCode() {
                return null;
            }
        };

        IllegalStateException missingDppIdException =
                assertThrows(IllegalStateException.class, missingDppId::getDppId);
        IllegalStateException missingProductIdException =
                assertThrows(IllegalStateException.class, missingProductId::getProductId);

        assertEquals(
                "PassportMetadata.uniqueProductIdentifier is required for dppId",
                missingDppIdException.getMessage());
        assertEquals(
                "Nameplate.gtinCode is required for productId",
                missingProductIdException.getMessage());
    }
}

