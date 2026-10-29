package dppsdk.dpp4fun.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.dpp4fun.model.aspect.Safety;
import dppsdk.dpp4fun.model.internal.KnownProfileAspect;
import dppsdk.dpp4fun.model.internal.ProfiledAspectState;
import dppsdk.dpp4fun.semantic.Dpp4FunSemanticProfile;
import dppsdk.dpp4fun.transport.Dpp4FunJsonCodec;
import dppsdk.dpp4fun.validation.Dpp4FunValidator;
import dppsdk.support.TestDataFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Dpp4FunProfessionalUseOnlyTest {
    @Test
    void keepsProfessionalUseOnlyBooleanThroughTypedModelValidationAndJson() throws Exception {
        Dpp4Fun value = Dpp4Fun.builder(TestDataFactory.validHeader())
                .safety(Safety.builder().professionalUseOnly(true).build())
                .build();

        assertEquals(Boolean.TRUE, value.getSafety().getProfessionalUseOnly());
        assertTrue(new Dpp4FunValidator().validate(value).isValid());
        JsonNode root = new ObjectMapper().readTree(new Dpp4FunJsonCodec().toJson(value));
        JsonNode field = findValue(root, "professionalUseOnly");
        assertTrue(field != null, "professionalUseOnly must be present in the serialized element tree");
        assertTrue(field.isBoolean());
        assertEquals(true, field.booleanValue());
    }

    @Test
    void rejectsStringBooleanCoercionAtTypedAndSemanticBoundaries() {
        assertThrows(IllegalArgumentException.class, () -> ProfiledAspectState.builder(KnownProfileAspect.SAFETY)
                .field("professionalUseOnly", "true"));

        SingleValuedDataElement stringBoolean = new SingleValuedDataElement.Builder()
                .elementId("professionalUseOnly").value("true").valueDataType("xsd:string").build();
        DataElementCollection safety = new DataElementCollection.Builder().elementId("safety")
                .dataElements(List.of(stringBoolean)).build();
        var header = TestDataFactory.validHeader().toBuilder()
                .contentSpecificationIds(Dpp4FunSemanticProfile.loadDefault().contentSpecificationIds()).build();
        Dpp4Fun value = Dpp4Fun.from(new DigitalProductPassport.Builder().dppHeader(header)
                .dataElements(List.of(safety)).build());

        assertThrows(IllegalArgumentException.class, () -> value.getSafety().getProfessionalUseOnly());
        assertTrue(new Dpp4FunValidator().validate(value).issues().stream()
                .anyMatch(issue -> issue.rule().equals("DPP4FUN_BINDING_DATATYPE_MISMATCH")));
    }

    private JsonNode findValue(JsonNode node, String elementId) {
        if (node.isObject() && elementId.equals(node.path("elementId").asText())) return node.path("value");
        for (JsonNode child : node) {
            JsonNode found = findValue(child, elementId);
            if (found != null) return found;
        }
        return null;
    }
}
