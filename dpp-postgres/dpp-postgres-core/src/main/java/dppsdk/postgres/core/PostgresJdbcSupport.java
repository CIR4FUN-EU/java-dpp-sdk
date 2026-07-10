package dppsdk.postgres.core;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class PostgresJdbcSupport {

    private PostgresJdbcSupport() {
    }

    public static void initializeSchema(DataSource dataSource, String resourcePath) {
        try (Connection connection = dataSource.getConnection()) {
            executeSqlScript(connection, resourcePath);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize schema from " + resourcePath, exception);
        }
    }

    public static void executeSqlScript(Connection connection, String resourcePath) {
        try (InputStream stream = PostgresJdbcSupport.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing SQL resource " + resourcePath);
            }
            String script = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement statement = connection.createStatement()) {
                statement.execute(script);
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Failed to execute SQL resource " + resourcePath, exception);
        }
    }

    static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    static Long getNullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
