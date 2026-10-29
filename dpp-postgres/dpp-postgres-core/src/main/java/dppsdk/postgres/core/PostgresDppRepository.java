package dppsdk.postgres.core;

import dppsdk.core.model.DigitalProductPassport;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** DataSource-backed generic repository for complete canonical DPP snapshots. */
public final class PostgresDppRepository {

    private final DataSource dataSource;
    private final PostgresDppCoreMapper mapper;
    private final PostgresDppVersionRepositorySupport versions;

    public PostgresDppRepository(DataSource dataSource) {
        this(dataSource, new PostgresDppCoreMapper());
    }

    /** Binds this canonical dataset to one domain profile before application seeding or writes. */
    public void initializeRepositoryProfile(String configuredProfile) {
        Objects.requireNonNull(configuredProfile, "configuredProfile must not be null");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int inserted;
                try (var statement = connection.prepareStatement("""
                        insert into dpp_repository_profile (singleton, repository_profile)
                        values (true, ?) on conflict (singleton) do nothing
                        """)) {
                    statement.setString(1, configuredProfile);
                    inserted = statement.executeUpdate();
                }
                if (inserted == 1 && hasPersistedDppData(connection)) {
                    throw new IllegalStateException("PostgreSQL repository contains persisted DPP history but has no "
                            + "profile marker. Explicit legacy repository migration or recreation is required.");
                }
                String storedProfile;
                try (var statement = connection.prepareStatement(
                        "select repository_profile from dpp_repository_profile where singleton = true");
                     var resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) throw new IllegalStateException("Repository profile marker initialization failed");
                    storedProfile = resultSet.getString(1);
                }
                if (!storedProfile.equals(configuredProfile)) {
                    throw new IllegalStateException("Repository was initialized for profile '" + storedProfile
                            + "' but configuration requests '" + configuredProfile
                            + "'. Explicit repository migration or recreation is required.");
                }
                connection.commit();
            } catch (SQLException | RuntimeException | Error failure) {
                try { connection.rollback(); } catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
                throw failure;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to verify PostgreSQL repository profile", exception);
        }
    }

    private boolean hasPersistedDppData(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("select exists(select 1 from dpp_passports)")) {
            try (var resultSet = statement.executeQuery()) { resultSet.next(); return resultSet.getBoolean(1); }
        }
    }

    PostgresDppRepository(DataSource dataSource, PostgresDppCoreMapper mapper) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.versions = new PostgresDppVersionRepositorySupport(mapper);
        PostgresJdbcSupport.initializeSchema(dataSource, "/postgres/core-schema.sql");
    }

    public PostgresDppVersionRepositorySupport.VersionRecord create(
            DigitalProductPassport dpp, PostgresDppOperationContext context) {
        return transact("create DPP", connection -> versions.create(connection, dpp, context));
    }

    public Optional<DigitalProductPassport> findCurrentByDppId(String dppId) {
        return query("find current DPP by DPP ID", connection -> readVersion(
                connection, versions.findCurrentByDppId(connection, dppId)));
    }

    /** Reads the current canonical passport and its optimistic-write version using one connection. */
    public Optional<PostgresDppSnapshot> findCurrentSnapshotByDppId(String dppId) {
        return query("find current DPP snapshot", connection -> versions.findCurrentByDppId(connection, dppId)
                .map(version -> {
                    try {
                        return new PostgresDppSnapshot(mapper.readVersionData(connection, version.versionId()),
                                version.versionNo());
                    } catch (SQLException exception) {
                        throw new IllegalStateException("Failed to read current DPP snapshot " + dppId, exception);
                    }
                }));
    }

    public Optional<DigitalProductPassport> findCurrentByProductId(String productId) {
        return query("find current DPP by product ID", connection -> readVersion(
                connection, versions.findCurrentByProductId(connection, productId)));
    }

    public Optional<DigitalProductPassport> findVersion(String dppId, long versionNo) {
        return query("find DPP version", connection -> readVersion(
                connection, versions.findVersionByDppId(connection, dppId, versionNo)));
    }

    public Optional<DigitalProductPassport> findByDppIdAt(String dppId, Instant timestamp) {
        return query("find DPP at timestamp", connection -> readVersion(
                connection, versions.findByDppIdAt(connection, dppId, timestamp)));
    }

    public Optional<DigitalProductPassport> findByProductIdAt(String productId, Instant timestamp) {
        return query("find DPP by product ID at timestamp", connection -> readVersion(
                connection, versions.findByProductIdAt(connection, productId, timestamp)));
    }

    public List<PostgresDppVersionRepositorySupport.VersionRecord> findHistoryByDppId(String dppId) {
        return query("find DPP history", connection -> versions.findHistoryByDppId(connection, dppId));
    }

    /**
     * Appends a semantic revision, or returns the current revision unchanged when codec output is identical.
     */
    public PostgresDppVersionRepositorySupport.VersionRecord appendVersion(
            DigitalProductPassport dpp, long expectedCurrentVersion, PostgresDppOperationContext context) {
        return transact("append DPP version",
                connection -> versions.appendVersion(connection, dpp, expectedCurrentVersion, context));
    }

    public Optional<Long> findCurrentVersionNoByDppId(String dppId) {
        return query("find current DPP version number",
                connection -> versions.findCurrentVersionNoByDppId(connection, dppId));
    }

    public boolean existsActiveByDppId(String dppId) {
        return query("check active DPP", connection -> versions.existsActiveByDppId(connection, dppId));
    }

    public boolean existsAnyByDppId(String dppId) {
        return query("check known DPP", connection -> versions.existsAnyByDppId(connection, dppId));
    }

    public List<String> findAllActiveDppIds() {
        return query("find active DPP IDs", versions::findAllActiveDppIds);
    }

    public DppPage<String> findActiveDppIds(DppPageRequest request) {
        return query("find active DPP IDs page", connection -> versions.findActiveDppIds(connection, request));
    }

    public DppPage<String> findActiveDppIdsByProductIds(List<String> productIds, DppPageRequest request) {
        return query("find active DPP IDs by product IDs",
                connection -> versions.findActiveDppIdsByProductIds(connection, productIds, request));
    }

    public PostgresDppVersionRepositorySupport.VersionRecord softDelete(
            String dppId, long expectedCurrentVersion, Instant deletedAt) {
        return transact("soft delete DPP",
                connection -> versions.softDelete(connection, dppId, expectedCurrentVersion, deletedAt));
    }

    private Optional<DigitalProductPassport> readVersion(Connection connection,
            Optional<PostgresDppVersionRepositorySupport.VersionRecord> version) throws SQLException {
        if (version.isEmpty()) return Optional.empty();
        return Optional.of(mapper.readVersionData(connection, version.get().versionId()));
    }

    private <T> T query(String operation, SqlWork<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            return work.run(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to " + operation, exception);
        }
    }

    private <T> T transact(String operation, SqlWork<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException | Error failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                if (failure instanceof SQLException sqlFailure) {
                    throw new IllegalStateException("Failed to " + operation, sqlFailure);
                }
                throw failure;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to " + operation, exception);
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }
}
