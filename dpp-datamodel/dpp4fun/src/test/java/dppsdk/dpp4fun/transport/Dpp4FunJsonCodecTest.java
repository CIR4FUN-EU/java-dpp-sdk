package dppsdk.dpp4fun.transport;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.transport.json.DppJsonCodec;
import dppsdk.core.transport.json.DppJsonException;
import dppsdk.core.validation.ValidationException;
import dppsdk.core.model.DataElement;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.dpp4fun.model.composite.Address;
import dppsdk.dpp4fun.model.composite.Contact;
import dppsdk.dpp4fun.model.composite.Email;
import dppsdk.dpp4fun.model.aspect.Nameplate;
import dppsdk.dpp4fun.model.composite.Telephone;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Dpp4FunJsonCodecTest {
    private final Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();

    @Test void outputIsExactlyTheCanonicalCoreExpandedTransport() {
        Dpp4Fun value = TestDataFactory.validDpp();
        assertEquals(new DppJsonCodec().toExpandedJson(value.asDigitalProductPassport()), codec.toExpandedJson(value));
    }

    @Test void typedCoreExpandedJsonRoundTripPreservesPassportAndViews() {
        Dpp4Fun value = TestDataFactory.validDpp();
        Dpp4Fun result = codec.fromJson(codec.toJson(value));
        assertEquals(value.asDigitalProductPassport(), result.asDigitalProductPassport());
        assertEquals(value.getChairCharacteristics(), result.getChairCharacteristics());
        assertEquals(value.getBillOfMaterials(), result.getBillOfMaterials());
    }

    @Test void completeTypedProfileRoundTripsEveryPublicViewAndCanonicalPassport() {
        Address address = new Address.Builder().country("DE").zipCode("10115").region("BE")
                .town("Berlin").street("Example 1").build();
        Contact contact = new Contact.Builder().organization("Maker HQ").address(address)
                .email(new Email.Builder().emailAddress("help@example.test").typeOfEmail("support").build())
                .telephone(new Telephone.Builder().telephoneNumber("+49-30-123").typeOfTelephone("office").build())
                .build();
        Dpp4Fun original = TestDataFactory.validDpp().toBuilder()
                .nameplate(Nameplate.builder().gtinCode("GTIN-1").internalArticleNumber("A-1")
                        .batchNumber("B-1").customsTariffNumber("T-1")
                        .uriOfTheProduct(URI.create("https://product.example.test"))
                        .manufacturerName("Maker GmbH").glnOfManufacturer("123")
                        .manufacturerProductDescription(Map.of("en", "Description"))
                        .manufacturerProductDesignation(Map.of("en", "Designation"))
                        .manufacturerProductFamily(Map.of("en", "Family"))
                        .manufacturerProductRoot(Map.of("en", "Root"))
                        .manufacturerProductOrderSuffix(Map.of("en", "Suffix"))
                        .uriOfManufacturer(URI.create("https://maker.example.test"))
                        .nameOfSupplier("Demo Supplier Ltd")
                        .contact(contact).build())
                .build();
        String json = codec.toExpandedJson(original);

        assertFalse(json.contains("dppHeader"));
        assertFalse(json.contains("PassportMetadata"));
        assertEquals(new DppJsonCodec().toExpandedJson(original.asDigitalProductPassport()), json);
        Dpp4Fun decoded = codec.fromExpandedJson(json);
        assertEquals(original.asDigitalProductPassport(), decoded.asDigitalProductPassport());
        assertEquals(original, decoded);
        assertEquals(original.getNameplate(), decoded.getNameplate());
        assertEquals(original.getNameplate().getContact(), decoded.getNameplate().getContact());
        assertEquals(original.getNameplate().getContact().getAddress(), decoded.getNameplate().getContact().getAddress());
        assertEquals(original.getNameplate().getContact().getEmail(), decoded.getNameplate().getContact().getEmail());
        assertEquals(original.getNameplate().getContact().getTelephone(), decoded.getNameplate().getContact().getTelephone());
        assertEquals(original.getChairCharacteristics(), decoded.getChairCharacteristics());
        assertEquals(original.getBillOfMaterials(), decoded.getBillOfMaterials());
    }

    @Test void fromJsonAndValidateIsTheValidationConvenience() {
        Dpp4Fun value = TestDataFactory.validDpp();
        assertEquals(value, codec.fromJsonAndValidate(codec.toJson(value)));
        Dpp4Fun profileInvalid = Dpp4Fun.builder(TestDataFactory.validHeader()).build();
        assertNotNull(codec.fromJson(codec.toJson(profileInvalid)));
        assertEquals(profileInvalid, codec.fromJsonAndValidate(codec.toJson(profileInvalid)));
    }

    @Test void fromJsonAndValidateRunsMandatoryCoreValidation() {
        DigitalProductPassport coreInvalid = new DigitalProductPassport.Builder()
                .dppHeader(TestDataFactory.validHeader())
                .dataElements(List.of(new SingleValuedDataElement.Builder().elementId("genericExtension")
                        .valueDataType("xsd:ID").value("not-an-id").build())).build();
        String json = new DppJsonCodec().toExpandedJson(coreInvalid);
        assertThrows(ValidationException.class, () -> codec.fromJsonAndValidate(json));
    }

    @Test void parsingRejectsMalformedExpandedJson() {
        DppJsonException malformed = assertThrows(DppJsonException.class, () -> codec.fromJson("{not-json"));
        assertNotNull(malformed.getCause());
        assertThrows(DppJsonException.class, () -> codec.fromJson("{}"));
        assertThrows(DppJsonException.class, () -> codec.fromJson("{\"elements\":\"wrong\"}"));
    }

    @Test void transportFailuresThroughFacadeMatchFrozenCoreContract() {
        String valid = codec.toExpandedJson(TestDataFactory.validDpp());
        String duplicateKey = valid.replaceFirst("\"granularity\":\"model\"",
                "\"granularity\":\"model\",\"granularity\":\"item\"");
        String wrongRoot = "[]";
        String unknownObjectType = valid.replaceFirst("\"objectType\":\"DataElementCollection\"",
                "\"objectType\":\"FutureType\"");
        String wrongFieldType = valid.replaceFirst("\"elementId\":\"nameplate\"",
                "\"elementId\":[]");
        Matcher multiValue = Pattern.compile("(\"objectType\":\"MultiValuedDataElement\"[^}]*?)(\"elements\":\\[)")
                .matcher(valid);
        assertTrue(multiValue.find(), "representative profile must contain a MultiValuedDataElement");
        String ambiguousMultiValue = multiValue.replaceFirst("$1\"value\":[],$2");
        String invalidHeader = valid.replace("\"lastUpdated\":\"2026-01-01T00:00:00Z\"",
                "\"lastUpdated\":\"not-an-instant\"");

        for (String invalidJson : new String[] {"{not-json", duplicateKey, wrongRoot,
                unknownObjectType, wrongFieldType,
                ambiguousMultiValue, invalidHeader}) {
            assertTransportMatchesCore(invalidJson);
        }

        DppJsonException coreNull = assertThrows(DppJsonException.class,
                () -> new DppJsonCodec().toExpandedJson(null));
        DppJsonException facadeNull = assertThrows(DppJsonException.class,
                () -> codec.toExpandedJson(null));
        assertEquals(coreNull.getMessage(), facadeNull.getMessage());
        if (coreNull.getCause() == null) assertNull(facadeNull.getCause());
        else assertEquals(coreNull.getCause().getClass(), facadeNull.getCause().getClass());
    }

    @Test void malformedReservedProfileStructureHasDocumentedInterpretationFailure() {
        Dpp4Fun source = TestDataFactory.validDpp();
        var elements = new java.util.ArrayList<>(source.asDigitalProductPassport().getDataElements());
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).getElementId().equals("characteristics")) {
                elements.set(i, new SingleValuedDataElement.Builder().elementId("characteristics").value("wrong-shape").build());
            }
        }
        elements.add(new SingleValuedDataElement.Builder().elementId("characteristics").value("wrong-shape").build());
        DigitalProductPassport malformedProfile = source.asDigitalProductPassport().toBuilder().dataElements(elements).build();
        String structurallyValidJson = new DppJsonCodec().toExpandedJson(malformedProfile);

        assertThrows(IllegalArgumentException.class, () -> codec.fromJson(structurallyValidJson));
    }

    private void assertTransportMatchesCore(String json) {
        DppJsonException coreFailure = assertThrows(DppJsonException.class,
                () -> new DppJsonCodec().fromExpandedJson(json), json);
        DppJsonException facadeFailure = assertThrows(DppJsonException.class, () -> codec.fromJson(json), json);
        assertEquals(coreFailure.getMessage(), facadeFailure.getMessage());
        if (coreFailure.getCause() == null) assertNull(facadeFailure.getCause());
        else {
            assertNotNull(facadeFailure.getCause());
            assertEquals(coreFailure.getCause().getClass(), facadeFailure.getCause().getClass());
        }
    }
}
