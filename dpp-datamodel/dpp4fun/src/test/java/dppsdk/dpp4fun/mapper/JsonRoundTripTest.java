package dppsdk.dpp4fun.mapper;

import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.support.TestDataFactory;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonRoundTripTest {

    private final Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();

    @Test
    void domainJsonDomainRoundTripPreservesValidDpp() {
        Dpp4Fun domain = TestDataFactory.validDpp().toBuilder()
                .characteristics(TestDataFactory.validCharacteristics().toBuilder()
                        .productType("Beds")
                        .build())
                .build();

        String json = codec.toJson(domain);
        Dpp4Fun roundTripped = codec.fromJson(json);

        assertEquals(domain, roundTripped);
    }

    @Test
    void jsonContainsExpectedTransportValues() {
        Dpp4Fun domain = TestDataFactory.validDpp().toBuilder()
                .characteristics(TestDataFactory.validCharacteristics().toBuilder()
                        .productType("Beds")
                        .build())
                .build();

        String json = codec.toJson(domain);

        assertTrue(json.contains("\"passportMetadata\""));
        assertTrue(json.contains("\"uniqueProductIdentifier\":\"" + domain.getUniqueProductIdentifier() + "\""));
        assertTrue(json.contains("\"role\":\"MANUFACTURER\""));
        assertTrue(json.contains("\"productName\":\"Demo Bed\""));
    }
}

