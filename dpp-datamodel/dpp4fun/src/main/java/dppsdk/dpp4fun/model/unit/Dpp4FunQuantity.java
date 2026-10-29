package dppsdk.dpp4fun.model.unit;
import dppsdk.dpp4fun.model.*;

import java.util.Objects;

/** A finite measurement and its semantically typed unit. */
public record Dpp4FunQuantity<U extends SemanticUnit>(double value, U unit) {
    public Dpp4FunQuantity {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("quantity value must be finite");
        Objects.requireNonNull(unit, "unit");
    }
}
