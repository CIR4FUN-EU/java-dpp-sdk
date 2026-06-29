package dppsdk.postgres.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostgresCoreSurfaceTest {

    @Test
    @DisplayName("core PostgreSQL support classes exist in the planned package")
    void coreSupportClassesExist() {
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.core.PostgresDppOperationContext"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.core.PostgresDppStatus"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.core.DppLifecycleEventType"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.core.DppLifecycleEventRecord"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.core.DppPage"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.core.DppPageRequest"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.core.PostgresDppTypeMapper"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.core.PostgresDppCoreMapper"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.core.PostgresLifecycleEventRepository"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.core.PostgresDppVersionRepositorySupport"));
    }
}
