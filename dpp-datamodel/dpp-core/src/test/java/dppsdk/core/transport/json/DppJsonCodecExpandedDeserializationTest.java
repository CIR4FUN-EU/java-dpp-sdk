package dppsdk.core.transport.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.RelatedResource;
import dppsdk.core.model.SingleValuedDataElement;
import org.junit.jupiter.api.Test;

import static dppsdk.core.transport.json.DppJsonTestSupport.fixture;
import static org.junit.jupiter.api.Assertions.*;

class DppJsonCodecExpandedDeserializationTest {
    private final DppJsonCodec codec = new DppJsonCodec();

    @Test
    void readsEveryCanonicalSubtypeFixture() throws Exception {
        assertTrue(codec.fromExpandedJson(fixture("expanded/header-only.json")).getDataElements().isEmpty());
        assertInstanceOf(SingleValuedDataElement.class, read("expanded/single-valued.json").getDataElements().get(0));
        assertInstanceOf(DataElementCollection.class, read("expanded/collection.json").getDataElements().get(0));
        assertInstanceOf(DataElementCollection.class, read("expanded/nested-collection.json").getDataElements().get(0));
        assertInstanceOf(MultiValuedDataElement.class, read("expanded/multi-valued-elements.json").getDataElements().get(0));
        assertInstanceOf(RelatedResource.class, read("expanded/related-resource.json").getDataElements().get(0));
        assertInstanceOf(MultiLanguageDataElement.class, read("expanded/multi-language.json").getDataElements().get(0));
        assertEquals(7, read("expanded/complete-nested-passport.json").getDataElements().size());
    }

    @Test
    void rejectsNonCanonicalNonUtcLastUpdatedTimestamps() throws Exception {
        String valid = fixture("expanded/header-only.json");
        assertTrue(codec.fromExpandedJson(valid).getDppHeader().getLastUpdated()
                .toString().equals("2026-09-21T10:00:00Z"));

        String offset = valid.replace("2026-09-21T10:00:00Z", "2026-09-21T11:00:00+01:00");
        String redundantFraction = valid.replace("2026-09-21T10:00:00Z", "2026-09-21T10:00:00.000Z");
        assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(offset));
        assertThrows(DppJsonException.class, () -> codec.fromExpandedJson(redundantFraction));
    }

    @Test
    void readsFlattenedHeaderFromFullAndCompressedJsonWithoutExpandingElements() throws Exception {
        String full = fixture("expanded/complete-nested-passport.json");
        String compressed = fixture("compressed/complete-nested-passport.json");
        var fullJson = new ObjectMapper().readTree(full);
        var compressedJson = new ObjectMapper().readTree(compressed);

        var parsedFull = codec.readHeader(fullJson);
        var parsedCompressed = codec.readHeader(compressedJson);

        assertEquals(codec.fromExpandedJson(full).getDppHeader(), parsedFull);
        assertEquals(codec.expandCompressedJson(compressed).passport().getDppHeader(), parsedCompressed);
        assertThrows(DppJsonException.class, () -> codec.readHeader(new ObjectMapper().createArrayNode()));
        assertThrows(DppJsonException.class, () -> codec.readHeader(new ObjectMapper().createObjectNode()));
    }

    private dppsdk.core.model.DigitalProductPassport read(String path) throws Exception {
        return codec.fromExpandedJson(fixture(path));
    }
}
