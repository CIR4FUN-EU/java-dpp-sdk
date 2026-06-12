package dppsdk.core.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PassportMetadata.Builder — local integrity only.
 */
class PassportMetadataBuilderTest {

    @Test
    void build_validMinimal_succeeds() {
        PassportMetadata m = new PassportMetadata.Builder()
                .uniqueProductIdentifier(UUID.randomUUID())
                .addPassportUpdateDate(LocalDate.now())
                .build();
        assertNotNull(m.getUniqueProductIdentifier());
        assertEquals(1, m.getPassportUpdateDates().size());
    }

    @Test
    void getPassportUpdateDates_returnsDefensiveCopy() {
        PassportMetadata m = new PassportMetadata.Builder()
                .uniqueProductIdentifier(UUID.randomUUID())
                .addPassportUpdateDate(LocalDate.now())
                .build();

        // Mutating the returned list must not affect the object
        m.getPassportUpdateDates().add(LocalDate.now().minusDays(1));
        assertEquals(1, m.getPassportUpdateDates().size());
    }

    @Test
    void toBuilder_preservesUUID() {
        UUID id = UUID.randomUUID();
        PassportMetadata original = new PassportMetadata.Builder()
                .uniqueProductIdentifier(id)
                .addPassportUpdateDate(LocalDate.now())
                .externalDocumentationLink("https://v1.example.com")
                .build();

        PassportMetadata updated = original.toBuilder()
                .externalDocumentationLink("https://v2.example.com")
                .build();

        assertEquals(id, updated.getUniqueProductIdentifier());
        assertEquals("https://v1.example.com", original.getExternalDocumentationLink());
        assertEquals("https://v2.example.com", updated.getExternalDocumentationLink());
    }
}
