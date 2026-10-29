package demo.repo;

import dpp.repo.payloads.DppStatusCode;
import dppsdk.core.model.DigitalProductPassport;
import org.springframework.stereotype.Component;

/**
 * Centralizes how the demo repository extracts identifiers from SDK DPP objects.
 *
 * <p>Keeping identifier access here avoids duplicating field assumptions across create/update paths and
 * makes missing-ID failures consistent.</p>
 */
@Component
class DppIdentifierExtractor {

    String extractDppId(DigitalProductPassport dpp) {
        String dppId = dpp.getDppHeader().getDigitalProductPassportId();
        if (dppId == null || dppId.isBlank()) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "MISSING_DPP_ID",
                    "The DPP does not expose a usable dpp identifier");
        }
        return dppId;
    }

    String extractProductId(DigitalProductPassport dpp) {
        String productId = dpp.getDppHeader().getUniqueProductIdentifier();
        if (productId == null || productId.isBlank()) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "MISSING_PRODUCT_ID",
                    "The DPP does not expose a usable product identifier");
        }
        return productId;
    }
}
