package dppsdk.postgres.dpp4fun;

import static org.junit.jupiter.api.Assertions.*;
import dppsdk.support.TestDataFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class Dpp4FunPostgresMapperTest {
    @Test void projectsTypedFieldsWithoutCreatingAnotherDomainGraph() {
        Dpp4FunPostgresMapper mapper = new Dpp4FunPostgresMapper();
        var dpp = TestDataFactory.validDpp().toBuilder()
                .nameplate(dppsdk.dpp4fun.model.aspect.Nameplate.builder().brand("Oak").build()).build();
        var document = mapper.project(dpp);
        assertEquals(dpp.getDigitalProductPassportId(), document.get("dppId"));
        assertEquals(dpp.getUniqueProductIdentifier(), document.get("productId"));
        assertEquals("Oak", document.get("brand"));
        assertInstanceOf(java.util.List.class, document.get("materials"));
        assertEquals(List.of("brand", "materials", "components", "parts"),
                mapper.projectionDefinition().indexes().stream().map(index -> index.fieldName()).toList());
    }

    @Test void builtInSchemaResourceUsesTheGeneratedIndexNames() throws IOException {
        var mapper = new Dpp4FunPostgresMapper();
        String schema;
        try (var input = getClass().getResourceAsStream("/postgres/dpp4fun-schema.sql")) {
            assertNotNull(input);
            schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var generator = new dppsdk.postgres.projection.PostgresProjectionDdlGenerator();
        var definition = mapper.projectionDefinition();
        assertEquals(definition.indexes().size(), schema.lines().filter(line -> line.startsWith("create index if not exists")).count());
        for (var index : definition.indexes()) {
            String generated = generator.indexDdl(definition, index);
            String name = generated.substring("create index if not exists \"".length(),
                    generated.indexOf('"', "create index if not exists \"".length()));
            assertTrue(schema.contains(name), "missing generated index name " + name);
        }
    }
}
