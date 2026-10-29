package dppsdk.postgres.core;

import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.transport.json.DppJsonCodec;
import dppsdk.core.transport.json.DppJsonException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostgresDppCoreMapperTest {

    @Test
    void mapsCanonicalPassportThroughExpandedCodecWithoutDroppingNestedElements() throws Exception {
        DigitalProductPassport passport = passport();
        PostgresDppCoreMapper mapper = new PostgresDppCoreMapper();
        DppJsonCodec codec = new DppJsonCodec();

        String expanded = mapper.toExpandedJson(passport);

        assertEquals(codec.toExpandedJson(passport), expanded);
        assertEquals(passport, mapper.fromExpandedJson(expanded));
        assertEquals(expanded, mapper.toExpandedJson(passport));
        var snapshot = mapper.snapshot(passport);
        assertEquals(expanded, snapshot.expandedJson());
        assertEquals(64, snapshot.contentHash().length());
        assertEquals(snapshot, mapper.snapshot(mapper.fromExpandedJson(expanded)));
    }

    @Test
    void letsCanonicalCodecErrorsEscapeUnchanged() {
        PostgresDppCoreMapper mapper = new PostgresDppCoreMapper();

        assertThrows(DppJsonException.class, () -> mapper.fromExpandedJson("{"));
    }

    @Test
    void hashesEquivalentJsonObjectsIndependentOfMapOrderAndNumericScale() {
        Map<String, Object> firstValue = new LinkedHashMap<>();
        firstValue.put("zeta", new BigDecimal("1.0"));
        firstValue.put("alpha", Map.of("second", 2, "first", 1));
        Map<String, Object> secondValue = new LinkedHashMap<>();
        secondValue.put("alpha", Map.of("first", 1, "second", 2));
        secondValue.put("zeta", new BigDecimal("1.00"));
        DigitalProductPassport first = withValue(firstValue);
        DigitalProductPassport second = withValue(secondValue);

        assertEquals(first, second);
        assertEquals(new PostgresDppCoreMapper().snapshot(first).contentHash(),
                new PostgresDppCoreMapper().snapshot(second).contentHash());
        assertNotEquals(new PostgresDppCoreMapper().snapshot(withValue(Map.of(
                        "precise", new BigDecimal("1.0000000000000001")))).contentHash(),
                new PostgresDppCoreMapper().snapshot(withValue(Map.of(
                        "precise", new BigDecimal("1.0000000000000002")))).contentHash());
    }

    static DigitalProductPassport passport() {
        DppHeader header = new DppHeader.Builder()
                .digitalProductPassportId("urn:example:dpp:bed-1")
                .uniqueProductIdentifier("https://example.org/products/bed-1")
                .granularity(Granularity.ITEM)
                .dppSchemaVersion("EN18223:2026")
                .dppStatus("active")
                .lastUpdated(Instant.parse("2026-09-21T10:00:00Z"))
                .economicOperatorId("operator-1")
                .build();
        return new DigitalProductPassport.Builder().dppHeader(header)
                .dataElements(List.of(
                        new DataElementCollection.Builder().elementId("futureProfileElement")
                                .dataElements(List.of(
                                        new SingleValuedDataElement.Builder().elementId("futureNestedValue")
                                                .valueDataType("xsd:string").value("preserve-me").build(),
                                        new DataElementCollection.Builder().elementId("futureNestedGroup")
                                                .dataElements(List.of(new SingleValuedDataElement.Builder()
                                                        .elementId("futureLeaf").value("also-preserved").build()))
                                                .build()))
                                .build()))
                .build();
    }

    private DigitalProductPassport withValue(Object value) {
        return passport().toBuilder().dataElements(List.of(new SingleValuedDataElement.Builder()
                .elementId("equivalent-json-value").value(value).build())).build();
    }
}
