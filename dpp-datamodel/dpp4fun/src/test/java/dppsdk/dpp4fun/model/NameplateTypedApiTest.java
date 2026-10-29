package dppsdk.dpp4fun.model;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;
import dppsdk.support.TestDataFactory;

class NameplateTypedApiTest {
    @Test
    void buildsCatalogBackedD4NameplateFields() {
        Nameplate value = Nameplate.builder()
                .manufacturerName("Maker")
                .uriOfManufacturer(URI.create("https://example.org/maker"))
                .manufacturerProductDesignation(Map.of("en", "Chair"))
                .build();

        assertEquals("Maker", value.getManufacturerName());
        assertEquals(URI.create("https://example.org/maker"), value.getUriOfManufacturer());
        assertEquals(Map.of("en", "Chair"), value.getManufacturerProductDesignation());
        assertTrue(((dppsdk.core.model.DataElementCollection) value.toDataElement()).getDataElements().stream()
                .anyMatch(element -> element.getElementId().equals("manufacturerProductDesignation")));

        Dpp4Fun passport = Dpp4Fun.builder(TestDataFactory.validHeader()).nameplate(value).build();
        assertEquals(value, passport.getNameplate());
    }
}
