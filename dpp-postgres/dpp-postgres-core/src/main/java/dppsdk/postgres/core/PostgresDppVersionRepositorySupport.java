package dppsdk.postgres.core;

import dppsdk.core.model.Dpp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reusable PostgreSQL support for passport identity, active-version resolution, history, and soft delete.
 *
 * <p>This class does not know about JSON transport or Dpp4Fun-specific fields.</p>
 */
public final class PostgresDppVersionRepositorySupport {

    public PostgresDppVersionRepositorySupport() {
    }

    public void initializeSchema(Connection connection) {
        PostgresJdbcSupport.executeSqlScript(connection, "/postgres/core-schema.sql");
    }

    public Optional<VersionRecord> findCurrentByDppId(Connection connection, String dppId) throws SQLException {
        return findSingle(connection, """
                select p.id as passport_id, p.dpp_id, p.product_id, p.passport_type,
                       v.id as version_id, v.version_no, v.status, v.valid_from, v.valid_to, v.operation_id
                from dpp_passports p
                join dpp_versions v on v.passport_id = p.id
                where p.dpp_id = ? and v.status = 'ACTIVE'
                """, dppId);
    }

    public Optional<VersionRecord> findCurrentByProductId(Connection connection, String productId) throws SQLException {
        return findSingle(connection, """
                select p.id as passport_id, p.dpp_id, p.product_id, p.passport_type,
                       v.id as version_id, v.version_no, v.status, v.valid_from, v.valid_to, v.operation_id
                from dpp_passports p
                join dpp_versions v on v.passport_id = p.id
                where p.product_id = ? and v.status = 'ACTIVE'
                """, productId);
    }

    public Optional<VersionRecord> findByProductIdAt(Connection connection, String productId, Instant timestamp) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select p.id as passport_id, p.dpp_id, p.product_id, p.passport_type,
                       v.id as version_id, v.version_no, v.status, v.valid_from, v.valid_to, v.operation_id
                from dpp_passports p
                join dpp_versions v on v.passport_id = p.id
                where p.product_id = ?
                  and v.valid_from <= ?
                  and (v.valid_to is null or v.valid_to > ?)
                order by v.valid_from desc, v.version_no desc
                limit 1
                """)) {
            statement.setString(1, productId);
            statement.setTimestamp(2, Timestamp.from(timestamp));
            statement.setTimestamp(3, Timestamp.from(timestamp));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapVersion(resultSet));
            }
        }
    }

    public boolean existsActiveByDppId(Connection connection, String dppId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select 1
                from dpp_passports p
                join dpp_versions v on v.passport_id = p.id
                where p.dpp_id = ? and v.status = 'ACTIVE'
                """)) {
            statement.setString(1, dppId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean existsAnyByDppId(Connection connection, String dppId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select 1
                from dpp_passports
                where dpp_id = ?
                """)) {
            statement.setString(1, dppId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public Optional<Long> findCurrentVersionNoByDppId(Connection connection, String dppId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select v.version_no
                from dpp_passports p
                join dpp_versions v on v.passport_id = p.id
                where p.dpp_id = ? and v.status = 'ACTIVE'
                """)) {
            statement.setString(1, dppId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(resultSet.getLong("version_no"));
            }
        }
    }

    public List<VersionRecord> findHistoryByDppId(Connection connection, String dppId) throws SQLException {
        List<VersionRecord> history = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                select p.id as passport_id, p.dpp_id, p.product_id, p.passport_type,
                       v.id as version_id, v.version_no, v.status, v.valid_from, v.valid_to, v.operation_id
                from dpp_passports p
                join dpp_versions v on v.passport_id = p.id
                where p.dpp_id = ?
                order by v.version_no
                """)) {
            statement.setString(1, dppId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    history.add(mapVersion(resultSet));
                }
            }
        }
        return history;
    }

    public VersionRecord create(Connection connection, Dpp dpp, PostgresDppOperationContext context) throws SQLException {
        String dppId = dpp.getDppId();
        String productId = dpp.getProductId();
        Instant occurredAt = context.occurredAt();
        long passportId = insertPassport(connection, dppId, productId, dpp.getPassportType(), occurredAt);
        long versionId = insertVersion(connection, passportId, 1L, PostgresDppStatus.ACTIVE, occurredAt, null, context.operationId());
        return new VersionRecord(passportId, versionId, dppId, productId, dpp.getPassportType(), 1L, PostgresDppStatus.ACTIVE, occurredAt, null, context.operationId());
    }

    public VersionRecord appendVersion(
            Connection connection,
            Dpp dpp,
            long expectedCurrentVersion,
            PostgresDppOperationContext context
    ) throws SQLException {
        Optional<VersionRecord> current = findCurrentByDppIdForUpdate(connection, dpp.getDppId());
        if (current.isEmpty()) {
            throw new PostgresDppNotFoundException(dpp.getDppId());
        }
        VersionRecord currentRecord = current.get();
        if (currentRecord.versionNo() != expectedCurrentVersion) {
            throw new PostgresDppVersionConflictException(
                    dpp.getDppId(),
                    expectedCurrentVersion,
                    currentRecord.versionNo()
            );
        }
        if (!currentRecord.productId().equals(dpp.getProductId())) {
            throw new IllegalArgumentException("productId is immutable for " + dpp.getDppId());
        }
        updateVersionStatus(connection, currentRecord.versionId(), PostgresDppStatus.SUPERSEDED, context.occurredAt());
        long versionId = insertVersion(
                connection,
                currentRecord.passportId(),
                currentRecord.versionNo() + 1,
                PostgresDppStatus.ACTIVE,
                context.occurredAt(),
                null,
                context.operationId()
        );
        return new VersionRecord(
                currentRecord.passportId(),
                versionId,
                currentRecord.dppId(),
                currentRecord.productId(),
                currentRecord.passportType(),
                currentRecord.versionNo() + 1,
                PostgresDppStatus.ACTIVE,
                context.occurredAt(),
                null,
                context.operationId()
        );
    }

    public VersionRecord softDelete(Connection connection, String dppId, long expectedCurrentVersion, Instant deletedAt) throws SQLException {
        Optional<VersionRecord> current = findCurrentByDppIdForUpdate(connection, dppId);
        if (current.isEmpty()) {
            throw new PostgresDppNotFoundException(dppId);
        }
        VersionRecord currentRecord = current.get();
        if (currentRecord.versionNo() != expectedCurrentVersion) {
            throw new PostgresDppVersionConflictException(
                    dppId,
                    expectedCurrentVersion,
                    currentRecord.versionNo()
            );
        }
        updateVersionStatus(connection, currentRecord.versionId(), PostgresDppStatus.DELETED, deletedAt);
        try (PreparedStatement statement = connection.prepareStatement("""
                update dpp_passports
                set deleted_at = ?
                where id = ?
                """)) {
            statement.setTimestamp(1, Timestamp.from(deletedAt));
            statement.setLong(2, currentRecord.passportId());
            statement.executeUpdate();
        }
        return new VersionRecord(
                currentRecord.passportId(),
                currentRecord.versionId(),
                currentRecord.dppId(),
                currentRecord.productId(),
                currentRecord.passportType(),
                currentRecord.versionNo(),
                PostgresDppStatus.DELETED,
                currentRecord.validFrom(),
                deletedAt,
                currentRecord.operationId()
        );
    }

    private long insertPassport(Connection connection, String dppId, String productId, String passportType, Instant occurredAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_passports (dpp_id, product_id, passport_type, created_at)
                values (?, ?, ?, ?)
                returning id
                """)) {
            statement.setString(1, dppId);
            statement.setString(2, productId);
            statement.setString(3, passportType);
            statement.setTimestamp(4, Timestamp.from(occurredAt));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private long insertVersion(
            Connection connection,
            long passportId,
            long versionNo,
            PostgresDppStatus status,
            Instant validFrom,
            Instant validTo,
            String operationId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_versions (
                    passport_id,
                    version_no,
                    status,
                    valid_from,
                    valid_to,
                    operation_id
                ) values (?, ?, ?, ?, ?, ?)
                returning id
                """)) {
            statement.setLong(1, passportId);
            statement.setLong(2, versionNo);
            statement.setString(3, status.name());
            statement.setTimestamp(4, Timestamp.from(validFrom));
            if (validTo == null) {
                statement.setNull(5, java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setTimestamp(5, Timestamp.from(validTo));
            }
            statement.setString(6, operationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private void updateVersionStatus(Connection connection, long versionId, PostgresDppStatus status, Instant validTo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                update dpp_versions
                set status = ?, valid_to = ?
                where id = ?
                """)) {
            statement.setString(1, status.name());
            statement.setTimestamp(2, Timestamp.from(validTo));
            statement.setLong(3, versionId);
            statement.executeUpdate();
        }
    }

    private Optional<VersionRecord> findCurrentByDppIdForUpdate(Connection connection, String dppId) throws SQLException {
        return findSingle(connection, """
                select p.id as passport_id, p.dpp_id, p.product_id, p.passport_type,
                       v.id as version_id, v.version_no, v.status, v.valid_from, v.valid_to, v.operation_id
                from dpp_passports p
                join dpp_versions v on v.passport_id = p.id
                where p.dpp_id = ? and v.status = 'ACTIVE'
                for update of p, v
                """, dppId);
    }

    private Optional<VersionRecord> findSingle(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapVersion(resultSet));
            }
        }
    }

    private VersionRecord mapVersion(ResultSet resultSet) throws SQLException {
        Timestamp validTo = resultSet.getTimestamp("valid_to");
        return new VersionRecord(
                resultSet.getLong("passport_id"),
                resultSet.getLong("version_id"),
                resultSet.getString("dpp_id"),
                resultSet.getString("product_id"),
                resultSet.getString("passport_type"),
                resultSet.getLong("version_no"),
                PostgresDppStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("valid_from").toInstant(),
                validTo == null ? null : validTo.toInstant(),
                resultSet.getString("operation_id")
        );
    }

    public record VersionRecord(
            long passportId,
            long versionId,
            String dppId,
            String productId,
            String passportType,
            long versionNo,
            PostgresDppStatus status,
            Instant validFrom,
            Instant validTo,
            String operationId
    ) {
    }
}
