package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SemanticUnitTest {
    @Test
    void exposesFrozenKnownUnitsWithTheirSemanticIdentities() {
        assertEquals("mm", LengthUnit.MILLIMETER.symbol());
        assertEquals(URI.create("http://qudt.org/vocab/unit/MilliM"), LengthUnit.MILLIMETER.semanticId());
        assertEquals(UnitDimension.LENGTH, LengthUnit.MILLIMETER.dimension());
        assertEquals(URI.create("http://qudt.org/vocab/unit/IN"), LengthUnit.INCH.semanticId());
        assertEquals(UnitDimension.LENGTH, LengthUnit.INCH.dimension());
        assertEquals("in", LengthUnit.INCH.symbol());

        assertEquals("cm", LengthUnit.CENTIMETER.symbol());
        assertEquals(URI.create("http://qudt.org/vocab/unit/CentiM"), LengthUnit.CENTIMETER.semanticId());
        assertEquals(UnitDimension.LENGTH, LengthUnit.CENTIMETER.dimension());
        assertEquals("m", LengthUnit.METER.symbol());
        assertEquals(URI.create("http://qudt.org/vocab/unit/M"), LengthUnit.METER.semanticId());
        assertEquals(UnitDimension.LENGTH, LengthUnit.METER.dimension());
        assertEquals("kg", MassUnit.KILOGRAM.symbol());
        assertEquals(URI.create("http://qudt.org/vocab/unit/KiloGM"), MassUnit.KILOGRAM.semanticId());
        assertEquals(UnitDimension.MASS, MassUnit.KILOGRAM.dimension());
        assertEquals(URI.create("http://qudt.org/vocab/unit/GM"), MassUnit.GRAM.semanticId());
        assertEquals(URI.create("http://qudt.org/vocab/unit/LB_M"), MassUnit.POUND.semanticId());
        assertEquals(UnitDimension.MASS, MassUnit.GRAM.dimension());
        assertEquals(UnitDimension.MASS, MassUnit.POUND.dimension());
        assertEquals("g", MassUnit.GRAM.symbol());
        assertEquals("lb", MassUnit.POUND.symbol());
        assertEquals("m\u00B3", VolumeUnit.CUBIC_METER.symbol());
        assertEquals(URI.create("http://qudt.org/vocab/unit/M3"), VolumeUnit.CUBIC_METER.semanticId());
        assertEquals(UnitDimension.VOLUME, VolumeUnit.CUBIC_METER.dimension());
        assertEquals("L", VolumeUnit.LITER.symbol());
        assertEquals(URI.create("http://qudt.org/vocab/unit/L"), VolumeUnit.LITER.semanticId());
        assertEquals(UnitDimension.VOLUME, VolumeUnit.LITER.dimension());
        for (SemanticUnit unit : java.util.stream.Stream.of(LengthUnit.values(), MassUnit.values(), VolumeUnit.values())
                .flatMap(java.util.Arrays::stream).toList()) {
            assertEquals(java.util.Optional.of(unit), SemanticUnit.known(unit.symbol(), unit.dimension()));
        }
    }

    @Test
    void customUnitProvidesAnOpenVocabularyEscapeHatch() {
        CustomSemanticUnit unit = new CustomSemanticUnit("yd", URI.create("https://example.org/unit/yard"),
                UnitDimension.LENGTH);
        Dpp4FunQuantity<CustomSemanticUnit> quantity = new Dpp4FunQuantity<>(-1.5, unit);

        assertEquals(-1.5, quantity.value());
        assertEquals(unit, quantity.unit());
        assertThrows(IllegalArgumentException.class,
                () -> new CustomSemanticUnit(" ", URI.create("https://example.org/unit/yard"), UnitDimension.LENGTH));
        assertThrows(IllegalArgumentException.class,
                () -> new CustomSemanticUnit("yd", URI.create("yard"), UnitDimension.LENGTH));
    }

    @Test
    void typedPassportBuilderRejectsCustomUnitsThatCannotBeStoredCanonically() {
        CustomSemanticUnit unit = new CustomSemanticUnit("yd", URI.create("https://example.org/unit/yard"),
                UnitDimension.LENGTH);

        assertThrows(IllegalArgumentException.class, () -> Dpp4Fun.builder(dppsdk.support.TestDataFactory.validHeader())
                .chairCharacteristics(ChairCharacteristics.builder().length(3, unit).build()));
    }

    @Test
    void typedQuantityReportsUnitDimensionRuleId() {
        IllegalArgumentException mismatch = assertThrows(IllegalArgumentException.class,
                () -> ChairCharacteristics.builder().weight(5, LengthUnit.CENTIMETER));

        org.junit.jupiter.api.Assertions.assertTrue(
                mismatch.getMessage().startsWith("DPP4FUN_UNIT_DIMENSION_MISMATCH"));
    }
}
