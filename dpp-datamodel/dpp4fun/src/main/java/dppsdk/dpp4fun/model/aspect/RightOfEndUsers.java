package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 RightOfEndUsers aspect. */
public final class RightOfEndUsers extends CatalogAspectModel {
    RightOfEndUsers(ProfiledAspectState aspect) { super(aspect); if (!"rightOfEndUsers".equals(aspect.elementId())) throw new IllegalArgumentException("Expected rightOfEndUsers aspect"); }
    public RightOfEndUsers(DataElement element) { this(new ProfiledAspectState("rightOfEndUsers", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.RIGHT_OF_END_USERS).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public List<PostSaleService> getPostSaleServices() { return entries("postSaleServiceSet", PostSaleService::new); }
    public Policies getPolicies() {
        var value = profileState().asDataElement().getDataElements().stream()
                .filter(child -> "policies".equals(child.getElementId())).findFirst().orElse(null);
        return value == null ? null : new Policies(value);
    }
    @Override public boolean equals(Object other) { return this == other || other instanceof RightOfEndUsers that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder postSaleServices(List<PostSaleService> value) { editor.entries("postSaleServiceSet", value); return this; }
        public Builder policies(Policies value) { if (value == null) editor.remove("policies"); else editor.entry("policies", value); return this; }
        public RightOfEndUsers build() { return new RightOfEndUsers(editor.aspect()); }
    }
}
