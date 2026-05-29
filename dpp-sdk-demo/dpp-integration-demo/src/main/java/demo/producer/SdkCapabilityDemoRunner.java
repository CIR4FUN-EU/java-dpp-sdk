package demo.producer;

import demo.producer.support.DemoDppFactory;
import dppsdk.core.model.Documentation;
import dppsdk.core.model.DppCore;
import dppsdk.core.model.Nameplate;
import dppsdk.core.model.Organization;
import dppsdk.core.model.OrganizationRole;
import dppsdk.core.model.PassportMetadata;
import dppsdk.core.validation.ValidationException;
import dppsdk.dpp4fun.mapper.Dpp4FunMapper;
import dppsdk.dpp4fun.model.BillOfMaterials;
import dppsdk.dpp4fun.model.Characteristics;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.Material;
import dppsdk.dpp4fun.model.ProductClassification;
import dppsdk.dpp4fun.payload.Dpp4FunPayload;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;
import java.time.LocalDate;
import java.util.UUID;

class SdkCapabilityDemoRunner {

    private final DemoDppFactory factory = new DemoDppFactory();
    private final Dpp4FunValidationService validationService = new Dpp4FunValidationService();
    private final Dpp4FunMapper mapper = new Dpp4FunMapper();
    private final Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();

    DemoDppSamples run() {
        DemoDppSamples samples = createSamples();

        ConsoleSupport.header("DPP SDK Capability Demo");
        System.out.println("Purpose  : build, validate, map, and serialize DPPs with the SDK");

        ConsoleSupport.step("Build immutable DPPs through SDK builders");
        ConsoleSupport.dpp(samples.validBedDpp());
        ConsoleSupport.dpp(samples.validChairDpp());

        ConsoleSupport.step("Build one full DPP step by step with SDK model builders");
        Dpp4Fun stepByStepDpp = buildStepByStepDpp();
        ConsoleSupport.dpp(stepByStepDpp);
        System.out.println("validated : " + validateAndReturnStatus(stepByStepDpp));

        ConsoleSupport.step("SDK builders reject missing required fields immediately");
        try {
            buildDppMissingRequiredClassification();
        } catch (IllegalArgumentException exception) {
            System.out.println("exception : " + exception.getClass().getSimpleName());
            System.out.println("message   : " + exception.getMessage());
        }

        ConsoleSupport.step("Validate complete DPPs through ValidationService");
        validateValid("bed", samples.validBedDpp());
        validateValid("chair", samples.validChairDpp());

        ConsoleSupport.step("Validation rejects semantic errors");
        validateInvalid("wrong supplier role", samples.invalidSupplierRoleDpp());
        validateInvalid("downloadable documentation without links", samples.invalidDocumentationDpp());

        ConsoleSupport.step("Update by creating a new immutable DPP");
        System.out.println("original  : " + samples.validBedDpp().getCharacteristics().getProductName());
        System.out.println("updated   : " + samples.updatedBedDpp().getCharacteristics().getProductName());

        ConsoleSupport.step("Immutable edit and delete examples");
        runImmutableEditExamples(samples.validBedDpp());

        ConsoleSupport.step("Map domain DPP to payload and back");
        Dpp4FunPayload payload = mapper.toPayload(samples.validBedDpp());
        Dpp4Fun mappedBack = mapper.toDomain(payload);
        System.out.println("sameId    : " + DemoDppFactory.idOf(mappedBack).equals(DemoDppFactory.idOf(samples.validBedDpp())));
        System.out.println("product   : " + mappedBack.getCharacteristics().getProductName());

        ConsoleSupport.step("Serialize, parse, and validate JSON transport");
        String json = codec.toJson(samples.validBedDpp());
        Dpp4Fun parsed = codec.fromJson(json);
        Dpp4Fun parsedAndValidated = codec.fromJsonAndValidate(json);
        System.out.println("jsonChars : " + json.length());
        System.out.println("preview   : " + jsonPreview(json));
        System.out.println("parsedId  : " + DemoDppFactory.idOf(parsed));
        System.out.println("validId   : " + DemoDppFactory.idOf(parsedAndValidated));

        ConsoleSupport.step("Malformed JSON is rejected by the SDK codec");
        try {
            codec.fromJson(samples.malformedJson());
        } catch (IllegalArgumentException exception) {
            System.out.println("error     : " + exception.getMessage());
        }

        ConsoleSupport.header("SDK capability demo complete");
        return samples;
    }

    DemoDppSamples createSamples() {
        return new DemoDppSamples(
                factory.createValidBedDpp(),
                factory.createValidChairDpp(),
                factory.createUpdatedBedDpp(),
                factory.createDppWithWrongSupplierRole(),
                factory.createDppWithInvalidDocumentation(),
                factory.createMalformedJson()
        );
    }

    Dpp4Fun mapperRoundTrip(Dpp4Fun dpp) {
        return mapper.toDomain(mapper.toPayload(dpp));
    }

    Dpp4Fun jsonRoundTrip(Dpp4Fun dpp) {
        return codec.fromJsonAndValidate(codec.toJson(dpp));
    }

    void validate(Dpp4Fun dpp) {
        validationService.validate(dpp);
    }

    Dpp4Fun buildStepByStepDpp() {
        PassportMetadata metadata = new PassportMetadata.Builder()
                .uniqueProductIdentifier(UUID.fromString("2d1fdfc2-79a2-49f5-b06e-09745f7d2a02"))
                .addPassportUpdateDate(LocalDate.of(2026, 4, 24))
                .qrCodeOrDigitalTag("https://demo.example/dpp/2d1fdfc2-79a2-49f5-b06e-09745f7d2a02")
                .externalDocumentationLink("https://demo.example/docs/lounge-chair")
                .build();

        ProductClassification classification = new ProductClassification.Builder()
                .sector("Furniture")
                .group("Home and office furniture")
                .category("Chairs")
                .addTag("cir4fun")
                .addTag("step-by-step")
                .build();

        Characteristics characteristics = new Characteristics.Builder()
                .productName("Cir4Fun Lounge Chair")
                .description("Step-by-step SDK builder example")
                .brand("Cir4Fun")
                .productType("Chair")
                .weight(18.0)
                .color("Forest green")
                .addFeature("repairable")
                .addFeature("recyclable")
                .build();

        Organization manufacturer = new Organization.Builder()
                .name("Cir4Fun Furniture GmbH")
                .role(OrganizationRole.MANUFACTURER)
                .uri("https://demo.example/organizations/cir4fun-furniture-gmbh")
                .build();

        Organization supplier = new Organization.Builder()
                .name("Partner Supplier GmbH")
                .role(OrganizationRole.SUPPLIER)
                .uri("https://demo.example/organizations/partner-supplier-gmbh")
                .build();

        Nameplate nameplate = new Nameplate.Builder()
                .gtinCode("04012345678902")
                .manufacturer(manufacturer)
                .supplier(supplier)
                .build();

        Documentation documentation = new Documentation.Builder()
                .digitalInstructionsLink("https://demo.example/docs/lounge-assembly")
                .safetyInstructionsLink("https://demo.example/docs/lounge-safety")
                .downloadable(true)
                .availableForYears(10)
                .build();

        DppCore coreDpp = new DppCore.Builder()
                .passportMetadata(metadata)
                .nameplate(nameplate)
                .documentation(documentation)
                .build();

        BillOfMaterials billOfMaterials = new BillOfMaterials.Builder()
                .addMaterial(new Material.Builder()
                        .name("FSC certified wood")
                        .mandatory(true)
                        .portion(70.0)
                        .reference("MAT-WOOD-STEP")
                        .build())
                .build();

        return new Dpp4Fun.Builder()
                .coreDpp(coreDpp)
                .classification(classification)
                .characteristics(characteristics)
                .billOfMaterials(billOfMaterials)
                .build();
    }

    Dpp4Fun buildDppMissingRequiredClassification() {
        PassportMetadata metadata = new PassportMetadata.Builder()
                .uniqueProductIdentifier(UUID.fromString("65ec9190-fdc6-4097-b8e9-f4a55326e9aa"))
                .addPassportUpdateDate(LocalDate.of(2026, 4, 24))
                .build();

        Characteristics characteristics = new Characteristics.Builder()
                .productName("Incomplete Demo Chair")
                .productType("Chair")
                .build();

        Nameplate nameplate = new Nameplate.Builder()
                .gtinCode("04012345678903")
                .manufacturer(new Organization.Builder()
                        .name("Cir4Fun Furniture GmbH")
                        .role(OrganizationRole.MANUFACTURER)
                        .build())
                .build();

        DppCore coreDpp = new DppCore.Builder()
                .passportMetadata(metadata)
                .nameplate(nameplate)
                .build();

        return new Dpp4Fun.Builder()
                .coreDpp(coreDpp)
                // classification is intentionally omitted to demonstrate SDK required-field protection.
                .characteristics(characteristics)
                .build();
    }

    private void runImmutableEditExamples(Dpp4Fun original) {
        Dpp4Fun editedCharacteristics = editProductCharacteristics(original);
        System.out.println("product   : "
                + original.getCharacteristics().getProductName()
                + " -> "
                + editedCharacteristics.getCharacteristics().getProductName());
        System.out.println("validated : " + validateAndReturnStatus(editedCharacteristics));

        Dpp4Fun editedCore = editCoreDocumentationLink(original);
        System.out.println("docLink   : "
                + original.getDocumentation().getDigitalInstructionsLink()
                + " -> "
                + editedCore.getDocumentation().getDigitalInstructionsLink());
        System.out.println("validated : " + validateAndReturnStatus(editedCore));

        Dpp4Fun withoutDocumentation = removeOptionalDocumentation(original);
        System.out.println("docs      : "
                + (original.getDocumentation() != null ? "present" : "missing")
                + " -> "
                + (withoutDocumentation.getDocumentation() != null ? "present" : "removed"));
        System.out.println("validated : " + validateAndReturnStatus(withoutDocumentation));

        Dpp4Fun withoutMaterial = removeFirstBomMaterial(original);
        System.out.println("materials : "
                + original.getBillOfMaterials().getMaterials().size()
                + " -> "
                + withoutMaterial.getBillOfMaterials().getMaterials().size());
        System.out.println("validated : " + validateAndReturnStatus(withoutMaterial));
    }

    Dpp4Fun editProductCharacteristics(Dpp4Fun dpp) {
        Characteristics characteristics = dpp.getCharacteristics().toBuilder()
                .productName(dpp.getCharacteristics().getProductName() + " - Edited")
                .build();
        return dpp.toBuilder()
                .characteristics(characteristics)
                .build();
    }

    Dpp4Fun editCoreDocumentationLink(Dpp4Fun dpp) {
        Documentation documentation = dpp.getDocumentation().toBuilder()
                .digitalInstructionsLink("https://demo.example/docs/assembly-v2")
                .build();
        DppCore coreDpp = dpp.getCoreDpp().toBuilder()
                .documentation(documentation)
                .build();
        return dpp.toBuilder()
                .coreDpp(coreDpp)
                .build();
    }

    Dpp4Fun removeOptionalDocumentation(Dpp4Fun dpp) {
        PassportMetadata metadata = dpp.getPassportMetadata().toBuilder()
                .externalDocumentationLink(null)
                .build();
        DppCore coreDpp = dpp.getCoreDpp().toBuilder()
                .passportMetadata(metadata)
                .documentation(null)
                .build();
        return dpp.toBuilder()
                .coreDpp(coreDpp)
                .build();
    }

    Dpp4Fun removeFirstBomMaterial(Dpp4Fun dpp) {
        Material material = dpp.getBillOfMaterials().getMaterials().get(0);
        BillOfMaterials billOfMaterials = dpp.getBillOfMaterials().toBuilder()
                .removeMaterial(material)
                .build();
        return dpp.toBuilder()
                .billOfMaterials(billOfMaterials)
                .build();
    }

    private void validateValid(String label, Dpp4Fun dpp) {
        validationService.validate(dpp);
        System.out.println(label + "      : valid");
    }

    private String validateAndReturnStatus(Dpp4Fun dpp) {
        validationService.validate(dpp);
        return "valid";
    }

    private void validateInvalid(String label, Dpp4Fun dpp) {
        try {
            validationService.validate(dpp);
            System.out.println(label + " : unexpectedly valid");
        } catch (ValidationException exception) {
            System.out.println(label + " : " + exception.getMessage());
        }
    }

    private String jsonPreview(String json) {
        String compact = json.replace("\r", "").replace("\n", "");
        return compact.length() <= 120 ? compact : compact.substring(0, 120) + "...";
    }
}
