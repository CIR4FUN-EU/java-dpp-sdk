package dppsdk.core.transport.json;

import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.Granularity;
import org.junit.jupiter.api.Test;

import static dppsdk.core.transport.json.DppJsonTestSupport.fixture;
import static org.junit.jupiter.api.Assertions.*;

class DppJsonCodecCompatibilityTest {
    private final DppJsonCodec codec = new DppJsonCodec();

    @Test
    void readsLegacyMultiValuedElementsAndWritesNormativeValueShape() throws Exception {
        var legacy = codec.fromExpandedJson(fixture("expanded/multi-valued-elements.json"));
        var canonical = codec.fromExpandedJson(fixture("expanded/multi-valued-value-alternate.json"));
        assertEquals(canonical, legacy);
        assertInstanceOf(MultiValuedDataElement.class, legacy.getDataElements().get(0));
        var normalized = DppJsonTestSupport.JSON.readTree(codec.toExpandedJson(legacy));
        assertTrue(normalized.get("elements").get(0).has("value"));
        assertFalse(normalized.get("elements").get(0).has("elements"));
        assertEquals(DppJsonTestSupport.tree("expanded/multi-valued-value-alternate.json"), normalized);
    }

    @Test
    void readsCaseInsensitiveGranularityAndWritesLowercase() throws Exception {
        for (Granularity granularity : Granularity.values()) {
            String canonical = granularity.name().toLowerCase(java.util.Locale.ROOT);
            for (String lexical : new String[] {canonical, canonical.substring(0, 1).toUpperCase(java.util.Locale.ROOT)
                    + canonical.substring(1), canonical.toUpperCase(java.util.Locale.ROOT)}) {
                var parsed = codec.fromExpandedJson(fixture("expanded/header-only.json")
                        .replace("\"item\"", "\"" + lexical + "\""));
                assertEquals(granularity, parsed.getDppHeader().getGranularity(), lexical);
                assertEquals(canonical, DppJsonTestSupport.JSON.readTree(codec.toExpandedJson(parsed))
                        .get("granularity").textValue());
            }
        }
    }

    @Test
    void writesEveryGranularityAsItsLowercaseWireValue() throws Exception {
        for (Granularity granularity : Granularity.values()) {
            var header = DppJsonTestSupport.header().toBuilder().granularity(granularity).build();
            var passport = new dppsdk.core.model.DigitalProductPassport.Builder()
                    .dppHeader(header).build();
            String expected = granularity.name().toLowerCase(java.util.Locale.ROOT);
            assertEquals(expected, DppJsonTestSupport.JSON.readTree(codec.toExpandedJson(passport))
                    .get("granularity").textValue());
            assertEquals(granularity, codec.fromExpandedJson(
                    DppJsonTestSupport.fixture("expanded/header-only.json")
                            .replace("\"item\"", "\"" + expected.toUpperCase(java.util.Locale.ROOT) + "\""))
                    .getDppHeader().getGranularity());
        }
    }

    @Test
    void compressedReaderIsNotPartOfThisApi() {
        assertThrows(NoSuchMethodException.class,
                () -> DppJsonCodec.class.getMethod("fromCompressedJson", String.class));
    }
}
