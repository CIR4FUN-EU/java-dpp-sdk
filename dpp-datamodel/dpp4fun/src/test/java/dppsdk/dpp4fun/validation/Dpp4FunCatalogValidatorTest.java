package dppsdk.dpp4fun.validation;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.internal.ProfiledAspectState;
import dppsdk.dpp4fun.model.internal.KnownProfileAspect;
import dppsdk.dpp4fun.model.unit.Dpp4FunQuantity;
import dppsdk.dpp4fun.model.unit.LengthUnit;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile;
import dppsdk.dpp4fun.model.characteristics.Dpp4FunProductVariant;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Dpp4FunCatalogValidatorTest {
    private final Dpp4FunValidator validator = new Dpp4FunValidator();
    private final Dpp4FunSemanticProfile catalog = Dpp4FunSemanticProfile.loadDefault();

    @Test
    void optionalAspectsAndOptionalFieldsAreNotMadeMandatoryByExamples() {
        assertTrue(validator.validate(Dpp4Fun.builder(TestDataFactory.validHeader()).build()).isValid());
        Dpp4Fun bom = dpp(ProfiledAspectState.builder(KnownProfileAspect.BILL_OF_MATERIALS)
                        .field("materialSet[0].name", "Foam")
                        .field("materialSet[0].mandatory", true)
                        .field("materialSet[0].portion", 0.0)
                        .build());
        assertTrue(validator.validate(bom).isValid());
    }

    @Test
    void catalogChecksExpectedKindDatatypeAndDictionaryReference() {
        SingleValuedDataElement color = new SingleValuedDataElement.Builder().elementId("color")
                .dictionaryReference("urn:wrong:dictionary").valueDataType("xsd:double").value(42.0).build();
        DataElementCollection characteristics = new DataElementCollection.Builder().elementId("characteristics")
                .dictionaryReference("https://www.cir4fun.eu/ontology/dpp4fun#Characteristics")
                .dataElements(List.of(color)).build();
        Dpp4Fun wrong = Dpp4Fun.from(passport(Dpp4FunProductVariant.CHAIR, characteristics));

        var issues = validator.validate(wrong).issues();
        assertTrue(issues.stream().anyMatch(issue -> issue.rule().equals("DPP4FUN_RESERVED_PATH_VALUE_DATATYPE")));
        assertTrue(issues.stream().anyMatch(issue -> issue.rule().equals("DPP4FUN_RESERVED_PATH_DICTIONARY_REFERENCE")));
        assertTrue(issues.stream().anyMatch(issue -> issue.rule().equals("DPP4FUN_BINDING_DATATYPE_MISMATCH")));

        MultiValuedDataElement wrongKind = new MultiValuedDataElement.Builder().elementId("color")
                .dataElements(List.of(new SingleValuedDataElement.Builder().elementId("0").value("blue").build())).build();
        Dpp4Fun wrongShape = Dpp4Fun.from(passport(Dpp4FunProductVariant.CHAIR,
                new DataElementCollection.Builder().elementId("characteristics").dataElements(List.of(wrongKind)).build()));
        assertTrue(validator.validate(wrongShape).issues().stream()
                .anyMatch(issue -> issue.rule().equals("DPP4FUN_RESERVED_PATH_KIND")));
    }

    @Test
    void rejectsACharacteristicsFieldFromAnotherProductVariant() {
        DataElementCollection characteristics = new DataElementCollection.Builder().elementId("characteristics")
                .dataElements(List.of(new SingleValuedDataElement.Builder().elementId("headrestIncluded")
                        .value(true).valueDataType("xsd:boolean").dictionaryReference(
                                "https://www.cir4fun.eu/ontology/dpp4fun#HeadrestIncluded").build())).build();
        Dpp4Fun value = Dpp4Fun.from(passport(Dpp4FunProductVariant.CHAIR, characteristics));
        assertTrue(validator.validate(value).issues().stream()
                .anyMatch(issue -> issue.rule().equals("DPP4FUN_CHARACTERISTICS_VARIANT_MATCHES_CONTENT_SPEC")));
    }

    @Test
    void sourceBackedQuantitySlotAndRepeatedIdsPassProfileValidation() {
        var characteristics = ProfiledAspectState.builder(Dpp4FunProductVariant.CHAIR)
                .quantity("length", new Dpp4FunQuantity<>(125, LengthUnit.CENTIMETER)).build();
        Dpp4Fun value = dpp(characteristics);
        assertTrue(validator.validate(value).isValid());
    }

    @Test
    void knownUnitFromWrongDimensionIsReportedForCanonicalData() {
        SingleValuedDataElement wrongWeightUnit = new SingleValuedDataElement.Builder()
                .elementId("weightUnit")
                .dictionaryReference("https://www.cir4fun.eu/ontology/dpp4fun#WeightUnit")
                .valueDataType("xsd:string")
                .value("cm")
                .build();
        DataElementCollection characteristics = new DataElementCollection.Builder()
                .elementId("characteristics")
                .dataElements(List.of(wrongWeightUnit))
                .build();
        Dpp4Fun value = Dpp4Fun.from(passport(Dpp4FunProductVariant.CHAIR, characteristics));

        assertTrue(validator.validate(value).issues().stream()
                .anyMatch(issue -> issue.rule().equals("DPP4FUN_UNIT_DIMENSION_MISMATCH")));
    }

    @Test
    void stringSurfaceReportsUnresolvedSemanticReferenceAsWarning() {
        DataElementCollection characteristics = new DataElementCollection.Builder().elementId("characteristics")
                .dictionaryReference("https://www.cir4fun.eu/ontology/dpp4fun#Characteristics")
                .dataElements(List.of(new SingleValuedDataElement.Builder().elementId("surface")
                        .value("Smooth").valueDataType("xsd:string").build())).build();
        Dpp4Fun value = Dpp4Fun.from(passport(Dpp4FunProductVariant.CHAIR, characteristics));
        var result = validator.validate(value);
        assertTrue(result.isValid(), result.issues().toString());
        assertTrue(result.issues().stream().anyMatch(issue ->
                issue.rule().equals("DPP4FUN_SURFACE_SEMANTIC_BINDING_UNRESOLVED")
                        && issue.severity() == dppsdk.core.validation.ValidationIssue.Severity.WARNING));
    }

    @Test
    void repeatedItemIdsMustFollowTheCatalogPositionPolicy() {
        Dpp4Fun value = dpp(ProfiledAspectState.builder(KnownProfileAspect.BILL_OF_MATERIALS)
                .field("materialSet[0].name", "Foam").build());
        DataElementCollection bom = (DataElementCollection) value.asDigitalProductPassport().getDataElements().get(0);
        MultiValuedDataElement materials = (MultiValuedDataElement) bom.getDataElements().get(0);
        DataElementCollection item = (DataElementCollection) materials.getDataElements().get(0);
        DataElementCollection malformedItem = item.toBuilder().elementId("wrong-item-id").build();
        MultiValuedDataElement malformedMaterials = materials.toBuilder()
                .dataElements(List.of(malformedItem)).build();
        DataElementCollection malformedBom = bom.toBuilder().dataElements(List.of(malformedMaterials)).build();
        DigitalProductPassport malformed = value.asDigitalProductPassport().toBuilder()
                .dataElements(List.of(malformedBom)).build();

        assertTrue(validator.validate(Dpp4Fun.from(malformed)).issues().stream()
                .anyMatch(issue -> issue.rule().equals("DPP4FUN_LIST_ITEM_ID_CANONICAL")));
    }

    @Test
    void languageValueRequirementsStayInMandatoryCoreValidation() {
        java.util.Map<String, String> invalidLanguage = new java.util.HashMap<>();
        invalidLanguage.put(null, null);
        Dpp4Fun value = dpp(ProfiledAspectState.builder(KnownProfileAspect.NAMEPLATE)
                .languages("manufacturerProductDescription", invalidLanguage).build());

        assertTrue(validator.validate(value).isValid(), validator.validate(value).issues().toString());
        assertTrue(new Dpp4FunValidationService().validate(value).issues().stream()
                .anyMatch(issue -> issue.rule().equals("EN18223_MULTILANGUAGE_LANGUAGE_REQUIRED")),
                new Dpp4FunValidationService().validate(value).issues().toString());
    }

    private DigitalProductPassport passport(Dpp4FunProductVariant variant, DataElement characteristics) {
        return new DigitalProductPassport.Builder().dppHeader(TestDataFactory.validHeader().toBuilder()
                .contentSpecificationIds(catalog.contentSpecificationIds(variant)).build())
                .dataElements(List.of(characteristics)).build();
    }

    private Dpp4Fun dpp(ProfiledAspectState aspect) {
        DppHeader header = TestDataFactory.validHeader();
        header = header.toBuilder().contentSpecificationIds(catalog.contentSpecificationIds(aspect.variant())).build();
        return Dpp4Fun.from(new DigitalProductPassport.Builder().dppHeader(header)
                .dataElements(List.of(aspect.asDataElement())).build());
    }
}
