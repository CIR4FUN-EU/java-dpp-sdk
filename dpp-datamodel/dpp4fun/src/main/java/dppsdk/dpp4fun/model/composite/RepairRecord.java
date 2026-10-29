package dppsdk.dpp4fun.model.composite;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import java.net.URI;
import java.util.List;
/** Immutable typed view of a D4.1 RepairRecord record. */
public final class RepairRecord extends TypedCompositeEntry {
    public static final List<String> ORDER = List.of("name", "description", "operator", "time", "location", "type");
    public RepairRecord(DataElement value) { super(value); }
    DataElementCollection dataElement() { return element(); }
    public static Builder builder() { return new Builder(new Editor(ProfileElementSupport.empty("item"), ORDER)); }
    public Builder toBuilder() { return new Builder(new Editor(element(), ORDER)); }
    public String getName() { return text("name"); }
    public String getDescription() { return text("description"); }
    public String getOperator() { return text("operator"); }
    public String getTime() { return text("time"); }
    public String getLocation() { return text("location"); }
    public String getType() { return text("type"); }
    public static final class Builder {
        private final Editor editor;
        private Builder(Editor editor) { this.editor = editor; }
        public Builder name(String value) { editor.set("name", value); return this; }
        public Builder description(String value) { editor.set("description", value); return this; }
        public Builder operator(String value) { editor.set("operator", value); return this; }
        public Builder time(String value) { editor.set("time", value); return this; }
        public Builder location(String value) { editor.set("location", value); return this; }
        public Builder type(String value) { editor.set("type", value); return this; }
        public RepairRecord build() { return new RepairRecord(editor.element()); }
    }
}
