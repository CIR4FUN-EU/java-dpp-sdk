package dppsdk.dpp4fun.model.unit;
import dppsdk.dpp4fun.model.*;

import java.net.URI;
import java.util.Objects;

/**
 * Advanced open-vocabulary unit input. D4.1 string slots preserve its symbol, but not this semantic URI;
 * canonical Dpp4Fun round-trip identity is guaranteed only for built-in units recognized by their symbol.
 */
public record CustomSemanticUnit(String symbol, URI semanticId, UnitDimension dimension) implements SemanticUnit {
    public CustomSemanticUnit {
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol is required");
        Objects.requireNonNull(semanticId, "semanticId");
        if (!semanticId.isAbsolute()) throw new IllegalArgumentException("semanticId must be an absolute URI");
        Objects.requireNonNull(dimension, "dimension");
    }
}
