package dppsdk.dpp4fun.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Characteristics.Builder â€” local integrity only.
 * Semantic validation is tested in CharacteristicsValidatorTest.
 */
class CharacteristicsBuilderTest {

    @Test
    void build_validMinimal_succeeds() {
        Characteristics c = new Characteristics.Builder()
                .productName("Test Bed")
                .build();
        assertEquals("Test Bed", c.getProductName());
    }

    @Test
    void build_nullProductName_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new Characteristics.Builder().productName(null).build()
        );
    }

    @Test
    void build_blankProductName_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new Characteristics.Builder().productName("   ").build()
        );
    }

    @Test
    void build_negativeWeight_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new Characteristics.Builder().productName("Test").weight(-1.0).build()
        );
    }

    @Test
    void build_zeroWeight_succeeds() {
        Characteristics c = new Characteristics.Builder().productName("Test").weight(0.0).build();
        assertEquals(0.0, c.getWeight());
    }

    @Test
    void getFeatures_returnsDefensiveCopy() {
        Characteristics c = new Characteristics.Builder()
                .productName("Test")
                .addFeature("Feature A")
                .build();

        // Mutating the returned list must not affect the object
        c.getFeatures().add("Injected");
        assertEquals(1, c.getFeatures().size());
    }

    @Test
    void toBuilder_createsIndependentCopy() {
        Characteristics original = new Characteristics.Builder()
                .productName("Original")
                .weight(50.0)
                .build();

        Characteristics updated = original.toBuilder().productName("Updated").build();

        assertEquals("Original", original.getProductName());
        assertEquals("Updated", updated.getProductName());
    }
}

