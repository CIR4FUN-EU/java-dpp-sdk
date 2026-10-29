package dppsdk.core.model;

import java.util.Objects;

/** A text value and its language tag. */
public final class MultiLanguageValue {
    private final String value;
    private final String language;

    private MultiLanguageValue(Builder builder) {
        this.value = builder.value;
        this.language = builder.language;
    }

    public String getValue() { return value; }
    public String getLanguage() { return language; }

    public Builder toBuilder() { return new Builder().value(value).language(language); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        MultiLanguageValue that = (MultiLanguageValue) other;
        return Objects.equals(value, that.value)
                && Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() { return Objects.hash(value, language); }

    @Override
    public String toString() {
        return "MultiLanguageValue{value='" + value + "', language='" + language + "'}";
    }

    public static final class Builder {
        private String value;
        private String language;

        public Builder value(String value) { this.value = value; return this; }
        public Builder language(String value) { this.language = value; return this; }
        public MultiLanguageValue build() { return new MultiLanguageValue(this); }
    }
}
