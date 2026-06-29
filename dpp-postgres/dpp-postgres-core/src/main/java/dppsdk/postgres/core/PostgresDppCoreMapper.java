package dppsdk.postgres.core;

import dppsdk.core.model.Address;
import dppsdk.core.model.Contact;
import dppsdk.core.model.Documentation;
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Email;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.model.PassportMetadata;
import dppsdk.core.model.Telephone;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Relational mapper for reusable {@code DppCore} data and its nested submodels.
 *
 * <p>This mapper is shared by PostgreSQL repositories for concrete DPP types.</p>
 */
public final class PostgresDppCoreMapper {

    public void insertVersionData(Connection connection, long versionId, DppCore coreDpp) throws SQLException {
        insertPassportMetadata(connection, versionId, coreDpp.getPassportMetadata());
        insertPassportUpdateDates(connection, versionId, coreDpp.getPassportUpdateDates());
        insertNameplate(connection, versionId, coreDpp.getNameplate());
        insertDocumentation(connection, versionId, coreDpp.getDocumentation());
    }

    public DppCore readVersionData(Connection connection, long versionId) throws SQLException {
        PassportMetadata passportMetadata = readPassportMetadata(connection, versionId);
        Nameplate nameplate = readNameplate(connection, versionId);
        Documentation documentation = readDocumentation(connection, versionId);
        return new DppCore.Builder()
                .passportMetadata(passportMetadata)
                .nameplate(nameplate)
                .documentation(documentation)
                .build();
    }

    private void insertPassportMetadata(Connection connection, long versionId, PassportMetadata metadata) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_passport_metadata (
                    version_id,
                    unique_product_identifier,
                    qr_code_or_digital_tag,
                    external_documentation_link
                ) values (?, ?, ?, ?)
                """)) {
            statement.setLong(1, versionId);
            statement.setObject(2, metadata.getUniqueProductIdentifier());
            statement.setString(3, metadata.getQrCodeOrDigitalTag());
            statement.setString(4, metadata.getExternalDocumentationLink());
            statement.executeUpdate();
        }
    }

    private void insertPassportUpdateDates(Connection connection, long versionId, List<LocalDate> updateDates) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_passport_update_dates (version_id, order_index, update_date)
                values (?, ?, ?)
                """)) {
            for (int i = 0; i < updateDates.size(); i++) {
                statement.setLong(1, versionId);
                statement.setInt(2, i);
                statement.setDate(3, Date.valueOf(updateDates.get(i)));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertNameplate(Connection connection, long versionId, Nameplate nameplate) throws SQLException {
        Long manufacturerId = insertOrganization(connection, versionId, "MANUFACTURER", nameplate.getManufacturer());
        Long supplierId = insertOrganization(connection, versionId, "SUPPLIER", nameplate.getSupplier());

        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_nameplates (
                    version_id,
                    gtin_code,
                    internal_article_number,
                    batch_number,
                    customs_tariff_number,
                    uri_of_the_product,
                    manufacturer_organization_id,
                    supplier_organization_id
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, versionId);
            statement.setString(2, nameplate.getGtinCode());
            statement.setString(3, nameplate.getInternalArticleNumber());
            statement.setString(4, nameplate.getBatchNumber());
            statement.setString(5, nameplate.getCustomsTariffNumber());
            statement.setString(6, nameplate.getUriOfTheProduct());
            PostgresJdbcSupport.setNullableLong(statement, 7, manufacturerId);
            PostgresJdbcSupport.setNullableLong(statement, 8, supplierId);
            statement.executeUpdate();
        }
    }

    private Long insertOrganization(Connection connection, long versionId, String slot, Organization organization) throws SQLException {
        if (organization == null) {
            return null;
        }

        Long contactId = insertContact(connection, versionId, organization.getContact());
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_organizations (
                    version_id,
                    slot_name,
                    name,
                    gln,
                    product_description,
                    product_designation,
                    product_family,
                    product_root,
                    product_order_suffix,
                    uri,
                    role,
                    contact_id
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                returning id
                """)) {
            statement.setLong(1, versionId);
            statement.setString(2, slot);
            statement.setString(3, organization.getName());
            statement.setString(4, organization.getGln());
            statement.setString(5, organization.getProductDescription());
            statement.setString(6, organization.getProductDesignation());
            statement.setString(7, organization.getProductFamily());
            statement.setString(8, organization.getProductRoot());
            statement.setString(9, organization.getProductOrderSuffix());
            statement.setString(10, organization.getUri());
            statement.setString(11, organization.getRole() == null ? null : organization.getRole().name());
            PostgresJdbcSupport.setNullableLong(statement, 12, contactId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private Long insertContact(Connection connection, long versionId, Contact contact) throws SQLException {
        if (contact == null) {
            return null;
        }

        Long addressId = insertAddress(connection, versionId, contact.getAddress());
        Long emailId = insertEmail(connection, versionId, contact.getEmail());
        Long telephoneId = insertTelephone(connection, versionId, contact.getTelephone());

        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_contacts (
                    version_id,
                    organization_name,
                    address_id,
                    email_id,
                    telephone_id
                ) values (?, ?, ?, ?, ?)
                returning id
                """)) {
            statement.setLong(1, versionId);
            statement.setString(2, contact.getOrganization());
            PostgresJdbcSupport.setNullableLong(statement, 3, addressId);
            PostgresJdbcSupport.setNullableLong(statement, 4, emailId);
            PostgresJdbcSupport.setNullableLong(statement, 5, telephoneId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private Long insertAddress(Connection connection, long versionId, Address address) throws SQLException {
        if (address == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_addresses (version_id, country, zip_code, region, town, street)
                values (?, ?, ?, ?, ?, ?)
                returning id
                """)) {
            statement.setLong(1, versionId);
            statement.setString(2, address.getCountry());
            statement.setString(3, address.getZipCode());
            statement.setString(4, address.getRegion());
            statement.setString(5, address.getTown());
            statement.setString(6, address.getStreet());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private Long insertEmail(Connection connection, long versionId, Email email) throws SQLException {
        if (email == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_emails (version_id, email_address, type_of_email)
                values (?, ?, ?)
                returning id
                """)) {
            statement.setLong(1, versionId);
            statement.setString(2, email.getEmailAddress());
            statement.setString(3, email.getTypeOfEmail());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private Long insertTelephone(Connection connection, long versionId, Telephone telephone) throws SQLException {
        if (telephone == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_telephones (version_id, telephone_number, type_of_telephone)
                values (?, ?, ?)
                returning id
                """)) {
            statement.setLong(1, versionId);
            statement.setString(2, telephone.getTelephoneNumber());
            statement.setString(3, telephone.getTypeOfTelephone());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private void insertDocumentation(Connection connection, long versionId, Documentation documentation) throws SQLException {
        if (documentation == null) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into dpp_documentation (
                    version_id,
                    digital_instructions_link,
                    safety_instructions_link,
                    downloadable,
                    available_for_years,
                    paper_copy_available_on_request
                ) values (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, versionId);
            statement.setString(2, documentation.getDigitalInstructionsLink());
            statement.setString(3, documentation.getSafetyInstructionsLink());
            statement.setBoolean(4, documentation.isDownloadable());
            if (documentation.getAvailableForYears() == null) {
                statement.setNull(5, Types.INTEGER);
            } else {
                statement.setInt(5, documentation.getAvailableForYears());
            }
            statement.setBoolean(6, documentation.isPaperCopyAvailableOnRequest());
            statement.executeUpdate();
        }
    }

    private PassportMetadata readPassportMetadata(Connection connection, long versionId) throws SQLException {
        UUID uniqueProductIdentifier;
        String qrCodeOrDigitalTag;
        String externalDocumentationLink;
        try (PreparedStatement statement = connection.prepareStatement("""
                select unique_product_identifier, qr_code_or_digital_tag, external_documentation_link
                from dpp_passport_metadata
                where version_id = ?
                """)) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Missing passport metadata for version " + versionId);
                }
                uniqueProductIdentifier = resultSet.getObject("unique_product_identifier", UUID.class);
                qrCodeOrDigitalTag = resultSet.getString("qr_code_or_digital_tag");
                externalDocumentationLink = resultSet.getString("external_documentation_link");
            }
        }

        List<LocalDate> updateDates = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                select update_date
                from dpp_passport_update_dates
                where version_id = ?
                order by order_index
                """)) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    updateDates.add(resultSet.getDate("update_date").toLocalDate());
                }
            }
        }

        PassportMetadata.Builder builder = new PassportMetadata.Builder()
                .uniqueProductIdentifier(uniqueProductIdentifier)
                .qrCodeOrDigitalTag(qrCodeOrDigitalTag)
                .externalDocumentationLink(externalDocumentationLink);
        updateDates.forEach(builder::addPassportUpdateDate);
        return builder.build();
    }

    private Nameplate readNameplate(Connection connection, long versionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select
                    gtin_code,
                    internal_article_number,
                    batch_number,
                    customs_tariff_number,
                    uri_of_the_product,
                    manufacturer_organization_id,
                    supplier_organization_id
                from dpp_nameplates
                where version_id = ?
                """)) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Missing nameplate for version " + versionId);
                }
                return new Nameplate.Builder()
                        .gtinCode(resultSet.getString("gtin_code"))
                        .internalArticleNumber(resultSet.getString("internal_article_number"))
                        .batchNumber(resultSet.getString("batch_number"))
                        .customsTariffNumber(resultSet.getString("customs_tariff_number"))
                        .uriOfTheProduct(resultSet.getString("uri_of_the_product"))
                        .manufacturer(readOrganization(connection, PostgresJdbcSupport.getNullableLong(resultSet, "manufacturer_organization_id")))
                        .supplier(readOrganization(connection, PostgresJdbcSupport.getNullableLong(resultSet, "supplier_organization_id")))
                        .build();
            }
        }
    }

    private Organization readOrganization(Connection connection, Long organizationId) throws SQLException {
        if (organizationId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                select
                    name,
                    gln,
                    product_description,
                    product_designation,
                    product_family,
                    product_root,
                    product_order_suffix,
                    uri,
                    role,
                    contact_id
                from dpp_organizations
                where id = ?
                """)) {
            statement.setLong(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Missing organization " + organizationId);
                }
                String role = resultSet.getString("role");
                return new Organization.Builder()
                        .name(resultSet.getString("name"))
                        .gln(resultSet.getString("gln"))
                        .productDescription(resultSet.getString("product_description"))
                        .productDesignation(resultSet.getString("product_designation"))
                        .productFamily(resultSet.getString("product_family"))
                        .productRoot(resultSet.getString("product_root"))
                        .productOrderSuffix(resultSet.getString("product_order_suffix"))
                        .uri(resultSet.getString("uri"))
                        .contact(readContact(connection, PostgresJdbcSupport.getNullableLong(resultSet, "contact_id")))
                        .role(role == null ? null : OrganizationRole.valueOf(role))
                        .build();
            }
        }
    }

    private Contact readContact(Connection connection, Long contactId) throws SQLException {
        if (contactId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                select organization_name, address_id, email_id, telephone_id
                from dpp_contacts
                where id = ?
                """)) {
            statement.setLong(1, contactId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Missing contact " + contactId);
                }
                return new Contact.Builder()
                        .organization(resultSet.getString("organization_name"))
                        .address(readAddress(connection, PostgresJdbcSupport.getNullableLong(resultSet, "address_id")))
                        .email(readEmail(connection, PostgresJdbcSupport.getNullableLong(resultSet, "email_id")))
                        .telephone(readTelephone(connection, PostgresJdbcSupport.getNullableLong(resultSet, "telephone_id")))
                        .build();
            }
        }
    }

    private Address readAddress(Connection connection, Long addressId) throws SQLException {
        if (addressId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                select country, zip_code, region, town, street
                from dpp_addresses
                where id = ?
                """)) {
            statement.setLong(1, addressId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Missing address " + addressId);
                }
                return new Address.Builder()
                        .country(resultSet.getString("country"))
                        .zipCode(resultSet.getString("zip_code"))
                        .region(resultSet.getString("region"))
                        .town(resultSet.getString("town"))
                        .street(resultSet.getString("street"))
                        .build();
            }
        }
    }

    private Email readEmail(Connection connection, Long emailId) throws SQLException {
        if (emailId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                select email_address, type_of_email
                from dpp_emails
                where id = ?
                """)) {
            statement.setLong(1, emailId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Missing email " + emailId);
                }
                return new Email.Builder()
                        .emailAddress(resultSet.getString("email_address"))
                        .typeOfEmail(resultSet.getString("type_of_email"))
                        .build();
            }
        }
    }

    private Telephone readTelephone(Connection connection, Long telephoneId) throws SQLException {
        if (telephoneId == null) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                select telephone_number, type_of_telephone
                from dpp_telephones
                where id = ?
                """)) {
            statement.setLong(1, telephoneId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Missing telephone " + telephoneId);
                }
                return new Telephone.Builder()
                        .telephoneNumber(resultSet.getString("telephone_number"))
                        .typeOfTelephone(resultSet.getString("type_of_telephone"))
                        .build();
            }
        }
    }

    private Documentation readDocumentation(Connection connection, long versionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select
                    digital_instructions_link,
                    safety_instructions_link,
                    downloadable,
                    available_for_years,
                    paper_copy_available_on_request
                from dpp_documentation
                where version_id = ?
                """)) {
            statement.setLong(1, versionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                Integer availableForYears = resultSet.getObject("available_for_years", Integer.class);
                return new Documentation.Builder()
                        .digitalInstructionsLink(resultSet.getString("digital_instructions_link"))
                        .safetyInstructionsLink(resultSet.getString("safety_instructions_link"))
                        .downloadable(resultSet.getBoolean("downloadable"))
                        .availableForYears(availableForYears)
                        .paperCopyAvailableOnRequest(resultSet.getBoolean("paper_copy_available_on_request"))
                        .build();
            }
        }
    }
}
