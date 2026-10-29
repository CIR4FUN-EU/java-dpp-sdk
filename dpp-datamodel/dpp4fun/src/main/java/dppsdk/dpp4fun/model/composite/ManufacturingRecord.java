package dppsdk.dpp4fun.model.composite;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import java.net.URI;
import java.util.List;
/** Immutable typed view of a D4.1 ManufacturingRecord record. */
public final class ManufacturingRecord extends TypedCompositeEntry {
    public static final List<String> ORDER = List.of("name", "description", "operator", "startTime", "endTime", "station", "status", "positionInProcessRouting");
    public ManufacturingRecord(DataElement value) { super(value); }
    DataElementCollection dataElement() { return element(); }
    public static Builder builder() { return new Builder(new Editor(ProfileElementSupport.empty("item"), ORDER)); }
    public Builder toBuilder() { return new Builder(new Editor(element(), ORDER)); }
    public String getName() { return text("name"); }
    public String getDescription() { return text("description"); }
    public String getOperator() { return text("operator"); }
    public String getStartTime() { return text("startTime"); }
    public String getEndTime() { return text("endTime"); }
    public String getStation() { return text("station"); }
    public String getStatus() { return text("status"); }
    public URI getPositionInProcessRouting() { return uri("positionInProcessRouting"); }
    public static final class Builder {
        private final Editor editor;
        private Builder(Editor editor) { this.editor = editor; }
        public Builder name(String value) { editor.set("name", value); return this; }
        public Builder description(String value) { editor.set("description", value); return this; }
        public Builder operator(String value) { editor.set("operator", value); return this; }
        public Builder startTime(String value) { editor.set("startTime", value); return this; }
        public Builder endTime(String value) { editor.set("endTime", value); return this; }
        public Builder station(String value) { editor.set("station", value); return this; }
        public Builder status(String value) { editor.set("status", value); return this; }
        public Builder positionInProcessRouting(URI value) { editor.set("positionInProcessRouting", value); return this; }
        public ManufacturingRecord build() { return new ManufacturingRecord(editor.element()); }
    }
}
