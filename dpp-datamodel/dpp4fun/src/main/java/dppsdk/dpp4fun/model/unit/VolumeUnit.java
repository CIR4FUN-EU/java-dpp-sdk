package dppsdk.dpp4fun.model.unit;
import dppsdk.dpp4fun.model.*;

import java.net.URI;

/** Volume units named in D4.1 examples and represented in QUDT. */
public enum VolumeUnit implements SemanticUnit {
    CUBIC_METER("m\u00B3", "http://qudt.org/vocab/unit/M3"),
    LITER("L", "http://qudt.org/vocab/unit/L");

    private final String symbol;
    private final URI semanticId;

    VolumeUnit(String symbol, String semanticId) {
        this.symbol = symbol;
        this.semanticId = URI.create(semanticId);
    }

    @Override public String symbol() { return symbol; }
    @Override public URI semanticId() { return semanticId; }
    @Override public UnitDimension dimension() { return UnitDimension.VOLUME; }
}
