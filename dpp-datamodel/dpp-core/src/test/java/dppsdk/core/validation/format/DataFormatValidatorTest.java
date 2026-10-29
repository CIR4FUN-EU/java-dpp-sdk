package dppsdk.core.validation.format;

import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.DataElement;
import dppsdk.core.model.DppHeader;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.MultiLanguageValue;
import dppsdk.core.model.RelatedResource;
import dppsdk.core.validation.ValidationIssue;
import dppsdk.core.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataFormatValidatorTest {

    private final DataFormatValidator validator = new DataFormatValidator();

    @Test
    void validatesAbsoluteRfc3986ResourceUrlsWithoutRestrictingSchemes() {
        ValidationResult result = validate(
                resource("urn:example:manual%20one", "application/pdf"),
                resource("custom:/manual/%E2%82%AC", "application/json"),
                resource("http://user:pass@example.invalid:/a?b#c", "application/json"),
                resource("http://[v1.a]/manual", "application/json"),
                resource("a:", "application/json"));

        assertTrue(result.isValid(), result.issues().toString());
    }

    @Test
    void rejectsMalformedRelativeAndNonRfc3986ResourceUrls() {
        ValidationResult result = validate(
                resource("../manual.pdf", "application/pdf"),
                resource("https://example.invalid/manual%2", "application/pdf"),
                resource("https://example.invalid/a b", "application/pdf"),
                resource("https://example.invalid/é", "application/pdf"));

        assertEquals(Set.of("elements[0].url", "elements[1].url", "elements[2].url", "elements[3].url"),
                paths(result, "EN18223_RELATED_RESOURCE_URL_RFC3986"));
    }

    @Test
    void validatesOpenEndedMediaTypeSyntaxAndParameters() {
        ValidationResult result = validate(
                resource("urn:manual:1", "application/pdf"),
                resource("urn:manual:2", "application/json"),
                resource("urn:manual:3", "application/ld+json"),
                resource("urn:manual:4", "Text/Plain;Charset=\"utf-8\""),
                resource("urn:manual:5", "application/x-future_media.v2+json; version=\"2;stable\""),
                resource("urn:manual:6", "application/json \t; charset=utf-8"));

        assertTrue(result.isValid(), result.issues().toString());
    }

    @Test
    void rejectsMalformedMediaTypeTokensParametersAndOuterWhitespace() {
        ValidationResult result = validate(
                resource("urn:manual:1", "application"),
                resource("urn:manual:2", "/json"),
                resource("urn:manual:3", "application/"),
                resource("urn:manual:4", "app lication/json"),
                resource("urn:manual:5", "application/json; charset"),
                resource("urn:manual:6", "application/json; charset=\"unterminated"),
                resource("urn:manual:7", "application/json; charset=utf-8; CHARSET=ascii"),
                resource("urn:manual:8", " application/json"),
                resource("urn:manual:9", "application/json "),
                resource("urn:manual:10", "application/json;charset=utf-8 "));

        assertEquals(Set.of("elements[0].contentType", "elements[1].contentType",
                        "elements[2].contentType", "elements[3].contentType",
                        "elements[4].contentType", "elements[5].contentType",
                        "elements[6].contentType", "elements[7].contentType",
                        "elements[8].contentType", "elements[9].contentType"),
                paths(result, "EN18223_RELATED_RESOURCE_CONTENT_TYPE_SYNTAX"));
    }

    @Test
    void validatesPinnedTwoLetterLanguageAndCountryCodesCaseInsensitively() {
        ValidationResult result = validate(
                resource("urn:manual:1", "application/pdf", "en-GB"),
                resource("urn:manual:2", "application/pdf", "de-DE"),
                resource("urn:manual:3", "application/pdf", "de-de"),
                resource("urn:manual:4", "application/pdf", "ar-EG"),
                resource("urn:manual:5", "application/pdf", "id-ID"),
                resource("urn:manual:6", "application/pdf", "ru-RU"),
                multilingual("translations", new MultiLanguageValue.Builder()
                        .value("Hallo").language("DE-de").build()));

        assertTrue(result.isValid(), result.issues().toString());
    }

    @Test
    void rejectsLanguageOnlyAndOtherNonEnLanguageTagForms() {
        ValidationResult result = validate(
                resource("urn:manual:1", "application/pdf", "en"),
                resource("urn:manual:2", "application/pdf", "eng-DE"),
                resource("urn:manual:3", "application/pdf", "zh-Hans-CN"),
                resource("urn:manual:4", "application/pdf", "en-US-posix"));

        assertEquals(Set.of("elements[0].language", "elements[1].language",
                        "elements[2].language", "elements[3].language"),
                paths(result, "EN18223_RELATED_RESOURCE_LANGUAGE_TAG"));
    }

    @Test
    void rejectsUnknownLanguageAndCountryCodesFromPinnedSets() {
        ValidationResult result = validate(
                resource("urn:manual:1", "application/pdf", "zz-US"),
                resource("urn:manual:2", "application/pdf", "en-ZZ"),
                multilingual("translations", new MultiLanguageValue.Builder()
                        .value("Unknown").language("zz-ZZ").build()));

        assertEquals(Set.of("elements[0].language", "elements[1].language"),
                paths(result, "EN18223_RELATED_RESOURCE_LANGUAGE_TAG"));
        assertEquals(Set.of("elements[2].values[0].language"),
                paths(result, "EN18223_MULTILANGUAGE_LANGUAGE_TAG"));
    }

    @Test
    void rejectsWithdrawnLanguageAliasesAndReservedOrUserAssignedCountryCodes() {
        ValidationResult result = validate(
                resource("urn:manual:1", "application/pdf", "in-ID"),
                resource("urn:manual:2", "application/pdf", "iw-IL"),
                resource("urn:manual:3", "application/pdf", "ji-IL"),
                resource("urn:manual:4", "application/pdf", "mo-RO"),
                resource("urn:manual:5", "application/pdf", "en-EU"),
                resource("urn:manual:6", "application/pdf", "en-UK"),
                resource("urn:manual:7", "application/pdf", "en-XK"));

        assertEquals(Set.of("elements[0].language", "elements[1].language", "elements[2].language",
                        "elements[3].language", "elements[4].language", "elements[5].language",
                        "elements[6].language"), paths(result, "EN18223_RELATED_RESOURCE_LANGUAGE_TAG"));
    }

    @Test
    void clauseFourRulesOverrideAnnexLanguageAndTrailingMediaTypeWhitespaceExamples() {
        ValidationResult result = validate(
                resource("urn:manual:1", "application/pdf ", "en"),
                resource("urn:manual:2", "application/pdf", null),
                multilingual("translations", new MultiLanguageValue.Builder()
                        .value("No language").language(null).build()));

        assertEquals(Set.of("elements[0].language"),
                paths(result, "EN18223_RELATED_RESOURCE_LANGUAGE_TAG"));
        assertEquals(Set.of("elements[0].contentType"), paths(result, "EN18223_RELATED_RESOURCE_CONTENT_TYPE_SYNTAX"));
        assertEquals(Set.of("elements[0].language", "elements[0].contentType"),
                result.issues().stream().map(ValidationIssue::path).collect(Collectors.toSet()));
    }

    private static ValidationResult validate(DataElement... resources) {
        return new DataFormatValidator().validate(new DigitalProductPassport.Builder()
                .dppHeader(new DppHeader.Builder().build())
                .dataElements(List.of(resources))
                .build());
    }

    private static RelatedResource resource(String url, String contentType) {
        return resource(url, contentType, null);
    }

    private static RelatedResource resource(String url, String contentType, String language) {
        return new RelatedResource.Builder().elementId("resource")
                .url(url).contentType(contentType).language(language).build();
    }

    private static MultiLanguageDataElement multilingual(String id, MultiLanguageValue... values) {
        return new MultiLanguageDataElement.Builder().elementId(id)
                .values(List.of(values)).build();
    }

    private static Set<String> paths(ValidationResult result, String rule) {
        return result.issues().stream().filter(issue -> issue.rule().equals(rule))
                .map(ValidationIssue::path).collect(Collectors.toSet());
    }
}
