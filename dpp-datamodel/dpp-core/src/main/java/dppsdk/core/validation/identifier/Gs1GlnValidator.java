package dppsdk.core.validation.identifier;

import dppsdk.core.validation.ValidationIssue;

import java.net.URI;
import java.util.List;

/** Validates a GLN key and its party or physical-location role. */
final class Gs1GlnValidator {
    enum Role { PARTY, PHYSICAL_LOCATION }

    void validate(String identifier, String path, Role role, List<ValidationIssue> issues) {
        if (identifier == null || identifier.isEmpty()) {
            invalid(issues, path, "A GS1 GLN is required");
            return;
        }

        String key;
        String roleAi = null;
        if (isWebUrl(identifier)) {
            URI uri = IdentifierUri.parseGs1WebUrl(identifier);
            if (uri == null || !IdentifierUri.hasValidGs1QuerySyntax(uri.getRawQuery())) {
                invalid(issues, path, "A GS1 GLN Digital Link must be a valid HTTP or HTTPS URI");
                return;
            }
            if (IdentifierUri.hasGs1NumericQueryParameter(uri.getRawQuery())) {
                issues.add(new ValidationIssue(path, "GS1_DIGITAL_LINK_QUERY_UNSUPPORTED",
                        "GS1 numeric query attributes require AI/value validation not implemented by this local profile"));
            }
            String[] pathSegments = uri.getRawPath().split("/", -1);
            int keyIndex = findRoleKeyIndex(pathSegments);
            if (keyIndex < 0) {
                invalid(issues, path, "A GS1 GLN Digital Link must use AI 414 or AI 417");
                return;
            }
            roleAi = pathSegments[keyIndex];
            int suffixLength = pathSegments.length - keyIndex;
            if (suffixLength == 4
                    && (!"414".equals(roleAi) || !"254".equals(pathSegments[keyIndex + 2])
                    || !isValidExtension(pathSegments[keyIndex + 3]))) {
                invalid(issues, path, "Only physical-location GLN can carry a GS1 GLN extension");
                return;
            }
            if (suffixLength != 2 && suffixLength != 4) {
                invalid(issues, path, "A GS1 GLN Digital Link must use AI 414 or AI 417");
                return;
            }
            key = pathSegments[keyIndex + 1];
        } else if (identifier.length() == 16 && isRoleAi(identifier.substring(0, 3))) {
            roleAi = identifier.substring(0, 3);
            key = identifier.substring(3);
        } else {
            invalid(issues, path, "A GS1 GLN must include AI 414 or AI 417");
            return;
        }

        if (!Gs1KeyValidation.isDigits(key, 13) || !Gs1KeyValidation.hasCheckDigit(key)) {
            invalid(issues, path, "A GLN must be 13 digits and have a valid GS1 check digit");
            return;
        }

        String expectedAi = role == Role.PARTY ? "417" : "414";
        if (roleAi != null && !expectedAi.equals(roleAi)) {
            issues.add(new ValidationIssue(path, "GS1_GLN_ROLE_MISMATCH",
                    role == Role.PARTY
                            ? "Economic operator GLN must identify a party (AI 417)"
                            : "Facility GLN must identify a physical location (AI 414)"));
        }
    }

    private static int findRoleKeyIndex(String[] segments) {
        for (int i = segments.length - 2; i >= 1; i--) {
            int remaining = segments.length - i;
            if (isRoleAi(segments[i]) && (remaining == 2 || remaining == 4)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isWebUrl(String value) {
        return value.regionMatches(true, 0, "https://", 0, 8)
                || value.regionMatches(true, 0, "http://", 0, 7);
    }

    private static boolean isRoleAi(String ai) {
        return "414".equals(ai) || "417".equals(ai);
    }

    private static boolean isValidExtension(String raw) {
        String value = IdentifierUri.decodeComponent(raw);
        return Gs1XcharValidation.isValidValue(raw, value, 20);
    }

    private static void invalid(List<ValidationIssue> issues, String path, String message) {
        issues.add(new ValidationIssue(path, "GS1_GLN_INVALID", message));
    }
}
