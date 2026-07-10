package dppsdk.postgres.dpp4fun;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Dpp4FunPostgresSurfaceTest {

    @Test
    @DisplayName("Dpp4Fun PostgreSQL classes exist in the planned package")
    void dpp4FunPostgresClassesExist() {
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.dpp4fun.Dpp4FunPostgresRepository"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.dpp4fun.Dpp4FunPostgresMapper"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.dpp4fun.Dpp4FunQueryRepository"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.dpp4fun.Dpp4FunVersionSummary"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.dpp4fun.Dpp4FunSearchCriteria"));
        assertDoesNotThrow(() -> Class.forName("dppsdk.postgres.dpp4fun.Dpp4FunSearchResult"));
    }
}
