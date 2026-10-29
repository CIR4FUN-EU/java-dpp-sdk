package dppsdk.postgres.dpp4fun;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class PostgresSchemaInitializationOrderTest {

    @Test
    void initializesCoreProjectionTableEventsAndProfileIndexesInDependencyOrder() {
        List<String> statements = new ArrayList<>();
        Statement statement = proxy(Statement.class, (method, args) -> {
            if (method.equals("execute")) {
                statements.add((String) args[0]);
                return false;
            }
            return defaultValue(Statement.class, method);
        });
        Connection connection = proxy(Connection.class, (method, args) ->
                method.equals("createStatement") ? statement : defaultValue(Connection.class, method));
        DataSource dataSource = proxy(DataSource.class, (method, args) ->
                method.equals("getConnection") ? connection : defaultValue(DataSource.class, method));

        new Dpp4FunPostgresRepository(dataSource).initializeSchema();

        int core = indexContaining(statements, "create table if not exists dpp_passports");
        int projectionTable = indexContaining(statements, "create table if not exists dpp_projections");
        int events = indexContaining(statements, "create table if not exists dpp_lifecycle_events");
        int profileIndex = indexContaining(statements, "ix_dpp_proj_dpp4fun_brand");
        assertTrue(core < projectionTable, "core schema must precede projection schema");
        assertTrue(projectionTable < events, "projection table must precede event schema");
        assertTrue(events < profileIndex, "event schema must precede Dpp4Fun projection indexes");
    }

    private static int indexContaining(List<String> statements, String expected) {
        for (int i = 0; i < statements.size(); i++) {
            if (statements.get(i).contains(expected)) {
                return i;
            }
        }
        throw new AssertionError("No SQL statement contained: " + expected);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, ProxyCall call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) ->
                call.invoke(method.getName(), args == null ? new Object[0] : args));
    }

    private static Object defaultValue(Class<?> type, String method) {
        if (method.equals("toString")) return type.getSimpleName() + " proxy";
        if (method.equals("hashCode")) return System.identityHashCode(type);
        if (method.equals("equals")) return false;
        return null;
    }

    @FunctionalInterface
    private interface ProxyCall {
        Object invoke(String method, Object[] args);
    }
}
