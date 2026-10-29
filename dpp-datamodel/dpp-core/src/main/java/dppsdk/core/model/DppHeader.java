package dppsdk.core.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** SDK grouping for EN root attributes; JSON transport writes these onto the passport root. */
public final class DppHeader {
    private final String digitalProductPassportId;
    private final String uniqueProductIdentifier;
    private final Granularity granularity;
    private final String dppSchemaVersion;
    private final String dppStatus;
    private final Instant lastUpdated;
    private final String economicOperatorId;
    private final String facilityId;
    private final List<String> contentSpecificationIds;

    private DppHeader(Builder builder) {
        this.digitalProductPassportId = builder.digitalProductPassportId;
        this.uniqueProductIdentifier = builder.uniqueProductIdentifier;
        this.granularity = builder.granularity;
        this.dppSchemaVersion = builder.dppSchemaVersion;
        this.dppStatus = builder.dppStatus;
        this.lastUpdated = builder.lastUpdated;
        this.economicOperatorId = builder.economicOperatorId;
        this.facilityId = builder.facilityId;
        this.contentSpecificationIds = List.copyOf(builder.contentSpecificationIds);
    }

    public String getDigitalProductPassportId() { return digitalProductPassportId; }
    public String getUniqueProductIdentifier() { return uniqueProductIdentifier; }
    public Granularity getGranularity() { return granularity; }
    public String getDppSchemaVersion() { return dppSchemaVersion; }
    public String getDppStatus() { return dppStatus; }
    public Instant getLastUpdated() { return lastUpdated; }
    public String getEconomicOperatorId() { return economicOperatorId; }
    public String getFacilityId() { return facilityId; }
    public List<String> getContentSpecificationIds() { return contentSpecificationIds; }

    public Builder toBuilder() {
        return new Builder()
                .digitalProductPassportId(digitalProductPassportId)
                .uniqueProductIdentifier(uniqueProductIdentifier)
                .granularity(granularity)
                .dppSchemaVersion(dppSchemaVersion)
                .dppStatus(dppStatus)
                .lastUpdated(lastUpdated)
                .economicOperatorId(economicOperatorId)
                .facilityId(facilityId)
                .contentSpecificationIds(contentSpecificationIds);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        DppHeader that = (DppHeader) other;
        return Objects.equals(digitalProductPassportId, that.digitalProductPassportId)
                && Objects.equals(uniqueProductIdentifier, that.uniqueProductIdentifier)
                && granularity == that.granularity
                && Objects.equals(dppSchemaVersion, that.dppSchemaVersion)
                && Objects.equals(dppStatus, that.dppStatus)
                && Objects.equals(lastUpdated, that.lastUpdated)
                && Objects.equals(economicOperatorId, that.economicOperatorId)
                && Objects.equals(facilityId, that.facilityId)
                && Objects.equals(contentSpecificationIds, that.contentSpecificationIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(digitalProductPassportId, uniqueProductIdentifier, granularity,
                dppSchemaVersion, dppStatus, lastUpdated, economicOperatorId, facilityId,
                contentSpecificationIds);
    }

    @Override
    public String toString() {
        return "DppHeader{digitalProductPassportId='" + digitalProductPassportId
                + "', uniqueProductIdentifier='" + uniqueProductIdentifier + "'}";
    }

    public static final class Builder {
        private String digitalProductPassportId;
        private String uniqueProductIdentifier;
        private Granularity granularity;
        private String dppSchemaVersion;
        private String dppStatus;
        private Instant lastUpdated;
        private String economicOperatorId;
        private String facilityId;
        private List<String> contentSpecificationIds = List.of();

        public Builder digitalProductPassportId(String value) { this.digitalProductPassportId = value; return this; }
        public Builder uniqueProductIdentifier(String value) { this.uniqueProductIdentifier = value; return this; }
        public Builder granularity(Granularity value) { this.granularity = value; return this; }
        public Builder dppSchemaVersion(String value) { this.dppSchemaVersion = value; return this; }
        public Builder dppStatus(String value) { this.dppStatus = value; return this; }
        public Builder lastUpdated(Instant value) { this.lastUpdated = value; return this; }
        public Builder economicOperatorId(String value) { this.economicOperatorId = value; return this; }
        public Builder facilityId(String value) { this.facilityId = value; return this; }
        public Builder contentSpecificationIds(List<String> value) {
            this.contentSpecificationIds = List.copyOf(value);
            return this;
        }
        public DppHeader build() { return new DppHeader(this); }
    }
}
