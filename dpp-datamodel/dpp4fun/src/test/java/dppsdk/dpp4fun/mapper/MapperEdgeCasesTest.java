package dppsdk.dpp4fun.mapper;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.dpp4fun.model.Dpp4Fun;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapperEdgeCasesTest {
    @Test void wrappingDoesNotRebuildOrLoseAnyCoreState() {
        Dpp4Fun source = TestDataFactory.validDpp();
        List<DataElement> elements = new ArrayList<>(source.asDigitalProductPassport().getDataElements());
        DataElement extension = new SingleValuedDataElement.Builder().elementId("externalExtension").value("retained").build();
        elements.add(0, extension);
        var passport = source.asDigitalProductPassport().toBuilder().dataElements(elements).build();
        Dpp4Fun wrapped = Dpp4Fun.from(passport);
        assertSame(passport, wrapped.asDigitalProductPassport());
        assertEquals(extension, wrapped.asDigitalProductPassport().getDataElements().get(0));
    }

    @Test void immutableEditsKeepHeaderAndExtensionOrder() {
        Dpp4Fun original = TestDataFactory.validDpp();
        List<DataElement> elements = new ArrayList<>(original.asDigitalProductPassport().getDataElements());
        DataElement first = new SingleValuedDataElement.Builder().elementId("extensionFirst").value(true).build();
        DataElement last = new SingleValuedDataElement.Builder().elementId("extensionLast").value(true).build();
        elements.add(0, first); elements.add(last);
        Dpp4Fun wrapped = Dpp4Fun.from(original.asDigitalProductPassport().toBuilder().dataElements(elements).build());
        Dpp4Fun edited = wrapped.toBuilder().chairCharacteristics(
                wrapped.getChairCharacteristics().toBuilder().color("changed").build()).build();
        assertEquals(List.of("extensionFirst", "extensionLast"),
                edited.asDigitalProductPassport().getDataElements().stream().filter(e -> e.getElementId().startsWith("extension"))
                        .map(DataElement::getElementId).toList());
        assertEquals(wrapped.getHeader(), edited.getHeader());
    }
}
