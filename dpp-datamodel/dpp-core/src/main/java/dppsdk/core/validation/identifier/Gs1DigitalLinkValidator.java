package dppsdk.core.validation.identifier;

import dppsdk.core.model.Granularity;
import dppsdk.core.validation.ValidationIssue;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validates the GS1 Digital Link GTIN-based product URI profile. */
final class Gs1DigitalLinkValidator {
    private static final Set<String> OTHER_PRODUCT_PRIMARY_KEYS = Set.of("8006", "8010", "8013");

    void validate(String identifier, Granularity granularity, List<ValidationIssue> issues) {
        URI uri = IdentifierUri.parseGs1WebUrl(identifier);
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())
                || !IdentifierUri.hasValidGs1QuerySyntax(uri.getRawQuery())) {
            add(issues, "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_URL_INVALID",
                    "GS1 Digital Link must be an absolute HTTPS URI with valid RFC 3986 encoding");
            return;
        }

        String[] segments = uri.getRawPath().split("/", -1);
        int keyIndex = findPrimaryKeyIndex(segments);
        if (keyIndex < 0) {
            int unsupportedKeyIndex = findOtherProductPrimaryKeyIndex(segments);
            if (unsupportedKeyIndex >= 0 && unsupportedKeyIndex + 1 < segments.length
                    && !segments[unsupportedKeyIndex + 1].isEmpty()) {
                add(issues, "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_KEY_UNSUPPORTED",
                        "This GS1 Digital Link product key is not locally validated");
                return;
            }
            add(issues, "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_KEY_INVALID",
                    "A GTIN primary key using AI 01 is required");
            return;
        }
        if (keyIndex + 1 >= segments.length || segments[keyIndex + 1].isEmpty()) {
            add(issues, "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_KEY_INVALID",
                    "A GTIN primary key using AI 01 is required");
            return;
        }
        String gtin = segments[keyIndex + 1];
        if (!Gs1KeyValidation.isDigits(gtin, 14) || !Gs1KeyValidation.hasCheckDigit(gtin)) {
            add(issues, "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_KEY_INVALID",
                    "The AI 01 value must be a valid 14-digit GTIN");
            return;
        }

        List<String> qualifiers = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int previousRank = -1;
        for (int i = keyIndex + 2; i < segments.length; i += 2) {
            if (i + 1 >= segments.length || segments[i].isEmpty() || segments[i + 1].isEmpty()) {
                add(issues, "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_QUALIFIER_INVALID",
                        "GS1 Digital Link qualifiers must be complete AI/value pairs");
                return;
            }
            String ai = segments[i];
            int rank = qualifierRank(ai);
            String rawValue = segments[i + 1];
            String value = decode(rawValue);
            if (rank < 0 || rank <= previousRank || !seen.add(ai)
                    || !isValidQualifierValue(ai, rawValue, value)
                    || "235".equals(ai) && !qualifiers.isEmpty()) {
                add(issues, "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_QUALIFIER_INVALID",
                        "The GTIN key qualifiers must use supported GS1 syntax and order");
                return;
            }
            previousRank = rank;
            qualifiers.add(ai);
        }

        Granularity represented = qualifiers.contains("21") || qualifiers.contains("235")
                ? Granularity.ITEM : qualifiers.contains("10") ? Granularity.BATCH : Granularity.MODEL;
        if (granularity == null) {
            add(issues, "header.granularity", "IDENTIFIER_GRANULARITY_REQUIRED",
                    "Granularity is required to compare with the GS1 Digital Link key");
        } else if (represented != granularity) {
            add(issues, "header.granularity", "IDENTIFIER_GRANULARITY_MISMATCH",
                    "Granularity does not match the product level represented by the GS1 key");
        }
        if (IdentifierUri.hasGs1NumericQueryParameter(uri.getRawQuery())) {
            add(issues, "header.uniqueProductIdentifier", "GS1_DIGITAL_LINK_QUERY_UNSUPPORTED",
                    "GS1 numeric query attributes require AI/value validation not implemented by this local profile");
        }
    }

    private static int findPrimaryKeyIndex(String[] segments) {
        for (int i = segments.length - 2; i >= 1; i--) {
            if ("01".equals(segments[i]) && isValidGtinSegment(segments[i + 1])) {
                return i;
            }
        }
        for (int i = 1; i < segments.length; i++) {
            if ("01".equals(segments[i])) {
                return i;
            }
        }
        return -1;
    }

    private static int findOtherProductPrimaryKeyIndex(String[] segments) {
        for (int i = segments.length - 2; i >= 1; i--) {
            if (OTHER_PRODUCT_PRIMARY_KEYS.contains(segments[i])) return i;
        }
        return -1;
    }

    private static boolean isValidGtinSegment(String raw) {
        return Gs1KeyValidation.isDigits(raw, 14) && Gs1KeyValidation.hasCheckDigit(raw);
    }

    private static int qualifierRank(String ai) {
        return switch (ai) {
            case "22" -> 0;
            case "10" -> 1;
            case "21" -> 2;
            case "235" -> 3;
            default -> -1;
        };
    }

    private static boolean isValidQualifierValue(String ai, String raw, String value) {
        int maximumLength = "235".equals(ai) ? 28 : 20;
        return Gs1XcharValidation.isValidValue(raw, value, maximumLength)
                && ("10".equals(ai) || "21".equals(ai) || "22".equals(ai)
                || "235".equals(ai));
    }

    private static String decode(String raw) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '%') {
                if (i + 2 >= raw.length()) {
                    return null;
                }
                int high = Character.digit(raw.charAt(i + 1), 16);
                int low = Character.digit(raw.charAt(i + 2), 16);
                if (high < 0 || low < 0) {
                    return null;
                }
                bytes.write(high * 16 + low);
                i += 2;
            } else if (c <= 0x7f) {
                bytes.write(c);
            } else {
                return null;
            }
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static void add(List<ValidationIssue> issues, String path, String rule, String message) {
        issues.add(new ValidationIssue(path, rule, message));
    }
}
