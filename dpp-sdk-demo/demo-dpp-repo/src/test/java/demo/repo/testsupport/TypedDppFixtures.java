package demo.repo.testsupport;

import dppsdk.core.model.Granularity;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Typed, source-aligned fixtures used by repository integration tests. */
public final class TypedDppFixtures {
    private TypedDppFixtures() { }

    public static Dpp4Fun comprehensiveChair(String dppId, String productId) {
        URI reference = URI.create("https://example.test/dpp/reference");
        Instruction instruction = Instruction.builder().name("Assembly")
                .description("Assemble using the supplied steps.").reference(reference).build();
        Certificate certificate = Certificate.builder().name("Furniture safety certificate")
                .description("Test certificate.").reference(reference).build();
        StandardAssessment assessment = StandardAssessment.builder().name("EN furniture assessment")
                .description("Assessment record.").reference(reference).build();
        Scoring scoring = Scoring.builder().name("durability score").description("Test score")
                .value("A").reference(reference).build();

        return Dpp4Fun.builder()
                .digitalProductPassportId(dppId)
                .uniqueProductIdentifier(productId)
                .granularity(Granularity.MODEL)
                .economicOperatorId("urn:example:operator:cir4fun")
                .dppStatus("active")
                .lastUpdated(Instant.parse("2026-04-24T00:00:00Z"))
                .nameplate(Nameplate.builder().gtinCode(productId).internalArticleNumber("C4F-CHAIR-001")
                        .manufacturerName("Cir4Fun Furniture GmbH")
                        .manufacturerProductDesignation(Map.of("en-GB", "Office chair", "de-DE", "Bürostuhl"))
                        .uriOfManufacturer(URI.create("https://example.test/operators/manufacturer"))
                        .glnOfManufacturer("4000001000005").hsCodeOfTheWCO("940139").brand("Cir4Fun")
                        .productType(Map.of("en-GB", "Chair", "de-DE", "Stuhl"))
                        .uriOfTheProduct(URI.create("https://example.test/products/chair")).build())
                .billOfMaterials(BillOfMaterials.builder()
                        .addMaterial(Material.builder().name("FSC beech").mandatory(false).portion(60.0)
                                .reference(reference).build())
                        .addMaterial(Material.builder().name("Recycled steel").portion(40.0).reference(reference).build())
                        .addComponent(Component.builder().name("Frame").reference(reference).build())
                        .addPart(Part.builder().name("Seat pan").mandatory(true).reference(reference).build())
                        .build())
                .carbonFootprint(CarbonFootprint.builder().calculations(List.of(
                        CarbonFootprintCalculation.builder().reference(reference).co2eq(42.5).phase("production")
                                .quantityOfMeasurementForCalculation("per product").impactUnitForCalculation("kg CO2e")
                                .build())).build())
                .durability(Durability.builder().cycleLife("100000 cycles").expectedLifetime("10 years")
                        .certificates(List.of(certificate)).standards(List.of(assessment)).scorings(List.of(scoring)).build())
                .environmentalImpact(EnvironmentalImpact.builder().indicators(List.of(
                        Indicator.builder().name("climate impact").description("Climate indicator")
                                .value("42.5").unit("kg CO2e").calculationMethod(reference).build())).build())
                .healthImpact(HealthImpact.builder().indicators(List.of(
                        HealthIndicator.builder().name("emissions").description("Indoor emissions")
                                .value("low").unit("class").hazardType("chemical").calculationMethod(reference)
                                .build())).build())
                .productRelationship(ProductRelationship.builder()
                        .accessories(List.of(Accessory.builder().name("replacement arm pad")
                                .description("Optional accessory").reference(reference).build()))
                        .collectionMembers(List.of(CollectionMember.builder().name("office range")
                                .description("Product range").collection("Office").reference(reference).build()))
                        .build())
                .recyclability(Recyclability.builder().recycledContent(35.0).recyclable(true)
                        .recycledMaterialIncluded(true).certificates(List.of(certificate)).standards(List.of(assessment))
                        .recyclingInstructions(List.of(instruction)).build())
                .refurbishment(Refurbishment.builder().instructions(List.of(instruction)).build())
                .reliability(Reliability.builder().certificates(List.of(certificate)).standards(List.of(assessment))
                        .scorings(List.of(scoring)).build())
                .remanufacturing(Remanufacturing.builder().assemblyInstructions(List.of(instruction))
                        .disassemblyInstructions(List.of(instruction)).build())
                .repairability(Repairability.builder().sparePartsAvailable(true).repairInstructions(List.of(instruction))
                        .repairRecords(List.of(RepairRecord.builder().name("seat replacement")
                                .description("Replaced the seat assembly.").operator("service partner")
                                .time("2026-01-15T10:00:00Z").location("Berlin").type("replacement").build()))
                        .build())
                .reusability(Reusability.builder().secondHand(true).scorings(List.of(scoring))
                        .scoringInstructions(List.of(instruction)).build())
                .rightOfEndUsers(RightOfEndUsers.builder()
                        .postSaleServices(List.of(PostSaleService.builder().name("repair service")
                                .description("Repair support").reference(reference).build()))
                        .policies(Policies.builder().permissions(List.of("repair", "resell"))
                                .prohibitions(List.of("discard without sorting")).build()).build())
                .safety(Safety.builder().ageRestriction("adult").professionalUseOnly(false)
                        .certificates(List.of(certificate)).standards(List.of(assessment)).build())
                .traceability(Traceability.builder().currentPhase("distribution").location("Berlin")
                        .designRecords(List.of(DesignRecord.builder().name("chair design")
                                .description("Approved design").operator("design team").time("2025-01-10T09:00:00Z")
                                .status("approved").designFile(reference).build()))
                        .manufacturingRecords(List.of(ManufacturingRecord.builder().name("chair production")
                                .description("Manufactured chair").operator("factory")
                                .startTime("2025-02-10T09:00:00Z").status("complete").station("line 1").build()))
                        .trackingRecords(List.of(TrackingRecord.builder().name("shipment")
                                .description("Shipped to distributor").operator("carrier")
                                .time("2025-03-10T09:00:00Z").status("shipped").origin("Berlin")
                                .destination("Hamburg").build())).build())
                .upgradability(Upgradability.builder().upgradeOptionsAvailable(true)
                        .upgradeInstructions(List.of(instruction)).build())
                .usage(Usage.builder().operatingHours(8000)
                        .primaryFunction(ProductFunction.builder().name("seating").description("Office seating")
                                .status("primary").build())
                        .secondaryFunctions(List.of(ProductFunction.builder().name("reclining").status("secondary").build()))
                        .assemblyInstructions(List.of(instruction)).disassemblyInstructions(List.of(instruction)).build())
                .wasteManagement(WasteManagement.builder().toxicityRisk(false).fireRisk(false).electricityRisk(false)
                        .disposalInstructions(List.of(instruction)).disassemblyInstructions(List.of(instruction)).build())
                .chairCharacteristics(ChairCharacteristics.builder().length(70, LengthUnit.CENTIMETER)
                        .width(65, LengthUnit.CENTIMETER).height(110, LengthUnit.CENTIMETER)
                        .seatHeight(45, LengthUnit.CENTIMETER).seatWidth(48, LengthUnit.CENTIMETER)
                        .seatLength(46, LengthUnit.CENTIMETER).weight(18, MassUnit.KILOGRAM)
                        .surface("Woven matte").color("graphite").swivel(true).armrestIncluded(true).build())
                .build();
    }

    public static Dpp4Fun variant(String dppId, String productId, Dpp4FunProductVariant variant) {
        Dpp4Fun.Builder builder = Dpp4Fun.builder().digitalProductPassportId(dppId)
                .uniqueProductIdentifier(productId).granularity(Granularity.MODEL)
                .economicOperatorId("urn:example:operator:cir4fun").dppStatus("active")
                .nameplate(Nameplate.builder().gtinCode(productId).manufacturerName("Cir4Fun Furniture GmbH").build());
        switch (variant) {
            case CHAIR -> builder.chairCharacteristics(ChairCharacteristics.builder().color("graphite").build());
            case CRIB -> builder.cribCharacteristics(CribCharacteristics.builder().mattressIncluded(true).build());
            case FABRIC -> builder.fabricCharacteristics(FabricCharacteristics.builder()
                    .flameRetardant(true).colorFastness(5).build());
            case LOCKER -> builder.lockerCharacteristics(LockerCharacteristics.builder().lockIncluded(true).build());
            case PARTICLE_BOARD -> builder.particleBoardCharacteristics(
                    ParticleBoardCharacteristics.builder().carb2Epa(true).build());
            case SOFA -> builder.sofaCharacteristics(SofaCharacteristics.builder().headrestIncluded(true).build());
        }
        return builder.build();
    }
}
