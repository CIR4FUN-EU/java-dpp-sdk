package dppsdk.support;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DppHeader;
import dppsdk.core.model.Granularity;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.net.URI;

public final class TestDataFactory {
    private TestDataFactory() { }

    public static DppHeader validHeader() {
        return new DppHeader.Builder().digitalProductPassportId("urn:example:dpp:1")
                .uniqueProductIdentifier("urn:example:product:1").granularity(Granularity.MODEL)
                .dppSchemaVersion("1.0").dppStatus("active").lastUpdated(Instant.parse("2026-01-01T00:00:00Z"))
                .economicOperatorId("urn:example:operator:1").contentSpecificationIds(List.of()).build();
    }

    public static Nameplate validNameplate() {
        return Nameplate.builder().gtinCode("GTIN-DEMO-001").internalArticleNumber("ART-001")
                .batchNumber("BATCH-2026").manufacturerName("Demo Manufacturer GmbH")
                .uriOfManufacturer(java.net.URI.create("https://manufacturer.example.com"))
                .glnOfManufacturer("123456789").build();
    }
    public static BillOfMaterials validBillOfMaterials() {
        return BillOfMaterials.builder()
                .addMaterial(new Material.Builder().name("Steel").mandatory(true).portion(2.5).reference(java.net.URI.create("urn:cir4fun:material:MAT-001")).build())
                .addMaterial(new Material.Builder().name("Foam").mandatory(true).portion(5.0).reference(java.net.URI.create("urn:cir4fun:material:MAT-002")).build())
                .addComponent(new Component.Builder().name("Frame Assembly").reference(java.net.URI.create("urn:cir4fun:component:COMP-001")).build())
                .addPart(new Part.Builder().name("Mattress Support").mandatory(true).reference(java.net.URI.create("urn:cir4fun:part:PART-001")).build())
                .build();
    }
    public static Dpp4Fun validDpp() {
        return Dpp4Fun.builder(validHeader()).nameplate(validNameplate())
                .chairCharacteristics(ChairCharacteristics.builder()
                        .length(200.0, LengthUnit.CENTIMETER).height(90.0, LengthUnit.CENTIMETER)
                        .weight(75.0, MassUnit.KILOGRAM).color("navy").build())
                .billOfMaterials(validBillOfMaterials()).build();
    }

    /** A public-typed API fixture covering every general aspect and reusable D4.1 composite family. */
    public static Dpp4Fun comprehensiveDpp4Fun() {
        Certificate certificate = Certificate.builder().name("Circularity certificate")
                .description("Verified material recovery").issueDate("2026-01-01T00:00:00Z")
                .reference(URI.create("https://example.test/certificates/circularity")).build();
        StandardAssessment standard = StandardAssessment.builder().name("Furniture standard")
                .description("Assessment against a published furniture standard")
                .complianceStatus("compliant").reference(URI.create("https://example.test/standards/1")).build();
        Scoring scoring = Scoring.builder().name("Durability score").description("Expected service score")
                .value("A").reference(URI.create("https://example.test/scores/1")).build();
        Instruction instruction = Instruction.builder().name("Assembly")
                .description("Assemble the chair using the supplied fasteners")
                .reference(URI.create("https://example.test/instructions/assembly")).build();

        return Dpp4Fun.builder(validHeader())
                .nameplate(Nameplate.builder().gtinCode("04012345678901").internalArticleNumber("CHAIR-001")
                        .batchNumber("B-2026-01").manufacturerName("Example Furniture")
                        .manufacturerProductDescription(Map.of("en-US", "Recyclable chair", "de-DE", "Recycelbarer Stuhl"))
                        .manufacturerProductDesignation(Map.of("en-US", "Chair", "fr-FR", "Chaise"))
                        .manufacturerProductFamily(Map.of("en-US", "Seating"))
                        .manufacturerProductRoot(Map.of("en-US", "Furniture"))
                        .manufacturerProductOrderSuffix(Map.of("en-US", "Blue"))
                        .uriOfManufacturer(URI.create("https://example.test/manufacturer"))
                        .glnOfManufacturer("4012345000008").hsCodeOfTheWCO("9401")
                        .customsTariffNumber("9401.61").brand("Example")
                        .productType(Map.of("en-US", "Chair", "de-DE", "Stuhl"))
                        .uriOfTheProduct(URI.create("https://example.test/products/chair-001"))
                        .productArticleNumberOfSupplier("SUP-CHAIR-001").nameOfSupplier("Example Supplier")
                        .supplierProductDescription(Map.of("en-US", "Supplier chair"))
                        .supplierProductDesignation(Map.of("en-US", "Chair model one"))
                        .supplierProductFamily(Map.of("en-US", "Seating"))
                        .supplierProductRoot(Map.of("en-US", "Furniture"))
                        .supplierProductOrderSuffix(Map.of("en-US", "001"))
                        .supplierProductType(Map.of("en-US", "Chair"))
                        .uriOfSupplier(URI.create("https://example.test/supplier"))
                        .glnOfSupplier("4012345000015")
                        .addressOfAdditionalLink(URI.create("https://example.test/product-details"))
                        .contact(Contact.builder().organization("Example Support")
                                .address(Address.builder().country("DE").zipCode("10115").region("Berlin")
                                        .town("Berlin").street("Example Street 1").build())
                                .email(Email.builder().emailAddress("support@example.test").typeOfEmail("support").build())
                                .telephone(Telephone.builder().telephoneNumber("+4930123456")
                                        .typeOfTelephone("support").build()).build()).build())
                .chairCharacteristics(ChairCharacteristics.builder()
                        .length(100, LengthUnit.CENTIMETER).width(50, LengthUnit.CENTIMETER)
                        .height(85, LengthUnit.CENTIMETER).diameter(4, LengthUnit.CENTIMETER)
                        .weight(8, MassUnit.KILOGRAM).volume(0.22, VolumeUnit.CUBIC_METER)
                        .seatHeight(46, LengthUnit.CENTIMETER).seatWidth(45, LengthUnit.CENTIMETER)
                        .seatLength(42, LengthUnit.CENTIMETER).armrestHeight(65, LengthUnit.CENTIMETER)
                        .armrestWidth(6, LengthUnit.CENTIMETER).backrestHeight(40, LengthUnit.CENTIMETER)
                        .legHeight(42, LengthUnit.CENTIMETER).surface("Smooth").color("blue")
                        .foamHardnessLevel("medium").seatingCapacity(1).indoorUse(true).outdoorUse(false)
                        .reclining(false).seatSliding(false).swivel(false).waterRepellent(true)
                        .stainResistant(true).easyToClean(true).fireResistant(false)
                        .seatRemovableCover(true).backrestRemovableCover(true).adjustableFeet(true)
                        .armrestIncluded(true).armrestRemovableCover(false).stackable(false)
                        .heightAdjustable(true).padded(true).payload(120.0).build())
                .billOfMaterials(BillOfMaterials.builder()
                        .materials(List.of(Material.builder().name("Steel").mandatory(true).portion(3.2)
                                        .reference(URI.create("https://example.test/materials/steel")).build(),
                                Material.builder().name("Foam").mandatory(false).portion(0.8)
                                        .reference(URI.create("https://example.test/materials/foam")).build()))
                        .components(List.of(Component.builder().name("Frame").mandatory(true)
                                        .reference(URI.create("https://example.test/components/frame")).build(),
                                Component.builder().name("Seat").mandatory(true)
                                        .reference(URI.create("https://example.test/components/seat")).build()))
                        .parts(List.of(Part.builder().name("Leg").mandatory(true)
                                        .reference(URI.create("https://example.test/parts/leg")).build(),
                                Part.builder().name("Fastener").mandatory(false)
                                        .reference(URI.create("https://example.test/parts/fastener")).build())).build())
                .carbonFootprint(CarbonFootprint.builder().calculations(List.of(
                        CarbonFootprintCalculation.builder().co2eq(12.5).phase("production")
                                .quantityOfMeasurementForCalculation("1 chair").impactUnitForCalculation("kg CO2e")
                                .reference(URI.create("https://example.test/carbon/production")).build(),
                        CarbonFootprintCalculation.builder().co2eq(2.0).phase("transport")
                                .quantityOfMeasurementForCalculation("1 chair").impactUnitForCalculation("kg CO2e")
                                .reference(URI.create("https://example.test/carbon/transport")).build())).build())
                .durability(Durability.builder().cycleLife("100000 cycles").expectedLifetime("10 years")
                        .certificates(List.of(certificate)).standards(List.of(standard)).scorings(List.of(scoring)).build())
                .environmentalImpact(EnvironmentalImpact.builder().indicators(List.of(
                        Indicator.builder().name("Water use").description("Production water consumption")
                                .value("10").unit("L").calculationMethod(URI.create("https://example.test/methods/water")).build(),
                        Indicator.builder().name("Energy use").description("Production energy consumption")
                                .value("4").unit("kWh").calculationMethod(URI.create("https://example.test/methods/energy")).build())).build())
                .healthImpact(HealthImpact.builder().indicators(List.of(HealthIndicator.builder()
                        .name("VOC").description("Volatile organic compound information").value("low").unit("class")
                        .calculationMethod(URI.create("https://example.test/methods/voc")).hazardType("chemical").build())).build())
                .productRelationship(ProductRelationship.builder().accessories(List.of(
                        Accessory.builder().name("Cushion").description("Optional cushion")
                                .reference(URI.create("https://example.test/accessories/cushion")).build(),
                        Accessory.builder().name("Cover").description("Replacement cover")
                                .reference(URI.create("https://example.test/accessories/cover")).build()))
                        .collectionMembers(List.of(CollectionMember.builder().name("Chair").description("Collection chair")
                                .collection("Example collection").reference(URI.create("https://example.test/collections/1")).build(),
                                CollectionMember.builder().name("Table").description("Collection table")
                                        .collection("Example collection").reference(URI.create("https://example.test/collections/2")).build())).build())
                .recyclability(Recyclability.builder().recycledContent(0.35).recyclable(true)
                        .recycledMaterialIncluded(true).certificates(List.of(certificate)).standards(List.of(standard))
                        .recyclingInstructions(List.of(instruction)).build())
                .refurbishment(Refurbishment.builder().instructions(List.of(instruction)).build())
                .reliability(Reliability.builder().certificates(List.of(certificate)).standards(List.of(standard))
                        .scorings(List.of(scoring)).build())
                .remanufacturing(Remanufacturing.builder().assemblyInstructions(List.of(instruction))
                        .disassemblyInstructions(List.of(instruction)).build())
                .repairability(Repairability.builder().sparePartsAvailable(true).repairInstructions(List.of(instruction))
                        .repairRecords(List.of(RepairRecord.builder().name("Leg replacement")
                                .description("Replace a damaged leg").operator("Example Repairer")
                                .time("2026-01-02T10:00:00Z").location("Berlin").type("replacement").build())).build())
                .reusability(Reusability.builder().secondHand(true).scorings(List.of(scoring))
                        .scoringInstructions(List.of(instruction)).build())
                .rightOfEndUsers(RightOfEndUsers.builder().postSaleServices(List.of(
                        PostSaleService.builder().name("Repair service").description("Chair repair")
                                .reference(URI.create("https://example.test/services/repair")).build(),
                        PostSaleService.builder().name("Take-back service").description("Product take-back")
                                .reference(URI.create("https://example.test/services/takeback")).build()))
                        .policies(Policies.builder().permissions(List.of("repair", "reuse"))
                                .prohibitions(List.of("burning")).build()).build())
                .safety(Safety.builder().ageRestriction("Adult").professionalUseOnly(false)
                        .certificates(List.of(certificate)).standards(List.of(standard)).build())
                .traceability(Traceability.builder().currentPhase("use").location("Berlin")
                        .designRecords(List.of(DesignRecord.builder().name("Design release")
                                .description("Approved design release").operator("Example Design GmbH")
                                .time("2025-11-01T09:00:00Z").status("approved").version("1.0")
                                .designFile(URI.create("https://example.test/design/chair-v1")).build()))
                        .manufacturingRecords(List.of(ManufacturingRecord.builder().name("Assembly batch")
                                .description("Chair assembly record").operator("Example Factory")
                                .status("complete").startTime("2025-12-20T07:00:00Z").station("line-1")
                                .positionInProcessRouting(URI.create("https://example.test/routing/assembly")).build()))
                        .trackingRecords(List.of(TrackingRecord.builder().name("Shipment")
                                .description("Shipment to distributor").operator("Example Logistics")
                                .time("2026-01-01T08:00:00Z").status("shipped").location("Berlin")
                                .origin("Factory").destination("Retailer").build())).build())
                .upgradability(Upgradability.builder().upgradeOptionsAvailable(true)
                        .upgradeInstructions(List.of(instruction)).build())
                .usage(Usage.builder().operatingHours(100).primaryFunction(ProductFunction.builder()
                                .name("Seating").description("Provides seating").status("primary").build())
                        .secondaryFunctions(List.of(ProductFunction.builder().name("Stacking")
                                .description("Chairs can be stacked").status("secondary").build()))
                        .assemblyInstructions(List.of(instruction)).disassemblyInstructions(List.of(instruction)).build())
                .wasteManagement(WasteManagement.builder().toxicityRisk(false).fireRisk(false).electricityRisk(false)
                        .disposalInstructions(List.of(instruction)).disassemblyInstructions(List.of(instruction)).build())
                .build();
    }
    public static String validDppJson() { return new Dpp4FunJsonCodec().toJson(validDpp()); }
}
