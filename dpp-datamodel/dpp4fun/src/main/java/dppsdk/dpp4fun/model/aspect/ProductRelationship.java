package dppsdk.dpp4fun.model.aspect;
import dppsdk.dpp4fun.model.internal.*;
import dppsdk.dpp4fun.model.*;
import dppsdk.dpp4fun.model.composite.*;
import dppsdk.dpp4fun.model.characteristics.*;
import dppsdk.dpp4fun.model.unit.*;

import dppsdk.core.model.DataElement;
import java.net.URI;
import java.util.List;

/** Immutable typed view of the D4.1 ProductRelationship aspect. */
public final class ProductRelationship extends CatalogAspectModel {
    ProductRelationship(ProfiledAspectState aspect) { super(aspect); if (!"productRelationship".equals(aspect.elementId())) throw new IllegalArgumentException("Expected productRelationship aspect"); }
    public ProductRelationship(DataElement element) { this(new ProfiledAspectState("productRelationship", element)); }
    public static Builder builder() { return new Builder(ProfiledAspectState.builder(KnownProfileAspect.PRODUCT_RELATIONSHIP).build()); }
    public Builder toBuilder() { return new Builder(profileState()); }
    public List<Accessory> getAccessories() { return entries("accessorySet", Accessory::new); }
    public List<CollectionMember> getCollectionMembers() { return entries("collectionMemberSet", CollectionMember::new); }
    @Override public boolean equals(Object other) { return this == other || other instanceof ProductRelationship that && profileState().asDataElement().equals(that.profileState().asDataElement()); }
    @Override public int hashCode() { return profileState().asDataElement().hashCode(); }
    public static final class Builder {
        private final Editor editor;
        private Builder(ProfiledAspectState aspect) { editor = new Editor(aspect); }
        public Builder accessories(List<Accessory> value) { editor.entries("accessorySet", value); return this; }
        public Builder collectionMembers(List<CollectionMember> value) { editor.entries("collectionMemberSet", value); return this; }
        public ProductRelationship build() { return new ProductRelationship(editor.aspect()); }
    }
}
