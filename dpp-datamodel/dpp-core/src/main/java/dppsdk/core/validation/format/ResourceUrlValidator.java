package dppsdk.core.validation.format;

/** RFC 3986 URI syntax checks shared by resource URLs and xsd:anyURI values. */
final class ResourceUrlValidator {

    private ResourceUrlValidator() { }

    static boolean isValid(String value) {
        if (value == null || value.isEmpty() || !isAsciiUriText(value)) return false;
        int colon = value.indexOf(':');
        if (colon <= 0 || !isScheme(value, 0, colon)) return false;

        int fragmentStart = value.indexOf('#', colon + 1);
        if (fragmentStart >= 0 && value.indexOf('#', fragmentStart + 1) >= 0) return false;
        int mainEnd = fragmentStart < 0 ? value.length() : fragmentStart;
        int queryStart = value.indexOf('?', colon + 1);
        if (queryStart >= mainEnd) queryStart = -1;

        int hierarchyEnd = queryStart < 0 ? mainEnd : queryStart;
        if (!isHierPart(value, colon + 1, hierarchyEnd)) return false;
        if (queryStart >= 0 && !isQueryOrFragment(value, queryStart + 1, mainEnd)) return false;
        return fragmentStart < 0
                || isQueryOrFragment(value, fragmentStart + 1, value.length());
    }

    private static boolean isHierPart(String value, int start, int end) {
        if (start == end) return true;
        if (start + 1 < end && value.charAt(start) == '/' && value.charAt(start + 1) == '/') {
            int authorityStart = start + 2;
            int pathStart = authorityStart;
            while (pathStart < end && value.charAt(pathStart) != '/') pathStart++;
            return isAuthority(value, authorityStart, pathStart)
                    && isPath(value, pathStart, end, true);
        }
        return isPath(value, start, end, false);
    }

    private static boolean isAuthority(String value, int start, int end) {
        int at = value.indexOf('@', start);
        if (at >= end) at = -1;
        if (at >= 0) {
            if (value.indexOf('@', at + 1) >= 0 && value.indexOf('@', at + 1) < end) return false;
            if (!isUserInfo(value, start, at)) return false;
            start = at + 1;
        }
        if (start == end) return true;

        int hostEnd;
        int portStart = -1;
        if (value.charAt(start) == '[') {
            int close = value.indexOf(']', start + 1);
            if (close < 0 || close >= end || !isIpLiteral(value, start + 1, close)) return false;
            hostEnd = close + 1;
            if (hostEnd < end) {
                if (value.charAt(hostEnd) != ':') return false;
                portStart = hostEnd + 1;
            }
        } else {
            int colon = value.indexOf(':', start);
            if (colon >= end) colon = -1;
            hostEnd = colon < 0 ? end : colon;
            if (!isRegName(value, start, hostEnd)) return false;
            if (colon >= 0) portStart = colon + 1;
        }
        if (portStart < 0) return true;
        for (int i = portStart; i < end; i++) {
            if (!isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    private static boolean isUserInfo(String value, int start, int end) {
        for (int i = start; i < end; i++) {
            char character = value.charAt(i);
            if (!isUnreserved(character) && !isSubDelimiter(character) && character != ':') {
                if (character != '%' || !isPercentTriplet(value, i, end)) return false;
                i += 2;
            }
        }
        return true;
    }

    private static boolean isRegName(String value, int start, int end) {
        for (int i = start; i < end; i++) {
            char character = value.charAt(i);
            if (!isUnreserved(character) && !isSubDelimiter(character)) {
                if (character != '%' || !isPercentTriplet(value, i, end)) return false;
                i += 2;
            }
        }
        return true;
    }

    private static boolean isIpLiteral(String value, int start, int end) {
        if (start >= end) return false;
        if (value.charAt(start) == 'v' || value.charAt(start) == 'V') {
            int dot = value.indexOf('.', start + 1);
            if (dot <= start + 1 || dot >= end) return false;
            for (int i = start + 1; i < dot; i++) {
                if (!isHex(value.charAt(i))) return false;
            }
            if (dot + 1 == end) return false;
            for (int i = dot + 1; i < end; i++) {
                char character = value.charAt(i);
                if (!isUnreserved(character) && !isSubDelimiter(character) && character != ':') {
                    return false;
                }
            }
            return true;
        }
        return isIpv6Address(value.substring(start, end));
    }

    private static boolean isIpv6Address(String address) {
        if (address.isEmpty()) return false;
        String normalized = address;
        int dot = address.indexOf('.');
        if (dot >= 0) {
            int lastColon = address.lastIndexOf(':');
            if (lastColon < 0 || !isIpv4Address(address.substring(lastColon + 1))) return false;
            String[] octets = address.substring(lastColon + 1).split("\\.", -1);
            int first = Integer.parseInt(octets[0]) * 256 + Integer.parseInt(octets[1]);
            int second = Integer.parseInt(octets[2]) * 256 + Integer.parseInt(octets[3]);
            normalized = address.substring(0, lastColon + 1)
                    + Integer.toHexString(first) + ':' + Integer.toHexString(second);
        }

        int compression = normalized.indexOf("::");
        if (compression >= 0 && normalized.indexOf("::", compression + 2) >= 0) return false;
        String left = compression < 0 ? normalized : normalized.substring(0, compression);
        String right = compression < 0 ? "" : normalized.substring(compression + 2);
        int leftGroups = countIpv6Groups(left);
        int rightGroups = countIpv6Groups(right);
        if (leftGroups < 0 || rightGroups < 0) return false;
        return compression < 0 ? leftGroups == 8 : leftGroups + rightGroups < 8;
    }

    private static int countIpv6Groups(String part) {
        if (part.isEmpty()) return 0;
        String[] groups = part.split(":", -1);
        for (String group : groups) {
            if (group.isEmpty() || group.length() > 4) return -1;
            for (int i = 0; i < group.length(); i++) {
                if (!isHex(group.charAt(i))) return -1;
            }
        }
        return groups.length;
    }

    private static boolean isIpv4Address(String address) {
        String[] octets = address.split("\\.", -1);
        if (octets.length != 4) return false;
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3
                    || octet.length() > 1 && octet.charAt(0) == '0') return false;
            int number = 0;
            for (int i = 0; i < octet.length(); i++) {
                if (!isDigit(octet.charAt(i))) return false;
                number = number * 10 + octet.charAt(i) - '0';
            }
            if (number > 255) return false;
        }
        return true;
    }

    private static boolean isPath(String value, int start, int end, boolean hasAuthority) {
        if (!hasAuthority && start + 1 < end && value.charAt(start) == '/'
                && value.charAt(start + 1) == '/') return false;
        for (int i = start; i < end; i++) {
            char character = value.charAt(i);
            if (character != '/' && !isPChar(value, i, end)) return false;
            if (character == '%') i += 2;
        }
        return true;
    }

    private static boolean isQueryOrFragment(String value, int start, int end) {
        for (int i = start; i < end; i++) {
            char character = value.charAt(i);
            if (character != '/' && character != '?' && !isPChar(value, i, end)) return false;
            if (character == '%') i += 2;
        }
        return true;
    }

    private static boolean isPChar(String value, int index, int end) {
        char character = value.charAt(index);
        if (isUnreserved(character) || isSubDelimiter(character)
                || character == ':' || character == '@') return true;
        return character == '%' && isPercentTriplet(value, index, end);
    }

    private static boolean isPercentTriplet(String value, int index, int end) {
        return index + 2 < end && isHex(value.charAt(index + 1)) && isHex(value.charAt(index + 2));
    }

    private static boolean isScheme(String value, int start, int end) {
        if (start == end || !isAlpha(value.charAt(start))) return false;
        for (int i = start + 1; i < end; i++) {
            char character = value.charAt(i);
            if (!isAlpha(character) && !isDigit(character)
                    && character != '+' && character != '-' && character != '.') return false;
        }
        return true;
    }

    private static boolean isAsciiUriText(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character <= 0x20 || character >= 0x7f) return false;
        }
        return true;
    }

    private static boolean isUnreserved(char character) {
        return isAlpha(character) || isDigit(character)
                || character == '-' || character == '.' || character == '_' || character == '~';
    }

    private static boolean isSubDelimiter(char character) {
        return "!$&'()*+,;=".indexOf(character) >= 0;
    }

    private static boolean isAlpha(char character) {
        return character >= 'a' && character <= 'z' || character >= 'A' && character <= 'Z';
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private static boolean isHex(char character) {
        return isDigit(character) || character >= 'a' && character <= 'f'
                || character >= 'A' && character <= 'F';
    }
}
