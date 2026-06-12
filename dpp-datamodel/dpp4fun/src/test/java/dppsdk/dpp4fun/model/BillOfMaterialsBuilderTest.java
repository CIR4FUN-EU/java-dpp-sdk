package dppsdk.dpp4fun.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BillOfMaterials.Builder â€” defensive copying and toBuilder independence.
 */
class BillOfMaterialsBuilderTest {

    @Test
    void build_emptyBom_succeeds() {
        BillOfMaterials bom = new BillOfMaterials.Builder().build();
        assertTrue(bom.getMaterials().isEmpty());
        assertTrue(bom.getComponents().isEmpty());
        assertTrue(bom.getParts().isEmpty());
    }

    @Test
    void getMaterials_returnsDefensiveCopy() {
        Material steel = new Material.Builder().name("Steel").portion(2.0).build();
        BillOfMaterials bom = new BillOfMaterials.Builder().addMaterial(steel).build();

        bom.getMaterials().clear(); // mutate returned list
        assertEquals(1, bom.getMaterials().size()); // original still intact
    }

    @Test
    void toBuilder_addMaterial_doesNotAffectOriginal() {
        Material steel = new Material.Builder().name("Steel").portion(2.0).build();
        BillOfMaterials original = new BillOfMaterials.Builder().addMaterial(steel).build();

        BillOfMaterials updated = original.toBuilder()
                .addMaterial(new Material.Builder().name("Foam").portion(1.0).build())
                .build();

        assertEquals(1, original.getMaterials().size());
        assertEquals(2, updated.getMaterials().size());
    }

    @Test
    void toBuilder_removeMaterial_doesNotAffectOriginal() {
        Material steel = new Material.Builder().name("Steel").portion(2.0).build();
        Material foam  = new Material.Builder().name("Foam").portion(1.0).build();
        BillOfMaterials original = new BillOfMaterials.Builder()
                .addMaterial(steel).addMaterial(foam).build();

        BillOfMaterials updated = original.toBuilder().removeMaterial(foam).build();

        assertEquals(2, original.getMaterials().size());
        assertEquals(1, updated.getMaterials().size());
    }
}

