package dppsdk.dpp4fun.mapper;

import dppsdk.core.mapper.MappingException;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.support.TestDataFactory;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.core.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InboundValidationTest {

    private final Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();

    @Test
    void fromJsonRejectsMissingRequiredTopLevelSection() {
        String json = """
                {
                  "classification": {
                    "sector": "Furniture",
                    "group": "Seating",
                    "category": "Beds",
                    "tags": ["demo"]
                  },
                  "characteristics": {
                    "productName": "Demo Bed",
                    "productType": "Beds"
                  },
                  "nameplate": {
                    "gtinCode": "GTIN-DEMO-001",
                    "manufacturer": {
                      "name": "Demo Manufacturer GmbH",
                      "role": "MANUFACTURER"
                    }
                  }
                }
                """;

        assertThrows(MappingException.class, () -> codec.fromJson(json));
    }

    @Test
    void fromJsonRejectsInvalidOrganizationRole() {
        String json = """
                {
                  "passportMetadata": {
                    "uniqueProductIdentifier": "1f4d3b6c-8743-47f3-9f1d-2ab3d415dd0f",
                    "passportUpdateDates": ["2026-04-20"]
                  },
                  "classification": {
                    "sector": "Furniture",
                    "group": "Seating",
                    "category": "Beds",
                    "tags": ["demo"]
                  },
                  "characteristics": {
                    "productName": "Demo Bed",
                    "productType": "Beds"
                  },
                  "nameplate": {
                    "gtinCode": "GTIN-DEMO-001",
                    "manufacturer": {
                      "name": "Demo Manufacturer GmbH",
                      "role": "BROKER"
                    }
                  }
                }
                """;

        assertThrows(MappingException.class, () -> codec.fromJson(json));
    }

    @Test
    void fromJsonAndValidateRejectsSemanticallyInvalidDomain() {
        Dpp4Fun invalidDpp = TestDataFactory.validDpp().toBuilder()
                .characteristics(TestDataFactory.validCharacteristics().toBuilder()
                        .productType("Beds")
                        .build())
                .coreDpp(TestDataFactory.validDppCore().toBuilder()
                        .passportMetadata(TestDataFactory.validPassportMetadata().toBuilder()
                                .addPassportUpdateDate(LocalDate.now().plusDays(1))
                                .build())
                        .build())
                .build();

        String json = codec.toJson(invalidDpp);

        assertThrows(ValidationException.class, () -> codec.fromJsonAndValidate(json));
    }
}

