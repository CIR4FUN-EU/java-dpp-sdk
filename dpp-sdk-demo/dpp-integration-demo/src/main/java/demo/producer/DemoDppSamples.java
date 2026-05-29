package demo.producer;

import dppsdk.dpp4fun.model.Dpp4Fun;

record DemoDppSamples(
        Dpp4Fun validBedDpp,
        Dpp4Fun validChairDpp,
        Dpp4Fun updatedBedDpp,
        Dpp4Fun invalidSupplierRoleDpp,
        Dpp4Fun invalidDocumentationDpp,
        String malformedJson
) {
}
