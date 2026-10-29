package dppsdk.core.transport.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.MultiLanguageValue;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.RelatedResource;
import dppsdk.core.model.SingleValuedDataElement;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class DppJsonTestSupport {
    static final ObjectMapper JSON = new ObjectMapper()
            .setNodeFactory(new JsonNodeFactory(true))
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private DppJsonTestSupport() { }

    static String fixture(String path) throws IOException {
        try (InputStream stream = DppJsonTestSupport.class.getResourceAsStream("/" + path)) {
            assertNotNull(stream, "Missing fixture " + path);
            // ponytail: normalize CRLF so fixture string surgery in tests matches on any checkout
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    static JsonNode tree(String path) throws IOException {
        return JSON.readTree(fixture(path));
    }

    static DppHeader header() {
        return new DppHeader.Builder()
                .digitalProductPassportId("https://example.org/dpp/1234545")
                .uniqueProductIdentifier("https://example.org/1234545")
                .granularity(Granularity.ITEM)
                .dppSchemaVersion("EN18223:2026")
                .dppStatus("active")
                .lastUpdated(Instant.parse("2026-09-21T10:00:00Z"))
                .economicOperatorId("gxx:ppp456789")
                .facilityId("gxx:xxx987654")
                .contentSpecificationIds(List.of("prEN1234_xyz", "prEN5678_abc"))
                .build();
    }

    static DigitalProductPassport completePassport() {
        DataElementCollection metrics = new DataElementCollection.Builder()
                .elementId("performanceMetrics")
                .dataElements(List.of(
                        single("maxPressure", "xsd:decimal", new BigDecimal("750.0")),
                        single("isFireResistant", "xsd:boolean", true)))
                .build();
        MultiValuedDataElement voltages = new MultiValuedDataElement.Builder()
                .elementId("supportedVoltages").valueDataType("xsd:integer")
                .dataElements(List.of(
                        single("voltage110", "xsd:integer", 110, "voltage"),
                        single("voltage220", "xsd:integer", 220, "voltage"),
                        single("voltage240", "xsd:integer", 240, "voltage")))
                .build();
        DataElementCollection partOne = new DataElementCollection.Builder()
                .elementId("componentPart1")
                .dataElements(List.of(single("partID", "xsd:string", "A-101"),
                        single("material", "xsd:string", "Steel")))
                .build();
        DataElementCollection partTwo = new DataElementCollection.Builder()
                .elementId("componentPart2")
                .dataElements(List.of(single("partID", "xsd:string", "B-202"),
                        single("material", "xsd:string", "Recycled Plastic")))
                .build();
        MultiValuedDataElement parts = new MultiValuedDataElement.Builder()
                .elementId("componentParts")
                .dataElements(List.of(partOne, partTwo)).build();
        return new DigitalProductPassport.Builder().dppHeader(header()).dataElements(List.of(
                single("manufacturerName", "xsd:string", "ExampleCorp"),
                single("recycledContentPercentage", "xsd:decimal", new BigDecimal("25.5")),
                metrics,
                voltages,
                parts,
                new RelatedResource.Builder().elementId("userManual")
                        .resourceTitle("User Manual for the Smart Thermostat Pro")
                        .contentType("application/pdf")
                        .url("https://data.example.com/manuals/thermostat-pro_v2.1.pdf")
                        .language("en-GB").build(),
                new MultiLanguageDataElement.Builder().elementId("productDescription")
                        .values(List.of(
                                new MultiLanguageValue.Builder().value("One Thing").language("en-IE").build(),
                                new MultiLanguageValue.Builder().value("Una Cosa").language("es-ES").build(),
                                new MultiLanguageValue.Builder().value("Une Chose").language("fr-CH").build()))
                        .build())).build();
    }

    static SingleValuedDataElement single(String id, String type, Object value) {
        return single(id, type, value, id);
    }

    static SingleValuedDataElement single(String id, String type, Object value, String dictionaryId) {
        return new SingleValuedDataElement.Builder().elementId(id)
                .dictionaryReference("https://example.org/dictionary/" + dictionaryId)
                .valueDataType(type).value(value).build();
    }
}
