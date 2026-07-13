package demo.repo;

enum DppRepresentation {
    FULL,
    COMPRESSED;

    static DppRepresentation parse(String value) {
        if (value == null || value.isBlank() || "compressed".equalsIgnoreCase(value)) {
            return COMPRESSED;
        }
        if ("full".equalsIgnoreCase(value)) {
            return FULL;
        }
        throw new RepoApiException(dpp.repo.payloads.DppStatusCode.ClientErrorBadRequest,
                "INVALID_REPRESENTATION", "representation must be full or compressed");
    }
}
