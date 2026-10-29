package dppsdk.postgres.projection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.postgres.core.PostgresDppCoreMapper;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Generic storage for rebuildable, non-authoritative current-state documents. */
public final class PostgresDppProjectionRepository {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private final DataSource dataSource;
    private final ObjectMapper json = new ObjectMapper();
    private final PostgresDppCoreMapper coreMapper = new PostgresDppCoreMapper();

    public PostgresDppProjectionRepository(DataSource dataSource) { this.dataSource = Objects.requireNonNull(dataSource); }

    public void upsertCurrent(String projectionName, String dppId, long versionId, DppProjectionDocument document) {
        run(connection -> { upsertCurrent(connection, projectionName, dppId, versionId, document); return null; });
    }
    public void upsertCurrent(Connection connection, String projectionName, String dppId, long versionId, DppProjectionDocument document) throws SQLException {
        DppProjectionDefinition.requireName(projectionName, "projectionName");
        String data = toJson(Objects.requireNonNull(document).values());
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_projections (projection_name, passport_id, version_id, data, updated_at)
                select ?, p.id, v.id, cast(? as jsonb), ?
                from dpp_passports p join dpp_versions v on v.passport_id = p.id
                where p.dpp_id = ? and p.deleted_at is null and v.id = ? and v.status = 'ACTIVE' and v.valid_to is null
                on conflict (projection_name, passport_id) do update set
                    version_id = excluded.version_id, data = excluded.data, updated_at = excluded.updated_at
                """)) {
            statement.setString(1, projectionName); statement.setString(2, data); statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.setString(4, dppId); statement.setLong(5, versionId);
            if (statement.executeUpdate() != 1) throw new IllegalStateException("No active current revision for DPP " + dppId + " version " + versionId);
        }
    }

    public Optional<ProjectionRecord> find(String projectionName, String dppId) {
        return query(connection -> find(connection, projectionName, dppId));
    }
    public Optional<ProjectionRecord> find(Connection connection, String projectionName, String dppId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select pr.version_id, pr.data::text as data_text, pr.updated_at
                from dpp_projections pr join dpp_passports p on p.id = pr.passport_id
                join dpp_versions v on v.id = pr.version_id and v.passport_id = p.id
                where pr.projection_name = ? and p.dpp_id = ? and p.deleted_at is null
                  and v.status = 'ACTIVE' and v.valid_to is null
                """)) {
            statement.setString(1, projectionName); statement.setString(2, dppId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(new ProjectionRecord(rs.getLong("version_id"), fromJson(rs.getString("data_text")), rs.getTimestamp("updated_at").toInstant())) : Optional.empty();
            }
        }
    }

    public void remove(String projectionName, String dppId) { run(connection -> { remove(connection, projectionName, dppId); return null; }); }
    public void remove(Connection connection, String projectionName, String dppId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("delete from dpp_projections pr using dpp_passports p where pr.passport_id = p.id and pr.projection_name = ? and p.dpp_id = ?")) {
            statement.setString(1, projectionName); statement.setString(2, dppId); statement.executeUpdate();
        }
    }

    /** Rebuilds one definition from canonical active revision snapshots using this connection. */
    public int rebuild(Connection connection, DppProjectionDefinition definition) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("delete from dpp_projections where projection_name = ?")) {
            statement.setString(1, definition.name());
            statement.executeUpdate();
        }
        int count = 0;
        try (PreparedStatement statement = connection.prepareStatement("""
                select p.dpp_id, v.id as version_id, v.content::text as content_text, v.content_hash
                from dpp_passports p join dpp_versions v on v.passport_id = p.id
                where p.deleted_at is null and v.status = 'ACTIVE' and v.valid_to is null
                order by p.dpp_id
                """); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String dppId = resultSet.getString("dpp_id");
                long versionId = resultSet.getLong("version_id");
                DigitalProductPassport dpp = coreMapper.readVersionData(resultSet.getString("content_text"),
                        resultSet.getString("content_hash"), versionId);
                upsertCurrent(connection, definition.name(), dppId, versionId, definition.project(dpp));
                count++;
            }
        }
        return count;
    }
    public int rebuild(DppProjectionDefinition definition) { return transact(connection -> rebuild(connection, definition)); }

    private String toJson(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Failed to serialize projection JSON", exception); }
    }
    private Map<String, Object> fromJson(String value) {
        try { return json.readValue(value, MAP_TYPE); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Failed to deserialize projection JSON", exception); }
    }
    private void run(SqlWork<Void> work) { query(connection -> { work.run(connection); return null; }); }
    private <T> T query(SqlWork<T> work) {
        try (Connection connection = dataSource.getConnection()) { return work.run(connection); }
        catch (SQLException exception) { throw new IllegalStateException("Projection database operation failed", exception); }
    }
    private <T> T transact(SqlWork<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try { T result = work.run(connection); connection.commit(); return result; }
            catch (SQLException | RuntimeException | Error failure) { try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); } throw failure; }
        } catch (SQLException exception) { throw new IllegalStateException("Projection rebuild failed", exception); }
    }
    @FunctionalInterface private interface SqlWork<T> { T run(Connection connection) throws SQLException; }
    public record ProjectionRecord(long versionId, Map<String, Object> data, Instant updatedAt) {
        public ProjectionRecord { data = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(data)); }
    }
}
