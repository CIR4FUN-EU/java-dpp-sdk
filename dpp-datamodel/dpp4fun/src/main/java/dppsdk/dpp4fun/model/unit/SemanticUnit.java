package dppsdk.dpp4fun.model.unit;
import dppsdk.dpp4fun.model.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

/** Semantic identity for a unit accepted by the typed Dpp4Fun API. */
public interface SemanticUnit {
    String symbol();

    URI semanticId();

    UnitDimension dimension();

    /** Resolves a canonical D4.1 unit symbol only when its built-in semantic identity is known. */
    static Optional<SemanticUnit> known(String symbol, UnitDimension dimension) {
        if (symbol == null || dimension == null) return Optional.empty();
        return switch (dimension) {
            case LENGTH -> Arrays.stream(LengthUnit.values()).filter(unit -> unit.symbol().equals(symbol)).map(unit -> (SemanticUnit) unit).findFirst();
            case MASS -> Arrays.stream(MassUnit.values()).filter(unit -> unit.symbol().equals(symbol)).map(unit -> (SemanticUnit) unit).findFirst();
            case VOLUME -> Arrays.stream(VolumeUnit.values()).filter(unit -> unit.symbol().equals(symbol)).map(unit -> (SemanticUnit) unit).findFirst();
        };
    }
}
