package dppsdk.dpp4fun.model.internal;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

/** Internal selector for the fixed Dpp4Fun profile aspect roots. */
public enum KnownProfileAspect {
    BILL_OF_MATERIALS,
    CARBON_FOOTPRINT,
    DURABILITY,
    ENVIRONMENTAL_IMPACT,
    HEALTH_IMPACT,
    NAMEPLATE,
    PRODUCT_RELATIONSHIP,
    RECYCLABILITY,
    REFURBISHMENT,
    RELIABILITY,
    REMANUFACTURING,
    REPAIRABILITY,
    REUSABILITY,
    RIGHT_OF_END_USERS,
    SAFETY,
    TRACEABILITY,
    UPGRADABILITY,
    USAGE,
    WASTE_MANAGEMENT;

    private static final java.util.Map<String, String> CATALOG_ROOT_IDS =
            dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile.loadDefault().definitions().stream()
                    .filter(entry -> entry.appliesTo().contains("general")
                            && entry.path().equals(entry.elementId()))
                    .flatMap(entry -> entry.aliases().stream().map(alias -> java.util.Map.entry(
                            alias.replaceAll("[^A-Za-z0-9]", "").toLowerCase(java.util.Locale.ROOT),
                            entry.elementId())))
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(
                            java.util.Map.Entry::getKey, java.util.Map.Entry::getValue, (left, right) -> left));

    /** Returns the canonical root elementId from the bundled source catalog. */
    public String elementId() {
        String normalizedName = name().replace("_", "").toLowerCase(java.util.Locale.ROOT);
        String elementId = CATALOG_ROOT_IDS.get(normalizedName);
        if (elementId == null) throw new IllegalStateException("No catalog root for " + name());
        return elementId;
    }
}
