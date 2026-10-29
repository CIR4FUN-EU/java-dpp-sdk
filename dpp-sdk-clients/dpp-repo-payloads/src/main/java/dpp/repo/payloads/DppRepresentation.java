package dpp.repo.payloads;

/** EN 18222 DPP payload representation, independent of HTTP content type. */
public enum DppRepresentation {
    COMPRESSED,
    FULL;

    public static DppRepresentation fromQuery(String value) {
        if (value == null || "compressed".equalsIgnoreCase(value)) return COMPRESSED;
        if ("full".equalsIgnoreCase(value)) return FULL;
        throw new IllegalArgumentException("representation must be full or compressed");
    }

    public String querySuffix() {
        return this == FULL ? "?representation=full" : "";
    }
}
