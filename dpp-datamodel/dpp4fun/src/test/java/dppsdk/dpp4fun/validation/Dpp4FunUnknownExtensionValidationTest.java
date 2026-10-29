package dppsdk.dpp4fun.validation;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Dpp4FunUnknownExtensionValidationTest {
    @Test
    void unknownExtensionNamesDoNotAcquireD4Point1Rules() {
        for (String name : new String[] {"documentation", "classification"}) {
            var source = TestDataFactory.validDpp().asDigitalProductPassport();
            var elements = new ArrayList<>(source.getDataElements());
            elements.add(new SingleValuedDataElement.Builder().elementId(name).value("legacy payload").build());
            DigitalProductPassport withExtension = source.toBuilder().dataElements(elements).build();

            var value = Dpp4Fun.from(withExtension);
            assertTrue(new Dpp4FunValidator().validate(value).isValid(), name);
            assertEquals("legacy payload", ((SingleValuedDataElement) value.asDigitalProductPassport()
                    .getDataElements().stream().filter(element -> name.equals(element.getElementId()))
                    .findFirst().orElseThrow()).getValue());
        }
    }
}
