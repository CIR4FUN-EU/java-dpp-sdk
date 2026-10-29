package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 Traceability aspect. */
public final class Traceability extends CatalogAspectModel {
    Traceability(ProfiledAspectState aspect) { super(aspect); if (!"traceability".equals(aspect.elementId())) throw new IllegalArgumentException("Expected traceability aspect"); }
    public Traceability(DataElement element) { this(new ProfiledAspectState("traceability", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.TRACEABILITY).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public String getCurrentPhase() { return text("currentPhase"); }
    public String getLocation() { return text("location"); }
    public List<DesignRecord> getDesignRecords() { return entries("designRecordSet", DesignRecord::new); }
    public List<ManufacturingRecord> getManufacturingRecords() { return entries("manufacturingRecordSet", ManufacturingRecord::new); }
    public List<TrackingRecord> getTrackingRecords() { return entries("trackingRecordSet", TrackingRecord::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof Traceability that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder currentPhase(String value) { editor.text("currentPhase", value); return this; }
        public Builder location(String value) { editor.text("location", value); return this; }
        public Builder designRecords(List<DesignRecord> value) { editor.entries("designRecordSet", value); return this; }
        public Builder manufacturingRecords(List<ManufacturingRecord> value) { editor.entries("manufacturingRecordSet", value); return this; }
        public Builder trackingRecords(List<TrackingRecord> value) { editor.entries("trackingRecordSet", value); return this; }
        public Traceability build() { return new Traceability(editor.aspect()); }
    }
}
