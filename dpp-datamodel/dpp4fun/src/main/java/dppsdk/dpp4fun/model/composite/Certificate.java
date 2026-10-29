package dppsdk.dpp4fun.model.composite;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import java.net.URI;
import java.util.List;
/** Immutable typed view of a D4.1 Certificate record. */
public final class Certificate extends TypedCompositeEntry {
    public static final List<String> ORDER = List.of("name", "issueDate", "description", "validityPolicy", "reference");
    public Certificate(DataElement value) { super(value); }
    DataElementCollection dataElement() { return element(); }
    public static Builder builder() { return new Builder(new Editor(ProfileElementSupport.empty("item"), ORDER)); }
    public Builder toBuilder() { return new Builder(new Editor(element(), ORDER)); }
    public String getName() { return text("name"); }
    public String getIssueDate() { return text("issueDate"); }
    public String getDescription() { return text("description"); }
    public String getValidityPolicy() { return text("validityPolicy"); }
    public URI getReference() { return uri("reference"); }
    public static final class Builder {
        private final Editor editor;
        private Builder(Editor editor) { this.editor = editor; }
        public Builder name(String value) { editor.set("name", value); return this; }
        public Builder issueDate(String value) { editor.set("issueDate", value); return this; }
        public Builder description(String value) { editor.set("description", value); return this; }
        public Builder validityPolicy(String value) { editor.set("validityPolicy", value); return this; }
        public Builder reference(URI value) { editor.set("reference", value); return this; }
        public Certificate build() { return new Certificate(editor.element()); }
    }
}
