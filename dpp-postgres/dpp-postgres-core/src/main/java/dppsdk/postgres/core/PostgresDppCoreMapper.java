package dppsdk.postgres.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.transport.json.DppJsonCodec;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Objects;

/** Maps the canonical passport to and from its expanded JSONB revision snapshot. */
public final class PostgresDppCoreMapper {

    private static final ObjectMapper HASH_JSON = hashJsonMapper();

    private final DppJsonCodec codec;

    public PostgresDppCoreMapper() {
        this(new DppJsonCodec());
    }

    PostgresDppCoreMapper(DppJsonCodec codec) {
        this.codec = Objects.requireNonNull(codec, "codec must not be null");
    }

    public String toExpandedJson(DigitalProductPassport dpp) {
        return codec.toExpandedJson(dpp);
    }

    public DigitalProductPassport fromExpandedJson(String json) {
        return codec.fromExpandedJson(json);
    }

    Snapshot snapshot(DigitalProductPassport dpp) {
        String expandedJson = toExpandedJson(dpp);
        return new Snapshot(expandedJson, sha256(expandedJson));
    }

    public DigitalProductPassport readVersionData(Connection connection, long versionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select content::text as content_text, content_hash from dpp_versions where id = ?")) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new PostgresDppNotFoundException("version " + versionId);
                }
                return readVersionData(resultSet.getString("content_text"), resultSet.getString("content_hash"), versionId);
            }
        }
    }

    /** Decodes a snapshot row already selected by a caller while preserving stored-hash verification. */
    public DigitalProductPassport readVersionData(String content, String contentHash, long versionId) {
        if (!sha256(content).equals(contentHash)) {
            throw new IllegalStateException("Stored DPP snapshot content hash mismatch for version " + versionId);
        }
        return fromExpandedJson(content);
    }

    static void setSnapshot(PreparedStatement statement, int contentIndex, int hashIndex, Snapshot snapshot)
            throws SQLException {
        statement.setString(contentIndex, snapshot.expandedJson());
        statement.setString(hashIndex, snapshot.contentHash());
    }

    private static String sha256(String expandedJson) {
        try {
            // Stored content stays byte-for-byte codec output; normalization only stabilizes the semantic hash.
            String normalizedJson = HASH_JSON.writeValueAsString(canonicalize(HASH_JSON.readTree(expandedJson)));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalizedJson.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to normalize canonical DPP JSON for hashing", exception);
        }
    }

    private static JsonNode canonicalize(JsonNode node) {
        if (node instanceof ObjectNode object) {
            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            ArrayList<String> names = new ArrayList<>();
            object.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            for (String name : names) sorted.set(name, canonicalize(object.get(name)));
            return sorted;
        }
        if (node instanceof ArrayNode array) {
            ArrayNode ordered = JsonNodeFactory.instance.arrayNode();
            array.forEach(value -> ordered.add(canonicalize(value)));
            return ordered;
        }
        if (node.isNumber()) {
            BigDecimal normalized = node.decimalValue().stripTrailingZeros();
            return JsonNodeFactory.instance.numberNode(normalized);
        }
        return node.deepCopy();
    }

    private static ObjectMapper hashJsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setNodeFactory(JsonNodeFactory.instance);
        mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        return mapper;
    }

    record Snapshot(String expandedJson, String contentHash) {
        public Snapshot {
            Objects.requireNonNull(expandedJson, "expandedJson must not be null");
            Objects.requireNonNull(contentHash, "contentHash must not be null");
        }
    }
}
