package dppsdk.dpp4fun.model.composite;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import java.net.URI;
import java.util.List;
/** Immutable typed view of a D4.1 CarbonFootprintCalculation record. */
public final class CarbonFootprintCalculation extends TypedCompositeEntry {
    public static final List<String> ORDER = List.of("reference", "co2eq", "phase", "quantityOfMeasurementForCalculation", "impactUnitForCalculation");
    public CarbonFootprintCalculation(DataElement value) { super(value); }
    DataElementCollection dataElement() { return element(); }
    public static Builder builder() { return new Builder(new Editor(ProfileElementSupport.empty("item"), ORDER)); }
    public Builder toBuilder() { return new Builder(new Editor(element(), ORDER)); }
    public URI getReference() { return uri("reference"); }
    public Double getCo2eq() { return decimal("co2eq"); }
    public String getPhase() { return text("phase"); }
    public String getQuantityOfMeasurementForCalculation() { return text("quantityOfMeasurementForCalculation"); }
    public String getImpactUnitForCalculation() { return text("impactUnitForCalculation"); }
    public static final class Builder {
        private final Editor editor;
        private Builder(Editor editor) { this.editor = editor; }
        public Builder reference(URI value) { editor.set("reference", value); return this; }
        public Builder co2eq(Double value) { editor.set("co2eq", value); return this; }
        public Builder phase(String value) { editor.set("phase", value); return this; }
        public Builder quantityOfMeasurementForCalculation(String value) { editor.set("quantityOfMeasurementForCalculation", value); return this; }
        public Builder impactUnitForCalculation(String value) { editor.set("impactUnitForCalculation", value); return this; }
        public CarbonFootprintCalculation build() { return new CarbonFootprintCalculation(editor.element()); }
    }
}
