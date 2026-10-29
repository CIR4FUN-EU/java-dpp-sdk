package demo.producer;

import demo.producer.support.DemoDppFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import dppsdk.core.transport.json.DppJsonCodec;
import dppsdk.core.transport.json.DppJsonException;
import dppsdk.core.validation.ValidationException;
import dppsdk.core.validation.ValidationResult;
import dppsdk.dpp4fun.model.aspect.BillOfMaterials;
import dppsdk.dpp4fun.model.characteristics.ChairCharacteristics;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.aspect.Nameplate;
import dppsdk.dpp4fun.model.composite.Material;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidationService;

class SdkCapabilityDemoRunner {

    private final DemoDppFactory factory = new DemoDppFactory();
    private final Dpp4FunValidationService validationService = new Dpp4FunValidationService();
    private final Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();
    private final DppJsonCodec coreCodec = new DppJsonCodec();

    DemoDppSamples run() {
        DemoDppSamples samples = createSamples();
        ConsoleSupport.header("DPP SDK Capability Demo");
        System.out.println("Purpose  : construct and validate a typed D4.1 model over the canonical EN passport");

        ConsoleSupport.step("Build and validate typed Chair passports");
        ConsoleSupport.dpp(samples.validChairDpp());
        validate(samples.validChairDpp());
        System.out.println("validation: valid");

        ConsoleSupport.step("Create an immutable edit");
        Dpp4Fun edited = editProductCharacteristics(samples.validChairDpp());
        System.out.println("original brand: " + samples.validChairDpp().getNameplate().getBrand());
        System.out.println("edited brand  : " + edited.getNameplate().getBrand());
        System.out.println("original retained unchanged: "
                + samples.validChairDpp().getNameplate().getBrand().equals("Cir4Fun"));

        ConsoleSupport.step("Edit a reusable structured BOM value");
        Dpp4Fun withoutFirstMaterial = removeFirstBomMaterial(samples.validChairDpp());
        System.out.println("materials: " + samples.validChairDpp().getBillOfMaterials().getMaterials().size()
                + " -> " + withoutFirstMaterial.getBillOfMaterials().getMaterials().size());

        ConsoleSupport.step("Round-trip the canonical passport through FULL and COMPRESSED EN JSON");
        String full = codec.toExpandedJson(samples.validChairDpp());
        String compressed = codec.toCompressedJson(samples.validChairDpp());
        Dpp4Fun fromFull = codec.fromExpandedJson(full);
        Dpp4Fun fromCompressed = codec.fromCompressedJson(compressed);
        System.out.println("full canonical equality      : "
                + samples.validChairDpp().asDigitalProductPassport().equals(fromFull.asDigitalProductPassport()));
        System.out.println("compressed semantic equality : "
                + samples.validChairDpp().asDigitalProductPassport().equals(fromCompressed.asDigitalProductPassport()));
        try {
            System.out.println("header from compressed       : "
                    + coreCodec.readHeader(new ObjectMapper().readTree(compressed)));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not inspect compressed DPP header", exception);
        }

        ConsoleSupport.step("Invalid incomplete typed passport is rejected by validation");
        validateInvalid("incomplete header", samples.incompleteDpp());

        ConsoleSupport.step("Malformed JSON is rejected by the SDK codec");
        try {
            codec.fromJson(samples.malformedJson());
        } catch (DppJsonException exception) {
            System.out.println("error     : " + exception.getMessage());
        }

        ConsoleSupport.header("SDK capability demo complete");
        return samples;
    }

    DemoDppSamples createSamples() {
        return new DemoDppSamples(factory.createValidBedDpp(), factory.createValidChairDpp(),
                factory.createUpdatedBedDpp(), factory.createIncompleteDpp(),
                factory.createMalformedJson());
    }

    Dpp4Fun facadeRoundTrip(Dpp4Fun dpp) {
        return Dpp4Fun.from(dpp.asDigitalProductPassport());
    }

    Dpp4Fun jsonRoundTrip(Dpp4Fun dpp) {
        return codec.fromJsonAndValidate(codec.toJson(dpp));
    }

    void validate(Dpp4Fun dpp) {
        ValidationResult result = validationService.validate(dpp);
        if (!result.isValid()) throw new ValidationException("Dpp4Fun validation failed: " + result.issues());
    }

    Dpp4Fun buildStepByStepDpp() { return factory.createValidChairDpp(); }

    Dpp4Fun editProductCharacteristics(Dpp4Fun dpp) {
        ChairCharacteristics characteristics = (ChairCharacteristics) dpp.getCharacteristics();
        return dpp.toBuilder().chairCharacteristics(characteristics.toBuilder().color("Edited graphite").build()).build();
    }

    Dpp4Fun removeFirstBomMaterial(Dpp4Fun dpp) {
        Material material = dpp.getBillOfMaterials().getMaterials().get(0);
        BillOfMaterials edited = dpp.getBillOfMaterials().toBuilder().removeMaterial(material).build();
        return dpp.toBuilder().billOfMaterials(edited).build();
    }

    private void validateInvalid(String label, Dpp4Fun dpp) {
        try {
            validate(dpp);
            System.out.println(label + " : unexpectedly valid");
        } catch (ValidationException exception) {
            System.out.println(label + " : " + exception.getMessage());
        }
    }
}
