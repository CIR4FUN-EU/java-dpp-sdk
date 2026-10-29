package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import dppsdk.core.model.DataElementCollection;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

class BillOfMaterialsTypedApiTest {
    @Test
    void buildsCatalogBackedSetsAndRootCopyEdit() {
        BillOfMaterials initial = BillOfMaterials.builder()
                .addMaterial(new Material.Builder().name("Steel").build())
                .addMaterial(new Material.Builder().name("Foam").build())
                .build();
        Dpp4Fun original = Dpp4Fun.builder(TestDataFactory.validHeader()).billOfMaterials(initial).build();
        Dpp4Fun changed = original.toBuilder().billOfMaterials(original.getBillOfMaterials().toBuilder()
                .removeMaterial(original.getBillOfMaterials().getMaterials().get(0))
                .addPart(new Part.Builder().name("Fastener").build()).build()).build();

        assertEquals(2, original.getBillOfMaterials().getMaterials().size());
        assertEquals(0, original.getBillOfMaterials().getParts().size());
        assertEquals(1, changed.getBillOfMaterials().getMaterials().size());
        assertEquals("Foam", changed.getBillOfMaterials().getMaterials().get(0).getName());
        assertEquals(1, changed.getBillOfMaterials().getParts().size());
        DataElementCollection root = (DataElementCollection) changed.asDigitalProductPassport().getDataElements().stream()
                .filter(element -> element.getElementId().equals("billOfMaterials")).findFirst().orElseThrow();
        assertNotEquals("legacyBillOfMaterials", root.getElementId());
    }
}
