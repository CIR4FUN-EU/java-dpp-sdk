package dppsdk.dpp4fun.model.unit;
import dppsdk.dpp4fun.model.*;

import java.net.URI;

/** Common mass unit shown in the frozen D4.1 examples and Dpp4Fun samples. */
public enum MassUnit implements SemanticUnit {
    KILOGRAM("kg", "http://qudt.org/vocab/unit/KiloGM"),
    GRAM("g", "http://qudt.org/vocab/unit/GM"),
    POUND("lb", "http://qudt.org/vocab/unit/LB_M");

    private final String symbol;
    private final URI semanticId;

    MassUnit(String symbol, String semanticId) {
        this.symbol = symbol;
        this.semanticId = URI.create(semanticId);
    }

    @Override public String symbol() { return symbol; }
    @Override public URI semanticId() { return semanticId; }
    @Override public UnitDimension dimension() { return UnitDimension.MASS; }
}
