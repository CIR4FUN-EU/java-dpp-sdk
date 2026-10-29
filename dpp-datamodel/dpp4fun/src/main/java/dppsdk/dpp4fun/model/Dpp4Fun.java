package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.Instant;

/** Typed Dpp4Fun profile facade whose sole state is one canonical core passport. */
public final class Dpp4Fun {
    /** Schema version emitted by this typed profile API. */
    public static final String SCHEMA_VERSION = "1.0";
    static final List<String> ORDER = Dpp4FunSemanticProfile.loadDefault().rootElementIds();
    private final DigitalProductPassport passport;

    private Dpp4Fun(DigitalProductPassport passport) { this.passport = Objects.requireNonNull(passport, "passport"); }

    /** Starts a typed passport with SDK-managed schema version and UTC last-updated time. */
    public static Builder builder() { return new Builder(); }
    /** Starts a typed edit using an existing immutable header. */
    public static Builder builder(DppHeader header) { return new Builder(header); }

    /**
     * Wraps an immutable core passport after checking reserved profile element shapes.
     * Missing profile-required fields remain representable and are reported by
     * {@code Dpp4FunValidator}; duplicate or wrongly typed reserved profile elements are rejected.
     *
     * @throws NullPointerException if {@code passport} is null
     * @throws IllegalArgumentException if a reserved Dpp4Fun element is ambiguous or has the wrong shape
     */
    public static Dpp4Fun from(DigitalProductPassport passport) {
        Objects.requireNonNull(passport, "passport");
        DataElementCollection root = new DataElementCollection.Builder().elementId("$")
                .dataElements(passport.getDataElements()).build();
        ProfileElementSupport.requireUniqueKnownIds(root, ORDER.toArray(String[]::new));
        DataElement child;
        for (KnownProfileAspect type : KnownProfileAspect.values()) {
            child = ProfileElementSupport.find(root, type.elementId());
            if (child != null) new ProfiledAspectState(type.elementId(), child);
        }
        child = ProfileElementSupport.find(root, "characteristics");
        if (child != null && !(child instanceof DataElementCollection)) {
            throw new IllegalArgumentException("Reserved Dpp4Fun Characteristics must be a DataElementCollection");
        }
        List<String> contentSpecificationIds = passport.getDppHeader().getContentSpecificationIds();
        List<Dpp4FunProductVariant> selectedVariants = java.util.Arrays.stream(Dpp4FunProductVariant.values())
                .filter(variant -> contentSpecificationIds
                        .contains(Dpp4FunSemanticProfile.loadDefault().variantContentSpecificationIds().get(variant.id())))
                .toList();
        if (selectedVariants.size() > 1) throw new IllegalArgumentException("A Dpp4Fun passport can select only one Characteristics variant");
        return new Dpp4Fun(passport);
    }

    public DigitalProductPassport asDigitalProductPassport() { return passport; }
    public DppHeader getHeader() { return passport.getDppHeader(); }
    public String getDigitalProductPassportId() { return getHeader().getDigitalProductPassportId(); }
    public String getUniqueProductIdentifier() { return getHeader().getUniqueProductIdentifier(); }
    public Granularity getGranularity() { return getHeader().getGranularity(); }
    public String getEconomicOperatorId() { return getHeader().getEconomicOperatorId(); }
    public String getDppStatus() { return getHeader().getDppStatus(); }
    public String getFacilityId() { return getHeader().getFacilityId(); }
    public Instant getLastUpdated() { return getHeader().getLastUpdated(); }
    /** Returns the D4.1 Nameplate aspect, or {@code null} when it is absent. */
    public Nameplate getNameplate() {
        DataElement value = ProfileElementSupport.find(root(), KnownProfileAspect.NAMEPLATE.elementId());
        return value == null ? null : new Nameplate(value);
    }
    /** Returns the selected source-specific Characteristics variant, if present. */
    public FurnitureCharacteristics getCharacteristics() {
        Dpp4FunProductVariant selected = selectedCharacteristicsVariant();
        DataElement value = ProfileElementSupport.find(root(), "characteristics");
        if (selected == null || value == null) return null;
        return switch (selected) {
            case CHAIR -> new ChairCharacteristics(value);
            case CRIB -> new CribCharacteristics(value);
            case FABRIC -> new FabricCharacteristics(value);
            case LOCKER -> new LockerCharacteristics(value);
            case PARTICLE_BOARD -> new ParticleBoardCharacteristics(value);
            case SOFA -> new SofaCharacteristics(value);
        };
    }
    /** Alias that states explicitly that the returned source-specific variant may vary. */
    public FurnitureCharacteristics getSelectedCharacteristics() { return getCharacteristics(); }
    public ChairCharacteristics getChairCharacteristics() { return (ChairCharacteristics) getSelectedCharacteristics(Dpp4FunProductVariant.CHAIR); }
    public CribCharacteristics getCribCharacteristics() { return (CribCharacteristics) getSelectedCharacteristics(Dpp4FunProductVariant.CRIB); }
    public FabricCharacteristics getFabricCharacteristics() { return (FabricCharacteristics) getSelectedCharacteristics(Dpp4FunProductVariant.FABRIC); }
    public LockerCharacteristics getLockerCharacteristics() { return (LockerCharacteristics) getSelectedCharacteristics(Dpp4FunProductVariant.LOCKER); }
    public ParticleBoardCharacteristics getParticleBoardCharacteristics() { return (ParticleBoardCharacteristics) getSelectedCharacteristics(Dpp4FunProductVariant.PARTICLE_BOARD); }
    public SofaCharacteristics getSofaCharacteristics() { return (SofaCharacteristics) getSelectedCharacteristics(Dpp4FunProductVariant.SOFA); }
    /** Returns the D4.1 BillOfMaterials aspect, or {@code null} when it is absent. */
    public BillOfMaterials getBillOfMaterials() {
        DataElement value = ProfileElementSupport.find(root(), KnownProfileAspect.BILL_OF_MATERIALS.elementId());
        return value == null ? null : new BillOfMaterials(value);
    }
    public CarbonFootprint getCarbonFootprint() { return typed(KnownProfileAspect.CARBON_FOOTPRINT, CarbonFootprint::new); }
    public Durability getDurability() { return typed(KnownProfileAspect.DURABILITY, Durability::new); }
    public EnvironmentalImpact getEnvironmentalImpact() { return typed(KnownProfileAspect.ENVIRONMENTAL_IMPACT, EnvironmentalImpact::new); }
    public HealthImpact getHealthImpact() { return typed(KnownProfileAspect.HEALTH_IMPACT, HealthImpact::new); }
    public ProductRelationship getProductRelationship() { return typed(KnownProfileAspect.PRODUCT_RELATIONSHIP, ProductRelationship::new); }
    public Recyclability getRecyclability() { return typed(KnownProfileAspect.RECYCLABILITY, Recyclability::new); }
    public Refurbishment getRefurbishment() { return typed(KnownProfileAspect.REFURBISHMENT, Refurbishment::new); }
    public Reliability getReliability() { return typed(KnownProfileAspect.RELIABILITY, Reliability::new); }
    public Remanufacturing getRemanufacturing() { return typed(KnownProfileAspect.REMANUFACTURING, Remanufacturing::new); }
    public Repairability getRepairability() { return typed(KnownProfileAspect.REPAIRABILITY, Repairability::new); }
    public Reusability getReusability() { return typed(KnownProfileAspect.REUSABILITY, Reusability::new); }
    public RightOfEndUsers getRightOfEndUsers() { return typed(KnownProfileAspect.RIGHT_OF_END_USERS, RightOfEndUsers::new); }
    public Safety getSafety() { return typed(KnownProfileAspect.SAFETY, Safety::new); }
    public Traceability getTraceability() { return typed(KnownProfileAspect.TRACEABILITY, Traceability::new); }
    public Upgradability getUpgradability() { return typed(KnownProfileAspect.UPGRADABILITY, Upgradability::new); }
    public Usage getUsage() { return typed(KnownProfileAspect.USAGE, Usage::new); }
    public WasteManagement getWasteManagement() { return typed(KnownProfileAspect.WASTE_MANAGEMENT, WasteManagement::new); }

    private Dpp4FunProductVariant selectedCharacteristicsVariant() {
        Dpp4FunSemanticProfile catalog = Dpp4FunSemanticProfile.loadDefault();
        return java.util.Arrays.stream(Dpp4FunProductVariant.values())
                .filter(candidate -> getHeader().getContentSpecificationIds()
                        .contains(catalog.variantContentSpecificationIds().get(candidate.id())))
                .findFirst().orElse(null);
    }

    private FurnitureCharacteristics getSelectedCharacteristics(Dpp4FunProductVariant variant) {
        if (selectedCharacteristicsVariant() != variant) return null;
        DataElement value = ProfileElementSupport.find(root(), "characteristics");
        if (value == null) return null;
        return switch (variant) {
            case CHAIR -> new ChairCharacteristics(value);
            case CRIB -> new CribCharacteristics(value);
            case FABRIC -> new FabricCharacteristics(value);
            case LOCKER -> new LockerCharacteristics(value);
            case PARTICLE_BOARD -> new ParticleBoardCharacteristics(value);
            case SOFA -> new SofaCharacteristics(value);
        };
    }

    public Builder toBuilder() { return new Builder(passport); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Dpp4Fun that && passport.equals(that.passport); }
    @Override public int hashCode() { return passport.hashCode(); }
    @Override public String toString() { return "Dpp4Fun{" + passport + '}'; }

    private <T> T typed(KnownProfileAspect type, java.util.function.Function<DataElement, T> factory) {
        DataElement child = ProfileElementSupport.find(root(), type.elementId());
        return child == null ? null : factory.apply(child);
    }
    private DataElementCollection root() {
        return new DataElementCollection.Builder().elementId("$").dataElements(passport.getDataElements()).build();
    }

    public static final class Builder {
        private DppHeader header;
        private List<DataElement> elements = List.of();
        private Dpp4FunProductVariant variant;

        private String digitalProductPassportId;
        private String uniqueProductIdentifier;
        private Granularity granularity;
        private String economicOperatorId;
        private String dppStatus;
        private String facilityId;
        private Instant lastUpdated;
        private List<String> existingContentSpecificationIds = List.of();

        private Builder() { }
        private Builder(DppHeader value) {
            header(value);
        }
        private Builder(DigitalProductPassport source) {
            header(source.getDppHeader()); elements = source.getDataElements();
            Dpp4FunSemanticProfile catalog = Dpp4FunSemanticProfile.loadDefault();
            for (Dpp4FunProductVariant candidate : Dpp4FunProductVariant.values()) {
                if (header.getContentSpecificationIds().contains(catalog.variantContentSpecificationIds().get(candidate.id()))) {
                    if (variant != null) throw new IllegalArgumentException("Multiple Dpp4Fun Characteristics variants are declared");
                    variant = candidate;
                }
            }
        }
        public Builder digitalProductPassportId(String value) { digitalProductPassportId = value; return this; }
        public Builder uniqueProductIdentifier(String value) { uniqueProductIdentifier = value; return this; }
        public Builder granularity(Granularity value) { granularity = value; return this; }
        public Builder economicOperatorId(String value) { economicOperatorId = value; return this; }
        public Builder dppStatus(String value) { dppStatus = value; return this; }
        public Builder facilityId(String value) { facilityId = value; return this; }
        /** Sets an explicit UTC timestamp; when omitted, the SDK captures the build time in UTC. */
        public Builder lastUpdated(Instant value) { lastUpdated = value; return this; }
        public Builder header(DppHeader value) {
            header = Objects.requireNonNull(value, "dppHeader is required");
            digitalProductPassportId = value.getDigitalProductPassportId();
            uniqueProductIdentifier = value.getUniqueProductIdentifier();
            granularity = value.getGranularity();
            economicOperatorId = value.getEconomicOperatorId();
            dppStatus = value.getDppStatus();
            facilityId = value.getFacilityId();
            lastUpdated = value.getLastUpdated();
            existingContentSpecificationIds = value.getContentSpecificationIds();
            return this;
        }
        public Builder characteristics(FurnitureCharacteristics value) {
            if (value == null) { variant = null; return set("characteristics", null); }
            if (!(value instanceof CatalogAspectModel model))
                throw new IllegalArgumentException("Unsupported Characteristics variant");
            if (variant != null && variant != value.variant())
                throw new IllegalArgumentException("A passport may select only one Characteristics variant");
            variant = value.variant();
            return setTyped("characteristics", model.toDataElement());
        }
        public Builder chairCharacteristics(ChairCharacteristics value) { return characteristics(value); }
        public Builder cribCharacteristics(CribCharacteristics value) { return characteristics(value); }
        public Builder fabricCharacteristics(FabricCharacteristics value) { return characteristics(value); }
        public Builder lockerCharacteristics(LockerCharacteristics value) { return characteristics(value); }
        public Builder particleBoardCharacteristics(ParticleBoardCharacteristics value) { return characteristics(value); }
        public Builder sofaCharacteristics(SofaCharacteristics value) { return characteristics(value); }
        public Builder nameplate(Nameplate value) {
            return setTyped(KnownProfileAspect.NAMEPLATE.elementId(), value == null ? null : value.toDataElement());
        }
        public Builder billOfMaterials(BillOfMaterials value) {
            return setTyped(KnownProfileAspect.BILL_OF_MATERIALS.elementId(), value == null ? null : value.toDataElement());
        }
        public Builder carbonFootprint(CarbonFootprint value) { return setAspect(KnownProfileAspect.CARBON_FOOTPRINT, value == null ? null : value.toDataElement()); }
        public Builder durability(Durability value) { return setAspect(KnownProfileAspect.DURABILITY, value == null ? null : value.toDataElement()); }
        public Builder environmentalImpact(EnvironmentalImpact value) { return setAspect(KnownProfileAspect.ENVIRONMENTAL_IMPACT, value == null ? null : value.toDataElement()); }
        public Builder healthImpact(HealthImpact value) { return setAspect(KnownProfileAspect.HEALTH_IMPACT, value == null ? null : value.toDataElement()); }
        public Builder productRelationship(ProductRelationship value) { return setAspect(KnownProfileAspect.PRODUCT_RELATIONSHIP, value == null ? null : value.toDataElement()); }
        public Builder recyclability(Recyclability value) { return setAspect(KnownProfileAspect.RECYCLABILITY, value == null ? null : value.toDataElement()); }
        public Builder refurbishment(Refurbishment value) { return setAspect(KnownProfileAspect.REFURBISHMENT, value == null ? null : value.toDataElement()); }
        public Builder reliability(Reliability value) { return setAspect(KnownProfileAspect.RELIABILITY, value == null ? null : value.toDataElement()); }
        public Builder remanufacturing(Remanufacturing value) { return setAspect(KnownProfileAspect.REMANUFACTURING, value == null ? null : value.toDataElement()); }
        public Builder repairability(Repairability value) { return setAspect(KnownProfileAspect.REPAIRABILITY, value == null ? null : value.toDataElement()); }
        public Builder reusability(Reusability value) { return setAspect(KnownProfileAspect.REUSABILITY, value == null ? null : value.toDataElement()); }
        public Builder rightOfEndUsers(RightOfEndUsers value) { return setAspect(KnownProfileAspect.RIGHT_OF_END_USERS, value == null ? null : value.toDataElement()); }
        public Builder safety(Safety value) { return setAspect(KnownProfileAspect.SAFETY, value == null ? null : value.toDataElement()); }
        public Builder traceability(Traceability value) { return setAspect(KnownProfileAspect.TRACEABILITY, value == null ? null : value.toDataElement()); }
        public Builder upgradability(Upgradability value) { return setAspect(KnownProfileAspect.UPGRADABILITY, value == null ? null : value.toDataElement()); }
        public Builder usage(Usage value) { return setAspect(KnownProfileAspect.USAGE, value == null ? null : value.toDataElement()); }
        public Builder wasteManagement(WasteManagement value) { return setAspect(KnownProfileAspect.WASTE_MANAGEMENT, value == null ? null : value.toDataElement()); }

        public Dpp4Fun build() {
            DataElementCollection root = new DataElementCollection.Builder().elementId("$").dataElements(elements).build();
            ProfileElementSupport.requireUniqueKnownIds(root, ORDER.toArray(String[]::new));
            List<String> contentIds = new ArrayList<>(existingContentSpecificationIds);
            Dpp4FunSemanticProfile catalog = Dpp4FunSemanticProfile.loadDefault();
            contentIds.removeAll(catalog.variantContentSpecificationIds().values());
            contentIds.removeIf(catalog.contentSpecificationId()::equals);
            boolean hasCatalogAspect = variant != null || elements.stream().anyMatch(element ->
                    java.util.Arrays.stream(KnownProfileAspect.values()).anyMatch(type -> type.elementId().equals(element.getElementId()))
                            || "characteristics".equals(element.getElementId()));
            if (hasCatalogAspect) contentIds.addAll(catalog.contentSpecificationIds(variant));
            DppHeader.Builder headerBuilder = header == null ? new DppHeader.Builder() : header.toBuilder();
            DppHeader resolvedHeader = headerBuilder
                    .digitalProductPassportId(digitalProductPassportId)
                    .uniqueProductIdentifier(uniqueProductIdentifier)
                    .granularity(granularity)
                    .dppSchemaVersion(SCHEMA_VERSION)
                    .dppStatus(dppStatus)
                    .lastUpdated(lastUpdated == null ? Instant.now() : lastUpdated)
                    .economicOperatorId(economicOperatorId)
                    .facilityId(facilityId)
                    .contentSpecificationIds(List.copyOf(new java.util.LinkedHashSet<>(contentIds))).build();
            DigitalProductPassport result = new DigitalProductPassport.Builder().dppHeader(resolvedHeader).dataElements(elements).build();
            return Dpp4Fun.from(result);
        }

        private Builder set(String id, DataElement value) {
            DataElementCollection root = new DataElementCollection.Builder().elementId("$").dataElements(elements).build();
            boolean catalogRoot = java.util.Arrays.stream(KnownProfileAspect.values())
                    .anyMatch(type -> type.elementId().equals(id)) || "characteristics".equals(id);
            if (catalogRoot) {
                DataElementCollection replacement = value == null ? null : ProfileElementSupport.collection(value, id);
                DataElement existing = ProfileElementSupport.find(root, id);
                if (existing instanceof DataElementCollection oldCollection && replacement != null) {
                    replacement = ProfiledAspectState.mergeForEdit(oldCollection, replacement,
                            "characteristics".equals(id) ? variant : null);
                }
                root = replaceCatalogRoot(root, id, replacement);
                elements = root.getDataElements();
                return this;
            }
            if (value == null || value instanceof DataElementCollection collection && collection.getDataElements().isEmpty()) {
                root = ProfileElementSupport.removeNestedKnown(root, id, List.of(), ORDER);
            } else {
                DataElementCollection group = ProfileElementSupport.collection(value, id);
                root = ProfileElementSupport.patchCollection(root, id, ProfileElementSupport.withId(group, id), ORDER);
            }
            elements = root.getDataElements();
            return this;
        }

        private Builder setTyped(String id, DataElement value) {
            DataElementCollection root = new DataElementCollection.Builder().elementId("$").dataElements(elements).build();
            root = replaceCatalogRoot(root, id, value == null ? null : ProfileElementSupport.collection(value, id));
            elements = root.getDataElements();
            return this;
        }

        private Builder setAspect(KnownProfileAspect type, DataElement value) {
            return setTyped(type.elementId(), value);
        }

        private static DataElementCollection replaceCatalogRoot(DataElementCollection root, String id,
                DataElementCollection replacement) {
            List<DataElement> result = new ArrayList<>();
            boolean inserted = false;
            int targetOrder = ORDER.indexOf(id);
            for (DataElement existing : root.getDataElements()) {
                if (id.equals(existing.getElementId())) {
                    if (replacement != null && !inserted) result.add(replacement);
                    inserted = true;
                    continue;
                }
                if (!inserted && replacement != null) {
                    int existingOrder = ORDER.indexOf(existing.getElementId());
                    if (existingOrder >= 0 && existingOrder > targetOrder) {
                        result.add(replacement);
                        inserted = true;
                    }
                }
                result.add(existing);
            }
            if (!inserted && replacement != null) result.add(replacement);
            return root.toBuilder().dataElements(result).build();
        }
    }
}
