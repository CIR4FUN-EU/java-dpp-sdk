package dppsdk.core.validation.identifier;

import dppsdk.core.model.Granularity;
import dppsdk.core.validation.ValidationIssue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/** Validates Identification Link URLs and the locally verifiable Part 2 structured subset. */
final class IdentificationLinkValidator {

    void validate(String identifier, Granularity granularity, List<ValidationIssue> issues) {
        URI uri = parseIdentificationLink(identifier);
        if (uri == null) {
            add(issues, "header.uniqueProductIdentifier", "IDENTIFICATION_LINK_URL_INVALID",
                    "Identification Link must use valid IEC 61406-1 URL syntax and permitted characters");
            return;
        }

        String query = uri.getRawQuery();
        if (query == null || query.isEmpty()) {
            compareGranularity(Granularity.ITEM, granularity, issues);
            return;
        }

        String[] parameters = query.split("&", -1);
        boolean hasSidi = false;
        for (String parameter : parameters) {
            int equals = parameter.indexOf('=');
            String name = equals < 0 ? parameter : parameter.substring(0, equals);
            hasSidi |= name.startsWith(".");
        }
        boolean hasProductCode = false;
        boolean hasBatch = false;
        boolean hasItem = false;
        boolean hasUnsupportedSidi = false;
        for (String parameter : parameters) {
            int equals = parameter.indexOf('=');
            String name = equals < 0 ? parameter : parameter.substring(0, equals);
            String value = equals < 0 ? null : parameter.substring(equals + 1);
            if (!name.startsWith(".")) {
                if (hasSidi && (equals < 0 || name.isEmpty())) {
                    add(issues, "header.uniqueProductIdentifier",
                            "IDENTIFICATION_LINK_STRUCTURED_SYNTAX_INVALID",
                            "Structured query parameters must be name/value pairs");
                    return;
                }
                continue; // Non-SIDI parameters are allowed and may be ignored by Part 2.
            }
            if (equals < 0 || !isSidiName(name)) {
                add(issues, "header.uniqueProductIdentifier",
                        "IDENTIFICATION_LINK_STRUCTURED_SYNTAX_INVALID",
                        "SIDI parameters require a dot-prefixed Data Identifier and a name/value pair");
                return;
            }
            if (value.isEmpty()) {
                add(issues, "header.uniqueProductIdentifier",
                        "IDENTIFICATION_LINK_REQUIRED_IDENTIFIER_MISSING",
                        "A structured Data Identifier requires a non-empty value");
                return;
            }
            if (IdentifierUri.decodeComponent(value) == null) {
                add(issues, "header.uniqueProductIdentifier",
                        "IDENTIFICATION_LINK_STRUCTURED_SYNTAX_INVALID",
                        "Structured Data Identifier values must use valid URI encoding");
                return;
            }
            switch (name) {
                case ".P", ".1P" -> hasProductCode = true;
                case ".T", ".1T" -> hasBatch = true;
                case ".S" -> hasItem = true;
                default -> hasUnsupportedSidi = true;
            }
        }

        if (hasUnsupportedSidi) {
            issues.add(new ValidationIssue("header.uniqueProductIdentifier",
                    "IDENTIFICATION_LINK_DI_UNSUPPORTED",
                    "This locally validated structured subset recognizes product categories .P/.1P, batch categories .T/.1T, and serial category .S"));
            return;
        }

        if (!hasSidi) {
            compareGranularity(Granularity.ITEM, granularity, issues);
            return;
        }

        Granularity represented;
        if (hasItem) {
            // An individual-item identifier remains item-level when batch data is also present.
            represented = Granularity.ITEM;
        } else if (hasBatch) {
            if (!hasProductCode) {
                add(issues, "header.uniqueProductIdentifier",
                        "IDENTIFICATION_LINK_REQUIRED_IDENTIFIER_MISSING",
                        "A batch-level Identification Link requires a product-code category");
                return;
            }
            represented = Granularity.BATCH;
        } else if (hasProductCode) {
            represented = Granularity.MODEL;
        } else {
            add(issues, "header.uniqueProductIdentifier",
                    "IDENTIFICATION_LINK_REQUIRED_IDENTIFIER_MISSING",
                    "A structured product link requires product-code, batch, or item identification");
            return;
        }

        compareGranularity(represented, granularity, issues);
    }

    private static URI parseIdentificationLink(String value) {
        if (value == null || value.isEmpty() || value.length() > 255 || !hasOnlyPermittedCharacters(value)) {
            return null;
        }
        String absolute = value;
        boolean hasExplicitAuthorityScheme = value.matches("^[A-Za-z][A-Za-z0-9+.-]*://.*");
        boolean hasScheme = value.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*");
        if (!hasExplicitAuthorityScheme && hasNumericPortInAuthority(value)) {
            absolute = "https://" + value;
        } else if (!hasExplicitAuthorityScheme && !hasScheme) {
            absolute = "https://" + value;
        }
        try {
            URI uri = new URI(absolute);
            if (!uri.isAbsolute() || !"https".equalsIgnoreCase(uri.getScheme())
                    || uri.isOpaque() || uri.getRawAuthority() == null
                    || uri.getRawAuthority().isEmpty()) {
                return null;
            }
            String host = hostFromAuthority(uri.getRawAuthority());
            if (host == null) {
                return null;
            }
            return uri;
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private static String hostFromAuthority(String authority) {
        int userInfoSeparator = authority.lastIndexOf('@');
        String server = userInfoSeparator < 0 ? authority : authority.substring(userInfoSeparator + 1);
        if (server.isEmpty()) {
            return null;
        }
        if (server.startsWith("[")) {
            int closingBracket = server.indexOf(']');
            if (closingBracket <= 1 || !hasValidPortSuffix(server.substring(closingBracket + 1))) {
                return null;
            }
            return server.substring(1, closingBracket);
        }
        int firstColon = server.indexOf(':');
        if (firstColon >= 0) {
            if (firstColon != server.lastIndexOf(':')
                    || !hasValidPortSuffix(server.substring(firstColon))) {
                return null;
            }
            server = server.substring(0, firstColon);
        }
        return server.isEmpty() || hasPunycodeLabel(server) ? null : server;
    }

    private static boolean hasPunycodeLabel(String host) {
        for (String label : host.split("\\.", -1)) {
            if (label.regionMatches(true, 0, "xn--", 0, 4)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasValidPortSuffix(String suffix) {
        if (suffix.isEmpty()) {
            return true;
        }
        if (suffix.charAt(0) != ':') {
            return false;
        }
        for (int i = 1; i < suffix.length(); i++) {
            if (suffix.charAt(i) < '0' || suffix.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean hasNumericPortInAuthority(String value) {
        int authorityEnd = value.length();
        for (char delimiter : new char[]{'/', '?', '#'}) {
            int index = value.indexOf(delimiter);
            if (index >= 0 && index < authorityEnd) authorityEnd = index;
        }
        int colon = value.lastIndexOf(':', authorityEnd - 1);
        if (colon <= 0 || colon == authorityEnd - 1) return false;
        for (int i = colon + 1; i < authorityEnd; i++) {
            if (value.charAt(i) < '0' || value.charAt(i) > '9') return false;
        }
        return true;
    }

    private static boolean hasOnlyPermittedCharacters(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean alphaNumeric = c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
                    || c >= '0' && c <= '9';
            boolean unreserved = c == '-' || c == '.' || c == '_' || c == '~';
            boolean reserved = ":/?#[]@!$&'()*+,;=".indexOf(c) >= 0;
            if (!alphaNumeric && !unreserved && !reserved) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSidiName(String name) {
        if (name.length() < 2 || name.length() > 5) {
            return false;
        }
        for (int i = 1; i < name.length() - 1; i++) {
            if (name.charAt(i) < '0' || name.charAt(i) > '9') {
                return false;
            }
        }
        char category = name.charAt(name.length() - 1);
        return category >= 'A' && category <= 'Z';
    }

    private static void compareGranularity(Granularity represented, Granularity granularity,
                                           List<ValidationIssue> issues) {
        if (granularity == null) {
            add(issues, "header.granularity", "IDENTIFIER_GRANULARITY_REQUIRED",
                    "Granularity is required to compare with the Identification Link");
        } else if (represented != granularity) {
            add(issues, "header.granularity", "IDENTIFIER_GRANULARITY_MISMATCH",
                    "Granularity does not match the level represented by the Identification Link");
        }
    }

    private static void add(List<ValidationIssue> issues, String path, String rule, String message) {
        issues.add(new ValidationIssue(path, rule, message));
    }
}
