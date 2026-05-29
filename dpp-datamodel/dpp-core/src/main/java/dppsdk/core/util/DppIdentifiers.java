package dppsdk.core.util;

import dppsdk.core.model.Dpp;

/**
 * Standard API-facing identifier accessors for DPP aggregates.
 */
public final class DppIdentifiers {

    private DppIdentifiers() {
    }

    public static String dppId(Dpp dpp) {
        return requireDpp(dpp).getDppId();
    }

    public static String productId(Dpp dpp) {
        return requireDpp(dpp).getProductId();
    }

    private static Dpp requireDpp(Dpp dpp) {
        if (dpp == null) {
            throw new IllegalArgumentException("dpp must not be null");
        }
        return dpp;
    }
}
