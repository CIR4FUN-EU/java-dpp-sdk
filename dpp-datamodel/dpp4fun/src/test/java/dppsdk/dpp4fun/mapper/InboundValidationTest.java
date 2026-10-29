package dppsdk.dpp4fun.mapper;
import dppsdk.dpp4fun.model.aspect.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.*;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InboundValidationTest {
    @Test void fromRejectsReservedProfileIdWithWrongCoreSubtype() {
        DataElement wrong = new SingleValuedDataElement.Builder().elementId("nameplate").value("wrong shape").build();
        DigitalProductPassport passport = new DigitalProductPassport.Builder().dppHeader(TestDataFactory.validHeader())
                .dataElements(List.of(wrong)).build();
        assertThrows(IllegalArgumentException.class, () -> Dpp4Fun.from(passport));
    }

    @Test void fromRejectsDuplicateReservedRootIds() {
        DataElementCollection first = new DataElementCollection.Builder().elementId("nameplate")
                .dataElements(List.of(new SingleValuedDataElement.Builder().elementId("gtinCode").value("one").build())).build();
        DataElementCollection second = new DataElementCollection.Builder().elementId("nameplate")
                .dataElements(List.of(new SingleValuedDataElement.Builder().elementId("gtinCode").value("two").build())).build();
        DigitalProductPassport passport = new DigitalProductPassport.Builder().dppHeader(TestDataFactory.validHeader())
                .dataElements(List.of(first, second)).build();
        assertThrows(IllegalArgumentException.class, () -> Dpp4Fun.from(passport));
    }
}
