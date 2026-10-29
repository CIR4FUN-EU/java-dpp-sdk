package dppsdk.postgres.projection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Pure deterministic generator for the shared current projection table and declared indexes. */
public final class PostgresProjectionDdlGenerator {
    public String createTableDdl() {
        return """
                create table if not exists dpp_projections (
                    projection_name varchar(63) not null,
                    passport_id bigint not null references dpp_passports(id) on delete cascade,
                    version_id bigint not null references dpp_versions(id) on delete cascade,
                    data jsonb not null,
                    updated_at timestamptz not null default now(),
                    primary key (projection_name, passport_id)
                )
                """.strip();
    }

    public String generate(DppProjectionDefinition definition) {
        return String.join(";\n", definition.indexes().stream().map(index -> indexDdl(definition, index)).toList());
    }

    public String indexDdl(DppProjectionDefinition definition, DppProjectionIndex index) {
        String name = quoteIdentifier(indexName(definition.name(), index));
        String projection = sqlLiteral(definition.name());
        String field = sqlLiteral(index.fieldName());
        String expression;
        if (index.indexType() == DppProjectionIndexType.GIN) {
            expression = "(data -> '" + field + "')";
        } else {
            expression = switch (index.valueType()) {
                case TEXT -> "(data ->> '" + field + "')";
                case NUMBER -> "(case when jsonb_typeof(data -> '" + field + "') = 'number' then (data ->> '" + field + "')::numeric end)";
                case BOOLEAN -> "(case when jsonb_typeof(data -> '" + field + "') = 'boolean' then (data ->> '" + field + "')::boolean end)";
            };
        }
        String method = index.indexType() == DppProjectionIndexType.GIN ? "gin" : "btree";
        return "create index if not exists " + name + " on dpp_projections using " + method + " (" + expression + ")"
                + " where projection_name = '" + projection + "'";
    }

    private static String indexName(String projection, DppProjectionIndex index) {
        String valueType = index.valueType() == null ? "" : "_" + index.valueType().name().toLowerCase(java.util.Locale.ROOT);
        String identity = projection + "\u0000" + index.fieldName() + "\u0000" + index.indexType() + "\u0000" + index.valueType();
        String prefix = ("ix_dpp_proj_" + projection + "_" + index.fieldName() + "_"
                + index.indexType().name().toLowerCase(java.util.Locale.ROOT) + valueType)
                .toLowerCase(java.util.Locale.ROOT);
        try {
            // The suffix preserves case-sensitive identity, projection/field boundaries, and
            // BTREE value type; PostgreSQL's lower-case identifiers alone cannot distinguish them.
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
            String readable = prefix.substring(0, Math.min(prefix.length(), 46));
            return readable + "_" + hash;
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable", impossible); }
    }

    private static String quoteIdentifier(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private static String sqlLiteral(String value) { return value.replace("'", "''"); }
}
