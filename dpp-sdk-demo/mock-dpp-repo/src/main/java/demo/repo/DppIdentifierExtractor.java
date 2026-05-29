package demo.repo;

import dpp.repo.payloads.DppStatusCode;
import dppsdk.dpp4fun.model.Dpp4Fun;
import org.springframework.stereotype.Component;

/**
 * Centralizes how the mock repository extracts identifiers from SDK DPP objects.
 *
 * <p>Keeping identifier access here avoids duplicating field assumptions across create/update paths and
 * makes missing-ID failures consistent.</p>
 */
@Component
class DppIdentifierExtractor {

    String extractDppId(Dpp4Fun dpp) {
        String dppId = dpp.getDppId();
        if (dppId == null || dppId.isBlank()) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "MISSING_DPP_ID",
                    "The DPP does not expose a usable dpp identifier");
        }
        return dppId;
    }

    String extractProductId(Dpp4Fun dpp) {
        String productId = dpp.getProductId();
        if (productId == null || productId.isBlank()) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "MISSING_PRODUCT_ID",
                    "The DPP does not expose a usable product identifier");
        }
        return productId;
    }
}
