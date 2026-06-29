package dppsdk.postgres.core;

import dppsdk.core.model.Dpp;
import java.sql.Connection;
import java.sql.SQLException;

public interface PostgresDppTypeMapper<T extends Dpp> {

    String passportType();

    void insertVersionData(Connection connection, long versionId, T dpp) throws SQLException;

    T readVersionData(Connection connection, long versionId, dppsdk.core.model.DppCore coreDpp) throws SQLException;
}
