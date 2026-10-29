package dppsdk.core.validation.format;

import dppsdk.core.model.DataElement;
import dppsdk.core.model.DataElementCollection;
import dppsdk.core.model.DigitalProductPassport;
import dppsdk.core.model.MultiLanguageDataElement;
import dppsdk.core.model.MultiLanguageValue;
import dppsdk.core.model.MultiValuedDataElement;
import dppsdk.core.model.RelatedResource;
import dppsdk.core.model.SingleValuedDataElement;
import dppsdk.core.validation.ValidationIssue;
import dppsdk.core.validation.ValidationResult;
import dppsdk.core.validation.format.ValueDataTypeValidator.Classification;

import java.util.ArrayList;
import java.util.List;

/** Validates local data formats declared by the generic DPP model. */
public final class DataFormatValidator {

    /** Validate present format-bearing values and collect every format issue. */
    public ValidationResult validate(DigitalProductPassport passport) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (passport != null) {
            validateElements(passport.getDataElements(), "elements", issues);
        }
        return ValidationResult.of(issues);
    }

    private static void validateElements(List<DataElement> elements, String path,
                                         List<ValidationIssue> issues) {
        for (int i = 0; i < elements.size(); i++) {
            validateElement(elements.get(i), path + "[" + i + "]", issues);
        }
    }

    private static void validateElement(DataElement element, String path, List<ValidationIssue> issues) {
        if (element instanceof RelatedResource resource) {
            if (resource.getUrl() != null && !ResourceUrlValidator.isValid(resource.getUrl())) {
                add(issues, path + ".url", "RESOURCE_URL_INVALID",
                        "url must be an absolute RFC 3986 URI");
            }
            if (resource.getContentType() != null
                    && !MediaTypeValidator.isValid(resource.getContentType())) {
                add(issues, path + ".contentType", "MEDIA_TYPE_INVALID",
                        "contentType must be a syntactically valid media type");
            }
            if (resource.getLanguage() != null) {
                validateLanguage(resource.getLanguage(), path + ".language", issues,
                        "EN18223_RELATED_RESOURCE_LANGUAGE_TAG");
            }
        } else if (element instanceof SingleValuedDataElement single) {
            validateSingleValue(single, path, single.getValueDataType(), false, issues);
        } else if (element instanceof DataElementCollection collection) {
            validateElements(collection.getDataElements(), path + ".elements", issues);
        } else if (element instanceof MultiValuedDataElement multi) {
            validateMultiValued(multi, path, issues);
        } else if (element instanceof MultiLanguageDataElement multilingual) {
            List<MultiLanguageValue> values = multilingual.getValues();
            for (int i = 0; i < values.size(); i++) {
                MultiLanguageValue value = values.get(i);
                if (value.getLanguage() != null) {
                    validateLanguage(value.getLanguage(), path + ".values[" + i + "].language", issues,
                            "EN18223_MULTILANGUAGE_LANGUAGE_TAG");
                }
            }
        }
    }

    private static void validateMultiValued(MultiValuedDataElement multi, String path,
                                             List<ValidationIssue> issues) {
        String containerType = multi.getValueDataType();
        if (containerType != null) validateDataType(containerType, path + ".valueDataType", issues);

        List<DataElement> children = multi.getDataElements();
        String inferredChildType = null;
        for (int i = 0; i < children.size(); i++) {
            DataElement child = children.get(i);
            String childPath = path + ".elements[" + i + "]";
            if (child instanceof SingleValuedDataElement single) {
                String childType = single.getValueDataType();
                if (containerType != null && childType != null && !containerType.equals(childType)) {
                    add(issues, childPath + ".valueDataType", "MULTI_VALUE_DATA_TYPE_CONFLICT",
                            "child valueDataType conflicts with its MultiValuedDataElement declaration");
                }
                if (containerType == null && childType != null) {
                    if (inferredChildType == null) {
                        inferredChildType = childType;
                    } else if (!inferredChildType.equals(childType)) {
                        add(issues, childPath + ".valueDataType", "MULTI_VALUE_DATA_TYPE_MISMATCH",
                                "all declared child valueDataType values in a MultiValuedDataElement must match");
                    }
                }
                if (childType != null) {
                    validateSingleValue(single, childPath, childType, false, issues);
                } else if (containerType != null) {
                    validateSingleValue(single, childPath, containerType, true, issues);
                }
            } else {
                validateElement(child, childPath, issues);
            }
        }
    }

    private static void validateSingleValue(SingleValuedDataElement single, String path,
                                            String effectiveType, boolean inheritedType,
                                            List<ValidationIssue> issues) {
        if (effectiveType == null) return;
        Classification classification = ValueDataTypeValidator.classify(effectiveType);
        if (!inheritedType) validateDataType(effectiveType, path + ".valueDataType", issues);
        if (classification != Classification.ALLOWED || single.getValue() == null) return;

        ValueCompatibilityValidator.Failure failure = ValueCompatibilityValidator.validate(
                ValueDataTypeValidator.builtInName(effectiveType), single.getValue());
        if (failure != null) {
            add(issues, path + ".value", failure.rule(), failure.message());
        }
    }

    private static Classification validateDataType(String valueDataType, String path,
                                                   List<ValidationIssue> issues) {
        Classification classification = ValueDataTypeValidator.classify(valueDataType);
        if (classification == Classification.PROHIBITED) {
            add(issues, path, "VALUE_DATA_TYPE_PROHIBITED",
                    "valueDataType uses an XSD built-in prohibited by EN 18223");
        } else if (classification == Classification.UNKNOWN) {
            add(issues, path, "VALUE_DATA_TYPE_UNKNOWN",
                    "valueDataType must use a supported xsd:<built-in> spelling");
        }
        return classification;
    }

    private static void validateLanguage(String language, String path, List<ValidationIssue> issues, String rule) {
        if (!LocalizedLanguageValidator.hasRequiredShape(language)) {
            add(issues, path, rule,
                    "language must contain a two-letter ISO 639 code and a two-letter ISO 3166-1 country code");
            return;
        }
        String languageCode = language.substring(0, 2);
        String countryCode = language.substring(3, 5);
        if (!LocalizedLanguageValidator.isLanguageCode(languageCode)) {
            add(issues, path, rule, "language code is not in ISO 639 Set 1");
        }
        if (!LocalizedLanguageValidator.isCountryCode(countryCode)) {
            add(issues, path, rule, "country code is not in ISO 3166-1 alpha-2");
        }
    }

    private static void add(List<ValidationIssue> issues, String path, String rule, String message) {
        issues.add(new ValidationIssue(path, rule, message));
    }
}
