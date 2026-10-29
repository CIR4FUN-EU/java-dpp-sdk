package dppsdk.postgres.dpp4fun;

import dppsdk.postgres.core.DppPage;
import dppsdk.postgres.core.DppPageRequest;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Dpp4Fun-specific typed searches over the generic current projection table. */
public final class Dpp4FunQueryRepository {
    public DppPage<String> findActiveDppIdsByProductIds(Connection connection, List<String> productIds, DppPageRequest pageRequest) throws SQLException {
        int start = pageRequest.cursor() == null ? 0 : Integer.parseInt(pageRequest.cursor());
        if (start >= productIds.size()) return new DppPage<>(List.of(), null);
        List<String> remaining = productIds.subList(start, productIds.size());
        List<String> items = new ArrayList<>();
        String cursor = null;
        Array productArray = connection.createArrayOf("varchar", remaining.toArray(String[]::new));
        try (PreparedStatement statement = connection.prepareStatement("""
                select input.order_index, p.dpp_id
                from unnest(cast(? as varchar[])) with ordinality as input(product_id, order_index)
                join dpp_passports p on p.product_id = input.product_id and p.deleted_at is null
                join dpp_versions v on v.passport_id = p.id and v.status = 'ACTIVE' and v.valid_to is null
                order by input.order_index
                limit ?
                """)) {
            statement.setArray(1, productArray); statement.setInt(2, pageRequest.limit());
            try (ResultSet rs = statement.executeQuery()) { while (rs.next()) { items.add(rs.getString("dpp_id")); cursor = Integer.toString(start + rs.getInt("order_index")); } }
        } finally { productArray.free(); }
        if (items.size() < pageRequest.limit()) cursor = null;
        return new DppPage<>(items, cursor);
    }

    public List<Dpp4FunSearchResult> search(Connection connection, Dpp4FunSearchCriteria criteria) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                select p.dpp_id, p.product_id, v.version_no,
                       pr.data ->> 'brand' as brand
                from dpp_projections pr
                join dpp_passports p on p.id = pr.passport_id and p.deleted_at is null
                join dpp_versions v on v.id = pr.version_id and v.passport_id = p.id and v.status = 'ACTIVE' and v.valid_to is null
                where pr.projection_name = 'dpp4fun'
                """);
        List<Object> parameters = new ArrayList<>();
        scalarFilter(sql, parameters, "brand", criteria.brand());
        arrayFilter(sql, parameters, "materials", criteria.materialName());
        arrayFilter(sql, parameters, "components", criteria.componentName());
        arrayFilter(sql, parameters, "parts", criteria.partName());
        sql.append(" order by p.dpp_id limit ? offset ?");
        parameters.add(criteria.limitOrDefault()); parameters.add(criteria.offsetOrDefault());
        List<Dpp4FunSearchResult> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) statement.setObject(i + 1, parameters.get(i));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) results.add(new Dpp4FunSearchResult(rs.getString("dpp_id"), rs.getString("product_id"),
                        rs.getLong("version_no"), rs.getString("brand")));
            }
        }
        return results;
    }

    private void scalarFilter(StringBuilder sql, List<Object> parameters, String key, String value) {
        if (value != null && !value.isBlank()) { sql.append(" and pr.data ->> '").append(key).append("' = ?"); parameters.add(value); }
    }
    private void arrayFilter(StringBuilder sql, List<Object> parameters, String key, String value) {
        if (value != null && !value.isBlank()) {
            sql.append(" and (pr.data -> '").append(key).append("') @> jsonb_build_array(?)");
            parameters.add(value);
        }
    }
}
