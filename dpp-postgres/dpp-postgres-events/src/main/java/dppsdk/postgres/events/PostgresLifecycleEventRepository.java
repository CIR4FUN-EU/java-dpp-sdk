package dppsdk.postgres.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import dppsdk.postgres.core.PostgresJdbcSupport;

/**
 * Stores and reads the SDK's append-only audit records associated with DPP changes.
 *
 * <p>The event types and record format are SDK-defined metadata. Canonical historical DPP state remains in
 * {@code dpp_versions}; these records supplement that archive and are not an event-sourcing stream or the
 * EN 18222 Lifecycle API representation.</p>
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

    public void initializeSchema(Connection connection) {
        PostgresJdbcSupport.executeSqlScript(connection, "/postgres/events-schema.sql");
    }

    public DppLifecycleEventRecord append(
            Connection connection,
            String dppId,
            DppLifecycleEventType eventType,
            Instant occurredAt,
            Map<String, String> data
    ) throws SQLException {
        return append(connection, dppId, null, null, eventType, occurredAt, data);
    }

    public DppLifecycleEventRecord append(Connection connection, String dppId, Long versionNo, String operationId,
            DppLifecycleEventType eventType, Instant occurredAt, Map<String, String> data) throws SQLException {
        occurredAt = occurredAt.truncatedTo(ChronoUnit.MICROS);
        String eventId = UUID.randomUUID().toString();
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_lifecycle_events (event_id, dpp_id, version_no, operation_id, event_type, occurred_at, data)
                values (?, ?, ?, ?, ?, ?, cast(? as jsonb))
                """)) {
            statement.setString(1, eventId);
            statement.setString(2, dppId);
            if (versionNo == null) statement.setNull(3, java.sql.Types.BIGINT); else statement.setLong(3, versionNo);
            statement.setString(4, operationId);
            statement.setString(5, eventType.name());
            statement.setTimestamp(6, Timestamp.from(occurredAt));
            statement.setString(7, toJson(data));
            statement.executeUpdate();
        }
        return new DppLifecycleEventRecord(eventId, dppId, versionNo, operationId, eventType, occurredAt, data);
    }

    public List<DppLifecycleEventRecord> findByDppId(Connection connection, String dppId) throws SQLException {
        List<DppLifecycleEventRecord> events = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                select event_id, dpp_id, version_no, operation_id, event_type, occurred_at, data::text as data_text
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
                            (Long) resultSet.getObject("version_no"),
                            resultSet.getString("operation_id"),
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
