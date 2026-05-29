package dppsdk.core.support;

import dppsdk.core.model.Documentation;
import dppsdk.core.model.Dpp;
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.model.PassportMetadata;

import java.time.LocalDate;
import java.util.UUID;

public final class CoreTestDataFactory {

    private CoreTestDataFactory() {}

    public static PassportMetadata validPassportMetadata() {
        return new PassportMetadata.Builder()
                .uniqueProductIdentifier(UUID.randomUUID())
                .addPassportUpdateDate(LocalDate.now())
                .qrCodeOrDigitalTag("QR-DEMO-001")
                .externalDocumentationLink("https://example.com/doc")
                .build();
    }

    public static Organization validManufacturer() {
        return new Organization.Builder()
                .name("Demo Manufacturer GmbH")
                .gln("123456789")
                .uri("https://manufacturer.example.com")
                .role(OrganizationRole.MANUFACTURER)
                .build();
    }

    public static Organization validSupplier() {
        return new Organization.Builder()
                .name("Demo Supplier Ltd")
                .gln("987654321")
                .uri("https://supplier.example.com")
                .role(OrganizationRole.SUPPLIER)
                .build();
    }

    public static Nameplate validNameplate() {
        return new Nameplate.Builder()
                .gtinCode("GTIN-DEMO-001")
                .internalArticleNumber("ART-001")
                .batchNumber("BATCH-2026")
                .manufacturer(validManufacturer())
                .build();
    }

    public static Documentation validDocumentation() {
        return new Documentation.Builder()
                .digitalInstructionsLink("https://example.com/instructions.pdf")
                .safetyInstructionsLink("https://example.com/safety.pdf")
                .downloadable(true)
                .availableForYears(10)
                .build();
    }

    public static DppCore validDppCore() {
        return new DppCore.Builder()
                .passportMetadata(validPassportMetadata())
                .nameplate(validNameplate())
                .documentation(validDocumentation())
                .build();
    }

    public static Dpp validDpp() {
        DppCore core = validDppCore();
        return new Dpp() {
            @Override
            public DppCore getCoreDpp() {
                return core;
            }

            @Override
            public String getPassportType() {
                return "Test";
            }
        };
    }
}
