package dppsdk.dpp4fun.mapper;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.*;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Typed model contract tests for canonical core-backed construction and read-back. */
class MapperRoundTripTest {
    @Test void typedBuildersCreateCanonicalCoreSubtreesInProfileOrder() {
        Dpp4Fun value = TestDataFactory.validDpp();
        List<DataElement> root = value.asDigitalProductPassport().getDataElements();
        assertTrue(root.stream().anyMatch(element -> "nameplate".equals(element.getElementId())));
        assertFalse(root.stream().anyMatch(element -> "legacyNameplate".equals(element.getElementId())));
        DataElementCollection nameplate = (DataElementCollection) root.stream()
                .filter(element -> "nameplate".equals(element.getElementId())).findFirst().orElseThrow();
        assertInstanceOf(SingleValuedDataElement.class,
                nameplate.getDataElements().stream().filter(e -> e.getElementId().equals("gtinCode")).findFirst().orElseThrow());
        DataElementCollection characteristics = (DataElementCollection) root.stream()
                .filter(element -> "characteristics".equals(element.getElementId())).findFirst().orElseThrow();
        assertInstanceOf(SingleValuedDataElement.class,
                characteristics.getDataElements().stream().filter(e -> e.getElementId().equals("color")).findFirst().orElseThrow());
    }

    @Test void canonicalCoreTreesReadBackThroughEveryPublicTypedView() {
        Dpp4Fun original = TestDataFactory.validDpp();
        Dpp4Fun restored = Dpp4Fun.from(original.asDigitalProductPassport());
        assertEquals("123456789", restored.getNameplate().getGlnOfManufacturer());
        assertEquals("navy", restored.getChairCharacteristics().getColor());
        assertEquals(200.0, restored.getChairCharacteristics().getLengthValue());
        assertEquals(2, restored.getBillOfMaterials().getMaterials().size());
        assertEquals("cm", restored.getChairCharacteristics().getLengthUnitSymbol());
    }
}
