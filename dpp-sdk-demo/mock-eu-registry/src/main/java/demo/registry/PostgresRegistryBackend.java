package demo.registry;

import dpp.registry.payloads.DppStatusCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

/**
 * Optional durable backend that stores the same mock registry metadata records in PostgreSQL.
 */
final class PostgresRegistryBackend implements RegistryBackend {

    private static final String SCHEMA_RESOURCE = "postgres/registry-schema.sql";

    private final DataSource dataSource;

    PostgresRegistryBackend(DataSource dataSource) {
        this.dataSource = dataSource;
        initializeSchema();
    }

    @Override
    public RegistryRecord create(
            String productIdentifier,
            String dppIdentifier,
            String operatorIdentifier,
            String repoUrl,
            Instant now
    ) {
        String registryId = UUID.randomUUID().toString();
        RegistryRecord record = new RegistryRecord(
                registryId,
                dppIdentifier,
                productIdentifier,
                operatorIdentifier,
                repoUrl,
                now,
                now
        );
        String sql = """
                INSERT INTO registry_records (
                    registry_identifier,
                    dpp_identifier,
                    product_identifier,
                    operator_identifier,
                    repo_url,
                    registered_at,
                    last_updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindRecord(statement, record);
            statement.executeUpdate();
            return record;
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                throw new RegistryApiException(DppStatusCode.ClientResourceConflict, "REGISTRY_CONFLICT",
                        "A registry record already exists for dpp id " + dppIdentifier);
            }
            throw new IllegalStateException("Failed to insert registry record", exception);
        }
    }

    @Override
    public Optional<RegistryRecord> findByRegistryId(String registryId) {
        String sql = """
                SELECT registry_identifier, dpp_identifier, product_identifier, operator_identifier, repo_url, registered_at, last_updated_at
                FROM registry_records
                WHERE registry_identifier = ?
                """;
        return findOne(sql, registryId);
    }

    @Override
    public Optional<RegistryRecord> findByDppId(String dppId) {
        String sql = """
                SELECT registry_identifier, dpp_identifier, product_identifier, operator_identifier, repo_url, registered_at, last_updated_at
                FROM registry_records
                WHERE dpp_identifier = ?
                """;
        return findOne(sql, dppId);
    }

    @Override
    public boolean existsByDppId(String dppId) {
        String sql = "SELECT 1 FROM registry_records WHERE dpp_identifier = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dppId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check registry record existence", exception);
        }
    }

    @Override
    public List<String> findAllRegisteredDppIds() {
        String sql = "SELECT dpp_identifier FROM registry_records ORDER BY dpp_identifier";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<String> dppIds = new ArrayList<>();
            while (resultSet.next()) {
                dppIds.add(resultSet.getString("dpp_identifier"));
            }
            return dppIds;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list registry DPP ids", exception);
        }
    }

    @Override
    public void seed(
            String registryIdentifier,
            String productIdentifier,
            String dppIdentifier,
            String operatorIdentifier,
            String repoUrl,
            Instant now
    ) {
        if (findByRegistryId(registryIdentifier).isPresent() || existsByDppId(dppIdentifier)) {
            return;
        }
        RegistryRecord record = new RegistryRecord(
                registryIdentifier,
                dppIdentifier,
                productIdentifier,
                operatorIdentifier,
                repoUrl,
                now,
                now
        );
        String sql = """
                INSERT INTO registry_records (
                    registry_identifier,
                    dpp_identifier,
                    product_identifier,
                    operator_identifier,
                    repo_url,
                    registered_at,
                    last_updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindRecord(statement, record);
            statement.executeUpdate();
        } catch (SQLException exception) {
            if (!"23505".equals(exception.getSQLState())) {
                throw new IllegalStateException("Failed to seed registry record", exception);
            }
        }
    }

    @Override
    public void clear() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM registry_records");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to clear registry records", exception);
        }
    }

    private Optional<RegistryRecord> findOne(String sql, String identifier) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identifier);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readRecord(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read registry record", exception);
        }
    }

    private void initializeSchema() {
        List<String> statements = loadStatements();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize registry schema", exception);
        }
    }

    private List<String> loadStatements() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(SCHEMA_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing schema resource " + SCHEMA_RESOURCE);
            }
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            List<String> statements = new ArrayList<>();
            for (String part : sql.split(";")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    statements.add(trimmed);
                }
            }
            return statements;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load registry schema resource", exception);
        }
    }

    private void bindRecord(PreparedStatement statement, RegistryRecord record) throws SQLException {
        statement.setString(1, record.registryIdentifier());
        statement.setString(2, record.dppIdentifier());
        statement.setString(3, record.productIdentifier());
        statement.setString(4, record.operatorIdentifier());
        statement.setString(5, record.repoUrl());
        statement.setTimestamp(6, Timestamp.from(record.registeredAt()));
        statement.setTimestamp(7, Timestamp.from(record.lastUpdatedAt()));
    }

    private RegistryRecord readRecord(ResultSet resultSet) throws SQLException {
        return new RegistryRecord(
                resultSet.getString("registry_identifier"),
                resultSet.getString("dpp_identifier"),
                resultSet.getString("product_identifier"),
                resultSet.getString("operator_identifier"),
                resultSet.getString("repo_url"),
                resultSet.getTimestamp("registered_at").toInstant(),
                resultSet.getTimestamp("last_updated_at").toInstant()
        );
    }
}
