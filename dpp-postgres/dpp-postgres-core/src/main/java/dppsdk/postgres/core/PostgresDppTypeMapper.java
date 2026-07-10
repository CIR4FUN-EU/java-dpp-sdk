package dppsdk.postgres.core;

import dppsdk.core.model.Dpp;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Contract for a grouped PostgreSQL mapper that writes and reconstructs one DPP subtype.
 *
 * <p>Implementations are persistence-focused only. Validation, JSON handling, and patch logic stay outside
 * the PostgreSQL modules.</p>
 */
public interface PostgresDppTypeMapper<T extends Dpp> {

    String passportType();

    void insertVersionData(Connection connection, long versionId, T dpp) throws SQLException;

    T readVersionData(Connection connection, long versionId, dppsdk.core.model.DppCore coreDpp) throws SQLException;
}
