package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.assertEquals;

class Dpp4FunProductDataParityTest {

    @Test
    void retainsEveryProductModelFieldThroughBuildersAndJsonRoundTrip() {
        Address address = new Address.Builder()
                .country("DE").zipCode("10115").region("Berlin")
                .town("Berlin").street("Example Street 1").build();
        Contact contact = new Contact.Builder()
                .organization("Product support")
                .address(address)
                .email(new Email.Builder().emailAddress("help@example.com").typeOfEmail("work").build())
                .telephone(new Telephone.Builder().telephoneNumber("+4930123456")
                        .typeOfTelephone("office").build())
                .build();
        Nameplate nameplate = Nameplate.builder()
                .gtinCode("12345678901234")
                .internalArticleNumber("article")
                .batchNumber("batch")
                .manufacturerName("Example Manufacturing")
                .manufacturerProductDescription(Map.of("en", "Description"))
                .manufacturerProductDesignation(Map.of("en", "Designation"))
                .manufacturerProductFamily(Map.of("en", "Family"))
                .manufacturerProductRoot(Map.of("en", "Root"))
                .manufacturerProductOrderSuffix(Map.of("en", "Suffix"))
                .uriOfManufacturer(URI.create("https://example.com/manufacturer"))
                .glnOfManufacturer("1234567890123")
                .nameOfSupplier("Example Supplier")
                .uriOfSupplier(URI.create("https://example.com/supplier"))
                .customsTariffNumber("tariff")
                .uriOfTheProduct(URI.create("https://example.com/product"))
                .contact(contact)
                .build();
        assertEquals(address, address.toBuilder().build());
        assertEquals(contact, contact.toBuilder().build());
        assertEquals(nameplate, nameplate.toBuilder().build());

        Dpp4Fun dpp = TestDataFactory.validDpp().toBuilder().nameplate(nameplate).build();
        Dpp4FunJsonCodec codec = new Dpp4FunJsonCodec();
        assertEquals(dpp, codec.fromJson(codec.toJson(dpp)));
    }
}
