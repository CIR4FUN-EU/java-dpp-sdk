package dppsdk.postgres.projection;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class PostgresProjectionDdlGeneratorTest {
    private final PostgresProjectionDdlGenerator generator = new PostgresProjectionDdlGenerator();

    @Test void emitsDeterministicScopedTextNumericAndGinIndexes() {
        DppProjectionDefinition definition = DppProjectionDefinition.builder("catalog")
                .extract(ignored -> DppProjectionDocument.builder().build())
                .btree("brand", DppProjectionValueType.TEXT)
                .btree("metric", DppProjectionValueType.NUMBER)
                .gin("tags").build();
        String first = generator.generate(definition);
        assertEquals(first, generator.generate(definition));
        assertTrue(first.contains("using btree ((data ->> 'brand')) where projection_name = 'catalog'"));
        assertTrue(first.contains("jsonb_typeof(data -> 'metric') = 'number'"));
        assertTrue(first.contains("using gin ((data -> 'tags')) where projection_name = 'catalog'"));
        assertTrue(first.contains("ix_dpp_proj_catalog_brand_btree"));
    }

    @Test void rejectsUnsafeAndDuplicateDeclarationsAndBoundsIndexNames() {
        assertThrows(IllegalArgumentException.class, () -> DppProjectionDefinition.builder("catalog;drop").extract(ignored -> DppProjectionDocument.builder().build()));
        assertThrows(IllegalArgumentException.class, () -> DppProjectionDefinition.builder("catalog").extract(ignored -> DppProjectionDocument.builder().build()).btree("brand' || true --", DppProjectionValueType.TEXT));
        assertThrows(IllegalArgumentException.class, () -> DppProjectionDefinition.builder("catalog").extract(ignored -> DppProjectionDocument.builder().build()).gin("tags").gin("tags"));
        var longDefinition = DppProjectionDefinition.builder("p".repeat(30)).extract(ignored -> DppProjectionDocument.builder().build()).gin("f".repeat(30)).build();
        String ddl = generator.generate(longDefinition);
        String identifier = ddl.substring("create index if not exists \"".length(), ddl.indexOf('"', "create index if not exists \"".length()));
        assertTrue(identifier.length() <= 63);
        assertTrue(ddl.startsWith("create index if not exists \""));
    }

    @Test void indexNamesDistinguishCaseSensitiveFieldsAndBtreeValueTypes() {
        var lower = DppProjectionDefinition.builder("catalog").extract(ignored -> DppProjectionDocument.builder().build())
                .btree("brand", DppProjectionValueType.TEXT).build();
        var upper = DppProjectionDefinition.builder("catalog").extract(ignored -> DppProjectionDocument.builder().build())
                .btree("Brand", DppProjectionValueType.TEXT).build();
        var numeric = DppProjectionDefinition.builder("catalog").extract(ignored -> DppProjectionDocument.builder().build())
                .btree("brand", DppProjectionValueType.NUMBER).build();
        var joinedProjection = DppProjectionDefinition.builder("catalog_a").extract(ignored -> DppProjectionDocument.builder().build())
                .btree("b", DppProjectionValueType.TEXT).build();
        var joinedField = DppProjectionDefinition.builder("catalog").extract(ignored -> DppProjectionDocument.builder().build())
                .btree("a_b", DppProjectionValueType.TEXT).build();

        assertNotEquals(indexName(lower), indexName(upper));
        assertNotEquals(indexName(lower), indexName(numeric));
        assertNotEquals(indexName(joinedProjection), indexName(joinedField));
    }

    private String indexName(DppProjectionDefinition definition) {
        String ddl = generator.indexDdl(definition, definition.indexes().get(0));
        return ddl.substring("create index if not exists \"".length(), ddl.indexOf('"', "create index if not exists \"".length()));
    }

    @Test void schemaManagerConstructionDoesNotConnectOrApplyDdl() {
        int[] connections = {0};
        DataSource dataSource = (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{DataSource.class}, (proxy, method, args) -> {
            if (method.getName().equals("getConnection")) connections[0]++;
            return null;
        });
        new PostgresProjectionSchemaManager(dataSource);
        new PostgresDppProjectionRepository(dataSource);
        assertEquals(0, connections[0]);
    }

    @Test void projectionDocumentsRejectUnknownJavaObjectGraphsAndRemainImmutable() {
        assertThrows(IllegalArgumentException.class, () -> DppProjectionDocument.builder().put("value", new Object()));
        var document = DppProjectionDocument.builder().text("label", "one").number("metric", 3).strings("tags", java.util.List.of("x")).build();
        assertEquals("one", document.get("label"));
        assertThrows(UnsupportedOperationException.class, () -> document.values().put("extra", true));
        var nullableList = DppProjectionDocument.builder().strings("tags", java.util.Arrays.asList("x", null)).build();
        assertEquals(java.util.Arrays.asList("x", null), nullableList.get("tags"));
    }
}
