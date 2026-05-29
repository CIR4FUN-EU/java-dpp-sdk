package demo.producer.support;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ApiContractAlignmentTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void repoAndRegistryStatusEnumsStayAligned() {
        assertArrayEquals(
                Stream.of(dpp.repo.payloads.DppStatusCode.values()).map(Enum::name).toArray(String[]::new),
                Stream.of(dpp.registry.payloads.DppStatusCode.values()).map(Enum::name).toArray(String[]::new)
        );
    }

    @Test
    void repoAndRegistryMessageTypesStayAligned() {
        assertArrayEquals(
                Stream.of(dpp.repo.payloads.MessageType.values()).map(Enum::name).toArray(String[]::new),
                Stream.of(dpp.registry.payloads.MessageType.values()).map(Enum::name).toArray(String[]::new)
        );
    }

    @Test
    void registryRecordPayloadJsonShapeRemainsStable() {
        Instant registeredAt = Instant.parse("2026-05-13T10:15:30Z");
        Instant lastUpdatedAt = Instant.parse("2026-05-13T11:15:30Z");

        RegistryRecordPayload payload = new RegistryRecordPayload(
                "registry-123",
                "dpp-123",
                "product-123",
                "operator-123",
                "http://localhost:8080",
                registeredAt,
                lastUpdatedAt
        );

        com.fasterxml.jackson.databind.JsonNode json = objectMapper.valueToTree(payload);
        assertEquals("registry-123", json.get("registryIdentifier").asText());
        assertEquals("dpp-123", json.get("dppIdentifier").asText());
        assertEquals("product-123", json.get("productIdentifier").asText());
        assertEquals("operator-123", json.get("operatorIdentifier").asText());
        assertEquals("http://localhost:8080", json.get("repoUrl").asText());
        assertEquals(registeredAt.getEpochSecond(), json.get("registeredAt").longValue());
        assertEquals(lastUpdatedAt.getEpochSecond(), json.get("lastUpdatedAt").longValue());
    }
}
