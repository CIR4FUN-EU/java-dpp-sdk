package dppsdk.core.validation.identifier;

import java.util.Objects;

/** The caller-selected identifier methods; methods are never inferred from identifier text. */
public record IdentifierValidationContext(
        ProductIdentifierMethod productIdentifierMethod,
        EconomicOperatorIdentifierMethod economicOperatorIdentifierMethod,
        FacilityIdentifierMethod facilityIdentifierMethod) {

    public IdentifierValidationContext {
        Objects.requireNonNull(productIdentifierMethod, "productIdentifierMethod is required");
        Objects.requireNonNull(economicOperatorIdentifierMethod,
                "economicOperatorIdentifierMethod is required");
        // A null facility method is meaningful only when the optional facility ID is absent.
    }
}
