package dppsdk.core.model;

import java.util.Objects;

/** A resource associated with the passport content. */
public final class RelatedResource extends DataElement {
    private final String contentType;
    private final String url;
    private final String language;
    private final String resourceTitle;

    private RelatedResource(Builder builder) {
        super(builder.elementId, builder.dictionaryReference);
        this.contentType = builder.contentType;
        this.url = builder.url;
        this.language = builder.language;
        this.resourceTitle = builder.resourceTitle;
    }

    public String getContentType() { return contentType; }
    public String getUrl() { return url; }
    public String getLanguage() { return language; }
    public String getResourceTitle() { return resourceTitle; }

    public Builder toBuilder() {
        return new Builder().elementId(getElementId())
                .dictionaryReference(getDictionaryReference())
                .contentType(contentType).url(url)
                .language(language).resourceTitle(resourceTitle);
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) return false;
        RelatedResource that = (RelatedResource) other;
        return Objects.equals(contentType, that.contentType)
                && Objects.equals(url, that.url)
                && Objects.equals(language, that.language)
                && Objects.equals(resourceTitle, that.resourceTitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contentType, url, language, resourceTitle);
    }

    @Override
    public String toString() {
        return "RelatedResource{elementId='" + getElementId()
                + "', contentType='" + contentType + "', url='" + url + "'}";
    }

    public static final class Builder {
        private String elementId;
        private String dictionaryReference;
        private String contentType;
        private String url;
        private String language;
        private String resourceTitle;

        public Builder elementId(String value) { this.elementId = value; return this; }
        public Builder dictionaryReference(String value) { this.dictionaryReference = value; return this; }
        public Builder contentType(String value) { this.contentType = value; return this; }
        public Builder url(String value) { this.url = value; return this; }
        public Builder language(String value) { this.language = value; return this; }
        public Builder resourceTitle(String value) { this.resourceTitle = value; return this; }
        public RelatedResource build() { return new RelatedResource(this); }
    }
}
