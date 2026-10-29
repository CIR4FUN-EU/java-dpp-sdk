package dppsdk.dpp4fun.model.composite;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import java.net.URI;
import java.util.List;
/** Immutable typed view of a D4.1 HealthIndicator record. */
public final class HealthIndicator extends TypedCompositeEntry {
    public static final List<String> ORDER = List.of("name", "description", "value", "unit", "calculationMethod", "hazardType");
    public HealthIndicator(DataElement value) { super(value); }
    DataElementCollection dataElement() { return element(); }
    public static Builder builder() { return new Builder(new Editor(ProfileElementSupport.empty("item"), ORDER)); }
    public Builder toBuilder() { return new Builder(new Editor(element(), ORDER)); }
    public String getName() { return text("name"); }
    public String getDescription() { return text("description"); }
    public String getValue() { return text("value"); }
    public String getUnit() { return text("unit"); }
    public URI getCalculationMethod() { return uri("calculationMethod"); }
    public String getHazardType() { return text("hazardType"); }
    public static final class Builder {
        private final Editor editor;
        private Builder(Editor editor) { this.editor = editor; }
        public Builder name(String value) { editor.set("name", value); return this; }
        public Builder description(String value) { editor.set("description", value); return this; }
        public Builder value(String value) { editor.set("value", value); return this; }
        public Builder unit(String value) { editor.set("unit", value); return this; }
        public Builder calculationMethod(URI value) { editor.set("calculationMethod", value); return this; }
        public Builder hazardType(String value) { editor.set("hazardType", value); return this; }
        public HealthIndicator build() { return new HealthIndicator(editor.element()); }
    }
}
