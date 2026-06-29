package dppsdk.postgres.dpp4fun;

import dppsdk.postgres.core.DppPage;
import dppsdk.postgres.core.DppPageRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class Dpp4FunQueryRepository {

    public DppPage<String> findActiveDppIdsByProductIds(Connection connection, List<String> productIds, DppPageRequest pageRequest) throws SQLException {
        int start = pageRequest.cursor() == null || pageRequest.cursor().isBlank()
                ? 0
                : Integer.parseInt(pageRequest.cursor());
        List<String> items = new ArrayList<>();
        int index = start;
        while (index < productIds.size() && items.size() < pageRequest.limit()) {
            String productId = productIds.get(index);
            findActiveDppIdByProductId(connection, productId).ifPresent(items::add);
            index++;
        }
        String nextCursor = index < productIds.size() ? Integer.toString(index) : null;
        return new DppPage<>(items, nextCursor);
    }

    public List<Dpp4FunSearchResult> search(Connection connection, Dpp4FunSearchCriteria criteria) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                select distinct
                    p.dpp_id,
                    p.product_id,
                    v.version_no,
                    c.sector,
                    c.category,
                    ch.brand,
                    ch.product_type,
                    ch.product_name
                from dpp_passports p
                join dpp_versions v on v.passport_id = p.id and v.status = 'ACTIVE'
                join dpp4fun_classifications c on c.version_id = v.id
                join dpp4fun_characteristics ch on ch.version_id = v.id
                left join dpp4fun_materials m on m.version_id = v.id
                left join dpp4fun_components cmp on cmp.version_id = v.id
                left join dpp4fun_parts pt on pt.version_id = v.id
                where 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();
        appendFilter(sql, parameters, "c.sector = ?", criteria.sector());
        appendFilter(sql, parameters, "c.category = ?", criteria.category());
        appendFilter(sql, parameters, "ch.brand = ?", criteria.brand());
        appendFilter(sql, parameters, "ch.product_type = ?", criteria.productType());
        appendFilter(sql, parameters, "m.name = ?", criteria.materialName());
        appendFilter(sql, parameters, "cmp.name = ?", criteria.componentName());
        appendFilter(sql, parameters, "pt.name = ?", criteria.partName());
        sql.append(" order by p.dpp_id");
        sql.append(" limit ? offset ?");
        parameters.add(criteria.limitOrDefault());
        parameters.add(criteria.offsetOrDefault());

        List<Dpp4FunSearchResult> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new Dpp4FunSearchResult(
                            resultSet.getString("dpp_id"),
                            resultSet.getString("product_id"),
                            resultSet.getLong("version_no"),
                            resultSet.getString("sector"),
                            resultSet.getString("category"),
                            resultSet.getString("brand"),
                            resultSet.getString("product_type"),
                            resultSet.getString("product_name")
                    ));
                }
            }
        }
        return results;
    }

    private java.util.Optional<String> findActiveDppIdByProductId(Connection connection, String productId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select p.dpp_id
                from dpp_passports p
                join dpp_versions v on v.passport_id = p.id
                where p.product_id = ? and v.status = 'ACTIVE'
                """)) {
            statement.setString(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return java.util.Optional.empty();
                }
                return java.util.Optional.of(resultSet.getString("dpp_id"));
            }
        }
    }

    private void appendFilter(StringBuilder sql, List<Object> parameters, String condition, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        sql.append(" and ").append(condition);
        parameters.add(value);
    }
}
