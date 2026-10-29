package dppsdk.postgres.projection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/** Explicitly applies generated projection table and index DDL. Construction never changes the database. */
public final class PostgresProjectionSchemaManager {
    private final DataSource dataSource;
    private final PostgresProjectionDdlGenerator generator;
    public PostgresProjectionSchemaManager(DataSource dataSource) { this(dataSource, new PostgresProjectionDdlGenerator()); }
    public PostgresProjectionSchemaManager(DataSource dataSource, PostgresProjectionDdlGenerator generator) {
        this.dataSource = Objects.requireNonNull(dataSource); this.generator = Objects.requireNonNull(generator);
    }
    public void apply(DppProjectionDefinition definition) {
        try (Connection connection = dataSource.getConnection()) { apply(connection, definition); }
        catch (SQLException exception) { throw new IllegalStateException("Failed to apply projection schema " + definition.name(), exception); }
    }
    public void apply(Connection connection, DppProjectionDefinition definition) throws SQLException {
        createTable(connection);
        applyIndexes(connection, definition);
    }
    /** Creates the shared projection table before optional event/profile schemas are initialized. */
    public void createTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.execute(generator.createTableDdl()); }
    }
    /** Applies the indexes declared by a definition after its required schemas exist. */
    public void applyIndexes(Connection connection, DppProjectionDefinition definition) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (var index : definition.indexes()) statement.execute(generator.indexDdl(definition, index));
        }
    }
}
