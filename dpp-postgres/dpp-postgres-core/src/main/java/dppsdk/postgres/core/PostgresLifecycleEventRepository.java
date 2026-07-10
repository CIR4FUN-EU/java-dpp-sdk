package dppsdk.postgres.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stores and reads simple append-only lifecycle events for DPP audit purposes.
 *
 * <p>These events are mock-compatible audit records, not full event sourcing or Track & Trace.</p>
 */
public final class PostgresLifecycleEventRepository {

    private static final TypeReference<LinkedHashMap<String, String>> MAP_TYPE = new TypeReference<>() { };

    private final ObjectMapper objectMapper;

    public PostgresLifecycleEventRepository() {
        this(new ObjectMapper());
    }

    public PostgresLifecycleEventRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DppLifecycleEventRecord append(
            Connection connection,
            String dppId,
            DppLifecycleEventType eventType,
            Instant occurredAt,
            Map<String, String> data
    ) throws SQLException {
        String eventId = UUID.randomUUID().toString();
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_lifecycle_events (event_id, dpp_id, event_type, occurred_at, data)
                values (?, ?, ?, ?, cast(? as jsonb))
                """)) {
            statement.setString(1, eventId);
            statement.setString(2, dppId);
            statement.setString(3, eventType.name());
            statement.setTimestamp(4, Timestamp.from(occurredAt));
            statement.setString(5, toJson(data));
            statement.executeUpdate();
        }
        return new DppLifecycleEventRecord(eventId, dppId, eventType, occurredAt, data);
    }

    public List<DppLifecycleEventRecord> findByDppId(Connection connection, String dppId) throws SQLException {
        List<DppLifecycleEventRecord> events = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                select event_id, dpp_id, event_type, occurred_at, data::text as data_text
                from dpp_lifecycle_events
                where dpp_id = ?
                order by occurred_at, id
                """)) {
            statement.setString(1, dppId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(new DppLifecycleEventRecord(
                            resultSet.getString("event_id"),
                            resultSet.getString("dpp_id"),
                            DppLifecycleEventType.valueOf(resultSet.getString("event_type")),
                            resultSet.getTimestamp("occurred_at").toInstant(),
                            fromJson(resultSet.getString("data_text"))
                    ));
                }
            }
        }
        return events;
    }

    private String toJson(Map<String, String> data) {
        try {
            return objectMapper.writeValueAsString(data == null ? Map.of() : data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize lifecycle event data", exception);
        }
    }

    private Map<String, String> fromJson(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize lifecycle event data", exception);
        }
    }
}
