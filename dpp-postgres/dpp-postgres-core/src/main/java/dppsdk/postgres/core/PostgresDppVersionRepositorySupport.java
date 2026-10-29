package dppsdk.postgres.core;

import dppsdk.core.model.DigitalProductPassport;

import java.sql.Connection;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Reusable PostgreSQL support for canonical passport identity, snapshots, history, and soft delete. */
public final class PostgresDppVersionRepositorySupport {

    private final PostgresDppCoreMapper mapper;

    public PostgresDppVersionRepositorySupport() {
        this(new PostgresDppCoreMapper());
    }

    PostgresDppVersionRepositorySupport(PostgresDppCoreMapper mapper) {
        this.mapper = mapper;
    }

    public void initializeSchema(Connection connection) {
        PostgresJdbcSupport.executeSqlScript(connection, "/postgres/core-schema.sql");
    }

    public Optional<VersionRecord> findCurrentByDppId(Connection connection, String dppId) throws SQLException {
        return findSingle(connection, """
                select %s
                from dpp_passports p join dpp_versions v on v.passport_id = p.id
                where p.dpp_id = ? and p.deleted_at is null and v.status = 'ACTIVE' and v.valid_to is null
                """.formatted(VERSION_COLUMNS), dppId);
    }

    public Optional<VersionRecord> findCurrentByProductId(Connection connection, String productId) throws SQLException {
        return findSingle(connection, """
                select %s
                from dpp_passports p join dpp_versions v on v.passport_id = p.id
                where p.product_id = ? and p.deleted_at is null and v.status = 'ACTIVE' and v.valid_to is null
                """.formatted(VERSION_COLUMNS), productId);
    }

    public Optional<VersionRecord> findVersionByDppId(Connection connection, String dppId, long versionNo)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select %s
                from dpp_passports p join dpp_versions v on v.passport_id = p.id
                where p.dpp_id = ? and v.version_no = ?
                """.formatted(VERSION_COLUMNS))) {
            statement.setString(1, dppId);
            statement.setLong(2, versionNo);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapVersion(resultSet)) : Optional.empty();
            }
        }
    }

    public Optional<VersionRecord> findByProductIdAt(Connection connection, String productId, Instant timestamp)
            throws SQLException {
        return findAt(connection, "p.product_id", productId, timestamp);
    }

    public Optional<VersionRecord> findByDppIdAt(Connection connection, String dppId, Instant timestamp)
            throws SQLException {
        return findAt(connection, "p.dpp_id", dppId, timestamp);
    }

    private Optional<VersionRecord> findAt(Connection connection, String identifierColumn, String identifier,
                                           Instant timestamp) throws SQLException {
        Instant databaseTimestamp = timestamp.truncatedTo(ChronoUnit.MICROS);
        try (PreparedStatement statement = connection.prepareStatement("""
                select %s
                from dpp_passports p join dpp_versions v on v.passport_id = p.id
                where %s = ? and v.valid_from <= ? and (v.valid_to is null or v.valid_to > ?)
                order by v.valid_from desc, v.version_no desc
                limit 1
                """.formatted(VERSION_COLUMNS, identifierColumn))) {
            statement.setString(1, identifier);
            statement.setTimestamp(2, Timestamp.from(databaseTimestamp));
            statement.setTimestamp(3, Timestamp.from(databaseTimestamp));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapVersion(resultSet)) : Optional.empty();
            }
        }
    }

    public boolean existsActiveByDppId(Connection connection, String dppId) throws SQLException {
        return findCurrentByDppId(connection, dppId).isPresent();
    }

    public boolean existsAnyByDppId(Connection connection, String dppId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select 1 from dpp_passports where dpp_id = ?")) {
            statement.setString(1, dppId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public Optional<Long> findCurrentVersionNoByDppId(Connection connection, String dppId) throws SQLException {
        return findCurrentByDppId(connection, dppId).map(VersionRecord::versionNo);
    }

    public List<String> findAllActiveDppIds(Connection connection) throws SQLException {
        List<String> dppIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                select p.dpp_id
                from dpp_passports p join dpp_versions v on v.passport_id = p.id
                where p.deleted_at is null and v.status = 'ACTIVE' and v.valid_to is null
                order by p.dpp_id
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) dppIds.add(resultSet.getString("dpp_id"));
        }
        return dppIds;
    }

    public DppPage<String> findActiveDppIds(Connection connection, DppPageRequest request) throws SQLException {
        int offset = request.cursor() == null ? 0 : Integer.parseInt(request.cursor());
        List<String> dppIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                select p.dpp_id
                from dpp_passports p join dpp_versions v on v.passport_id = p.id
                where p.deleted_at is null and v.status = 'ACTIVE' and v.valid_to is null
                order by p.dpp_id
                limit ? offset ?
                """)) {
            statement.setInt(1, request.limit() + 1);
            statement.setInt(2, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) dppIds.add(resultSet.getString("dpp_id"));
            }
        }
        boolean hasNext = dppIds.size() > request.limit();
        if (hasNext) dppIds.remove(dppIds.size() - 1);
        return new DppPage<>(dppIds, hasNext ? Integer.toString(offset + request.limit()) : null);
    }

    /** Batch lookup that preserves input order and the input-offset cursor used by the lifecycle API. */
    public DppPage<String> findActiveDppIdsByProductIds(Connection connection, List<String> productIds,
                                                        DppPageRequest request) throws SQLException {
        int start = request.cursor() == null ? 0 : Integer.parseInt(request.cursor());
        if (start >= productIds.size()) return new DppPage<>(List.of(), null);
        Array values = connection.createArrayOf("varchar", productIds.subList(start, productIds.size()).toArray(String[]::new));
        List<String> items = new ArrayList<>();
        String cursor = null;
        try (PreparedStatement statement = connection.prepareStatement("""
                select input.order_index, p.dpp_id
                from unnest(cast(? as varchar[])) with ordinality as input(product_id, order_index)
                join dpp_passports p on p.product_id = input.product_id and p.deleted_at is null
                join dpp_versions v on v.passport_id = p.id and v.status = 'ACTIVE' and v.valid_to is null
                order by input.order_index
                limit ?
                """)) {
            statement.setArray(1, values);
            statement.setInt(2, request.limit());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(resultSet.getString("dpp_id"));
                    cursor = Integer.toString(start + resultSet.getInt("order_index"));
                }
            }
        } finally {
            values.free();
        }
        if (items.size() < request.limit()) cursor = null;
        return new DppPage<>(items, cursor);
    }

    public List<VersionRecord> findHistoryByDppId(Connection connection, String dppId) throws SQLException {
        List<VersionRecord> history = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                select %s
                from dpp_passports p join dpp_versions v on v.passport_id = p.id
                where p.dpp_id = ? order by v.version_no
                """.formatted(VERSION_COLUMNS))) {
            statement.setString(1, dppId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) history.add(mapVersion(resultSet));
            }
        }
        return history;
    }

    public VersionRecord create(Connection connection, DigitalProductPassport dpp,
                                PostgresDppOperationContext context) throws SQLException {
        PostgresDppCoreMapper.Snapshot snapshot = mapper.snapshot(dpp);
        String dppId = dpp.getDppHeader().getDigitalProductPassportId();
        String productId = dpp.getDppHeader().getUniqueProductIdentifier();
        Instant occurredAt = context.occurredAt();
        long passportId = insertPassport(connection, dppId, productId, occurredAt);
        return insertVersion(connection, passportId, dppId, productId, 1L,
                PostgresDppStatus.ACTIVE, occurredAt, null, context.operationId(), snapshot);
    }

    public VersionRecord appendVersion(Connection connection, DigitalProductPassport dpp,
                                       long expectedCurrentVersion, PostgresDppOperationContext context)
            throws SQLException {
        String dppId = dpp.getDppHeader().getDigitalProductPassportId();
        Optional<Long> passportId = lockActivePassport(connection, dppId);
        if (passportId.isEmpty()) throw new PostgresDppNotFoundException(dppId);
        VersionRecord current = findCurrentByDppId(connection, dppId)
                .orElseThrow(() -> new PostgresDppNotFoundException(dppId));
        if (current.versionNo() != expectedCurrentVersion) {
            throw new PostgresDppVersionConflictException(dppId, expectedCurrentVersion, current.versionNo());
        }
        String productId = dpp.getDppHeader().getUniqueProductIdentifier();
        if (!current.productId().equals(productId)) {
            throw new IllegalArgumentException("uniqueProductIdentifier is immutable for " + dppId);
        }
        PostgresDppCoreMapper.Snapshot snapshot = mapper.snapshot(dpp);
        if (current.contentHash().equals(snapshot.contentHash())) return current;
        Instant occurredAt = context.occurredAt();
        requireAfterCurrentVersion(dppId, occurredAt, current.validFrom());
        updateVersionStatus(connection, current.versionId(), PostgresDppStatus.SUPERSEDED, occurredAt);
        return insertVersion(connection, current.passportId(), dppId, productId, current.versionNo() + 1,
                PostgresDppStatus.ACTIVE, occurredAt, null, context.operationId(), snapshot);
    }

    public VersionRecord softDelete(Connection connection, String dppId, long expectedCurrentVersion,
                                    Instant deletedAt) throws SQLException {
        deletedAt = deletedAt.truncatedTo(ChronoUnit.MICROS);
        if (lockActivePassport(connection, dppId).isEmpty()) throw new PostgresDppNotFoundException(dppId);
        VersionRecord current = findCurrentByDppId(connection, dppId)
                .orElseThrow(() -> new PostgresDppNotFoundException(dppId));
        if (current.versionNo() != expectedCurrentVersion) {
            throw new PostgresDppVersionConflictException(dppId, expectedCurrentVersion, current.versionNo());
        }
        requireAfterCurrentVersion(dppId, deletedAt, current.validFrom());
        updateVersionStatus(connection, current.versionId(), PostgresDppStatus.DELETED, deletedAt);
        try (PreparedStatement statement = connection.prepareStatement(
                "update dpp_passports set deleted_at = ? where id = ? and deleted_at is null")) {
            statement.setTimestamp(1, Timestamp.from(deletedAt));
            statement.setLong(2, current.passportId());
            if (statement.executeUpdate() != 1) throw new IllegalStateException("Passport was concurrently deleted: " + dppId);
        }
        return new VersionRecord(current.passportId(), current.versionId(), current.dppId(), current.productId(),
                current.versionNo(), PostgresDppStatus.DELETED, current.validFrom(), deletedAt,
                current.storedAt(), current.operationId(), current.contentHash());
    }

    private Optional<Long> lockActivePassport(Connection connection, String dppId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select id from dpp_passports where dpp_id = ? and deleted_at is null for update
                """)) {
            statement.setString(1, dppId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(resultSet.getLong("id")) : Optional.empty();
            }
        }
    }

    private long insertPassport(Connection connection, String dppId, String productId, Instant createdAt)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_passports (dpp_id, product_id, created_at) values (?, ?, ?) returning id
                """)) {
            statement.setString(1, dppId);
            statement.setString(2, productId);
            statement.setTimestamp(3, Timestamp.from(createdAt));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private VersionRecord insertVersion(Connection connection, long passportId, String dppId, String productId,
                                        long versionNo, PostgresDppStatus status, Instant validFrom, Instant validTo,
                                        String operationId, PostgresDppCoreMapper.Snapshot snapshot) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_versions (
                    passport_id, version_no, status, valid_from, valid_to, operation_id, content, content_hash
                ) values (?, ?, ?, ?, ?, ?, cast(? as jsonb), ?) returning id, stored_at
                """)) {
            statement.setLong(1, passportId);
            statement.setLong(2, versionNo);
            statement.setString(3, status.name());
            statement.setTimestamp(4, Timestamp.from(validFrom));
            if (validTo == null) statement.setNull(5, java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
            else statement.setTimestamp(5, Timestamp.from(validTo));
            statement.setString(6, operationId);
            PostgresDppCoreMapper.setSnapshot(statement, 7, 8, snapshot);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new VersionRecord(passportId, resultSet.getLong("id"), dppId, productId, versionNo, status,
                        validFrom, validTo, resultSet.getTimestamp("stored_at").toInstant(), operationId,
                        snapshot.contentHash());
            }
        }
    }

    private void updateVersionStatus(Connection connection, long versionId, PostgresDppStatus status, Instant validTo)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                update dpp_versions set status = ?, valid_to = ? where id = ? and status = 'ACTIVE' and valid_to is null
                """)) {
            statement.setString(1, status.name());
            statement.setTimestamp(2, Timestamp.from(validTo));
            statement.setLong(3, versionId);
            if (statement.executeUpdate() != 1) throw new IllegalStateException("Active version changed concurrently");
        }
    }

    private void requireAfterCurrentVersion(String dppId, Instant next, Instant current) {
        if (!next.isAfter(current)) {
            throw new IllegalArgumentException("operation time must be after current version for " + dppId);
        }
    }

    private Optional<VersionRecord> findSingle(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapVersion(resultSet)) : Optional.empty();
            }
        }
    }

    private VersionRecord mapVersion(ResultSet resultSet) throws SQLException {
        Timestamp validTo = resultSet.getTimestamp("valid_to");
        return new VersionRecord(resultSet.getLong("passport_id"), resultSet.getLong("version_id"),
                resultSet.getString("dpp_id"), resultSet.getString("product_id"), resultSet.getLong("version_no"),
                PostgresDppStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("valid_from").toInstant(), validTo == null ? null : validTo.toInstant(),
                resultSet.getTimestamp("stored_at").toInstant(), resultSet.getString("operation_id"),
                resultSet.getString("content_hash"));
    }

    private static final String VERSION_COLUMNS = """
            p.id as passport_id, v.id as version_id, p.dpp_id, p.product_id, v.version_no, v.status,
            v.valid_from, v.valid_to, v.stored_at, v.operation_id, v.content_hash
            """;

    public record VersionRecord(long passportId, long versionId, String dppId, String productId,
                                long versionNo, PostgresDppStatus status, Instant validFrom, Instant validTo,
                                Instant storedAt, String operationId, String contentHash) {
    }
}
