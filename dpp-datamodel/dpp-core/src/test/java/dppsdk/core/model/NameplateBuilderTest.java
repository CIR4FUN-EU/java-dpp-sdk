package dppsdk.core.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Nameplate.Builder — local integrity only.
 */
class NameplateBuilderTest {

    @Test
    void build_withGtinCode_succeeds() {
        Nameplate n = new Nameplate.Builder().gtinCode("GTIN-001").build();
        assertEquals("GTIN-001", n.getGtinCode());
    }

    @Test
    void build_missingGtinCode_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new Nameplate.Builder().internalArticleNumber("ART-001").build()
        );
    }

    @Test
    void build_blankGtinCode_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new Nameplate.Builder().gtinCode("   ").build()
        );
    }

    @Test
    void build_withManufacturer_succeeds() {
        Organization manufacturer = new Organization.Builder()
                .name("Manufacturer Co")
                .role(OrganizationRole.MANUFACTURER)
                .build();

        Nameplate n = new Nameplate.Builder()
                .gtinCode("GTIN-001")
                .manufacturer(manufacturer)
                .build();

        assertNotNull(n.getManufacturer());
        assertEquals("Manufacturer Co", n.getManufacturer().getName());
    }
}
