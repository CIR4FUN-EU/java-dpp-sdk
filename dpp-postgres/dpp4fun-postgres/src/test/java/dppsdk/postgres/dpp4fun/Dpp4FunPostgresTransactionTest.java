package dppsdk.postgres.dpp4fun;

import dppsdk.postgres.core.PostgresDppOperationContext;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Dpp4FunPostgresTransactionTest {

    @Test
    void doesNotReenableAutoCommitAfterRollbackFails() {
        AtomicBoolean autoCommitReenabled = new AtomicBoolean();
        Connection connection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getAutoCommit" -> true;
                    case "setAutoCommit" -> {
                        if (Boolean.TRUE.equals(args[0])) autoCommitReenabled.set(true);
                        yield null;
                    }
                    case "prepareStatement" -> throw new SQLException("write failed");
                    case "rollback" -> throw new SQLException("rollback failed");
                    case "toString" -> "failing connection";
                    default -> null;
                });
        DataSource dataSource = (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class}, (proxy, method, args) ->
                        method.getName().equals("getConnection") ? connection : null);

        Dpp4FunPostgresRepository repository = new Dpp4FunPostgresRepository(dataSource);
        assertThrows(IllegalStateException.class, () -> repository.create(TestDataFactory.validDpp(),
                new PostgresDppOperationContext("failed-create", Instant.parse("2026-01-01T00:00:00Z"))));

        assertFalse(autoCommitReenabled.get());
    }
}
