package dppsdk.postgres.dpp4fun;

import dppsdk.core.model.DppCore;
import dppsdk.dpp4fun.model.BillOfMaterials;
import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Component;
import dppsdk.dpp4fun.model.Dimensions;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.Material;
import dppsdk.dpp4fun.model.Part;
import dppsdk.dpp4fun.model.ProductClassification;
import dppsdk.postgres.core.PostgresDppTypeMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public final class Dpp4FunPostgresMapper implements PostgresDppTypeMapper<Dpp4Fun> {

    @Override
    public String passportType() {
        return "Dpp4Fun Furniture";
    }

    @Override
    public void insertVersionData(Connection connection, long versionId, Dpp4Fun dpp) throws SQLException {
        insertClassification(connection, versionId, dpp.getClassification());
        insertClassificationTags(connection, versionId, dpp.getTags());
        insertCharacteristics(connection, versionId, dpp.getCharacteristics());
        insertDimensions(connection, versionId, dpp.getDimensions());
        insertFeatures(connection, versionId, dpp.getFeatures());
        insertBillOfMaterials(connection, versionId, dpp.getBillOfMaterials());
    }

    @Override
    public Dpp4Fun readVersionData(Connection connection, long versionId, DppCore coreDpp) throws SQLException {
        ProductClassification classification = readClassification(connection, versionId);
        Characteristics characteristics = readCharacteristics(connection, versionId);
        BillOfMaterials billOfMaterials = readBillOfMaterials(connection, versionId);
        return new Dpp4Fun.Builder()
                .coreDpp(coreDpp)
                .classification(classification)
                .characteristics(characteristics)
                .billOfMaterials(billOfMaterials)
                .build();
    }

    private void insertClassification(Connection connection, long versionId, ProductClassification classification) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp4fun_classifications (version_id, sector, group_name, category, sub_category)
                values (?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, versionId);
            statement.setString(2, classification.getSector());
            statement.setString(3, classification.getGroup());
            statement.setString(4, classification.getCategory());
            statement.setString(5, classification.getSubCategory());
            statement.executeUpdate();
        }
    }

    private void insertClassificationTags(Connection connection, long versionId, List<String> tags) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp4fun_classification_tags (version_id, order_index, tag)
                values (?, ?, ?)
                """)) {
            for (int i = 0; i < tags.size(); i++) {
                statement.setLong(1, versionId);
                statement.setInt(2, i);
                statement.setString(3, tags.get(i));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertCharacteristics(Connection connection, long versionId, Characteristics characteristics) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp4fun_characteristics (
                    version_id,
                    product_name,
                    description,
                    brand,
                    product_type,
                    weight,
                    color
                ) values (?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, versionId);
            statement.setString(2, characteristics.getProductName());
            statement.setString(3, characteristics.getDescription());
            statement.setString(4, characteristics.getBrand());
            statement.setString(5, characteristics.getProductType());
            if (characteristics.getWeight() == null) {
                statement.setNull(6, Types.DOUBLE);
            } else {
                statement.setDouble(6, characteristics.getWeight());
            }
            statement.setString(7, characteristics.getColor());
            statement.executeUpdate();
        }
    }

    private void insertDimensions(Connection connection, long versionId, Dimensions dimensions) throws SQLException {
        if (dimensions == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp4fun_dimensions (version_id, width, height, depth, unit)
                values (?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, versionId);
            statement.setDouble(2, dimensions.getWidth());
            statement.setDouble(3, dimensions.getHeight());
            statement.setDouble(4, dimensions.getDepth());
            statement.setString(5, dimensions.getUnit());
            statement.executeUpdate();
        }
    }

    private void insertFeatures(Connection connection, long versionId, List<String> features) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp4fun_features (version_id, order_index, feature)
                values (?, ?, ?)
                """)) {
            for (int i = 0; i < features.size(); i++) {
                statement.setLong(1, versionId);
                statement.setInt(2, i);
                statement.setString(3, features.get(i));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertBillOfMaterials(Connection connection, long versionId, BillOfMaterials billOfMaterials) throws SQLException {
        if (billOfMaterials == null) {
            return;
        }
        try (PreparedStatement marker = connection.prepareStatement("""
                insert into dpp4fun_bill_of_materials (version_id)
                values (?)
                """)) {
            marker.setLong(1, versionId);
            marker.executeUpdate();
        }
        insertMaterials(connection, versionId, billOfMaterials.getMaterials());
        insertComponents(connection, versionId, billOfMaterials.getComponents());
        insertParts(connection, versionId, billOfMaterials.getParts());
    }

    private void insertMaterials(Connection connection, long versionId, List<Material> materials) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp4fun_materials (version_id, order_index, name, mandatory, portion, reference)
                values (?, ?, ?, ?, ?, ?)
                """)) {
            for (int i = 0; i < materials.size(); i++) {
                Material material = materials.get(i);
                statement.setLong(1, versionId);
                statement.setInt(2, i);
                statement.setString(3, material.getName());
                statement.setBoolean(4, material.isMandatory());
                statement.setDouble(5, material.getPortion());
                statement.setString(6, material.getReference());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertComponents(Connection connection, long versionId, List<Component> components) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp4fun_components (version_id, order_index, name, reference)
                values (?, ?, ?, ?)
                """)) {
            for (int i = 0; i < components.size(); i++) {
                Component component = components.get(i);
                statement.setLong(1, versionId);
                statement.setInt(2, i);
                statement.setString(3, component.getName());
                statement.setString(4, component.getReference());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertParts(Connection connection, long versionId, List<Part> parts) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp4fun_parts (version_id, order_index, name, mandatory, reference)
                values (?, ?, ?, ?, ?)
                """)) {
            for (int i = 0; i < parts.size(); i++) {
                Part part = parts.get(i);
                statement.setLong(1, versionId);
                statement.setInt(2, i);
                statement.setString(3, part.getName());
                statement.setBoolean(4, part.isMandatory());
                statement.setString(5, part.getReference());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private ProductClassification readClassification(Connection connection, long versionId) throws SQLException {
        ProductClassification.Builder builder = new ProductClassification.Builder();
        try (PreparedStatement statement = connection.prepareStatement("""
                select sector, group_name, category, sub_category
                from dpp4fun_classifications
                where version_id = ?
                """)) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Missing Dpp4Fun classification for version " + versionId);
                }
                builder.sector(resultSet.getString("sector"))
                        .group(resultSet.getString("group_name"))
                        .category(resultSet.getString("category"))
                        .subCategory(resultSet.getString("sub_category"));
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                select tag
                from dpp4fun_classification_tags
                where version_id = ?
                order by order_index
                """)) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    builder.addTag(resultSet.getString("tag"));
                }
            }
        }
        return builder.build();
    }

    private Characteristics readCharacteristics(Connection connection, long versionId) throws SQLException {
        Characteristics.Builder builder = new Characteristics.Builder();
        try (PreparedStatement statement = connection.prepareStatement("""
                select product_name, description, brand, product_type, weight, color
                from dpp4fun_characteristics
                where version_id = ?
                """)) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Missing Dpp4Fun characteristics for version " + versionId);
                }
                builder.productName(resultSet.getString("product_name"))
                        .description(resultSet.getString("description"))
                        .brand(resultSet.getString("brand"))
                        .productType(resultSet.getString("product_type"))
                        .weight(resultSet.getObject("weight", Double.class))
                        .color(resultSet.getString("color"));
            }
        }
        builder.dimensions(readDimensions(connection, versionId));
        try (PreparedStatement statement = connection.prepareStatement("""
                select feature
                from dpp4fun_features
                where version_id = ?
                order by order_index
                """)) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    builder.addFeature(resultSet.getString("feature"));
                }
            }
        }
        return builder.build();
    }

    private Dimensions readDimensions(Connection connection, long versionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select width, height, depth, unit
                from dpp4fun_dimensions
                where version_id = ?
                """)) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new Dimensions.Builder()
                        .width(resultSet.getDouble("width"))
                        .height(resultSet.getDouble("height"))
                        .depth(resultSet.getDouble("depth"))
                        .unit(resultSet.getString("unit"))
                        .build();
            }
        }
    }

    private BillOfMaterials readBillOfMaterials(Connection connection, long versionId) throws SQLException {
        try (PreparedStatement exists = connection.prepareStatement("""
                select 1 from dpp4fun_bill_of_materials where version_id = ?
                """)) {
            exists.setLong(1, versionId);
            try (ResultSet resultSet = exists.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
            }
        }

        BillOfMaterials.Builder builder = new BillOfMaterials.Builder();
        try (PreparedStatement statement = connection.prepareStatement("""
                select name, mandatory, portion, reference
                from dpp4fun_materials
                where version_id = ?
                order by order_index
                """)) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    builder.addMaterial(new Material.Builder()
                            .name(resultSet.getString("name"))
                            .mandatory(resultSet.getBoolean("mandatory"))
                            .portion(resultSet.getDouble("portion"))
                            .reference(resultSet.getString("reference"))
                            .build());
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                select name, reference
                from dpp4fun_components
                where version_id = ?
                order by order_index
                """)) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    builder.addComponent(new Component.Builder()
                            .name(resultSet.getString("name"))
                            .reference(resultSet.getString("reference"))
                            .build());
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                select name, mandatory, reference
                from dpp4fun_parts
                where version_id = ?
                order by order_index
                """)) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    builder.addPart(new Part.Builder()
                            .name(resultSet.getString("name"))
                            .mandatory(resultSet.getBoolean("mandatory"))
                            .reference(resultSet.getString("reference"))
                            .build());
                }
            }
        }
        return builder.build();
    }
}
