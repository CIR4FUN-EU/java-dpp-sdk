package dppsdk.dpp4fun.model.unit;
import dppsdk.dpp4fun.model.*;

import java.net.URI;

/** Common length units shown in the frozen D4.1 examples and Dpp4Fun samples. */
public enum LengthUnit implements SemanticUnit {
    MILLIMETER("mm", "http://qudt.org/vocab/unit/MilliM"),
    CENTIMETER("cm", "http://qudt.org/vocab/unit/CentiM"),
    METER("m", "http://qudt.org/vocab/unit/M"),
    INCH("in", "http://qudt.org/vocab/unit/IN");

    private final String symbol;
    private final URI semanticId;

    LengthUnit(String symbol, String semanticId) {
        this.symbol = symbol;
        this.semanticId = URI.create(semanticId);
    }

    @Override public String symbol() { return symbol; }
    @Override public URI semanticId() { return semanticId; }
    @Override public UnitDimension dimension() { return UnitDimension.LENGTH; }
}
