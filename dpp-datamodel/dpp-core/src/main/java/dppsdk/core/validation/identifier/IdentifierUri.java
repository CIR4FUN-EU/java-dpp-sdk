package dppsdk.core.validation.identifier;

import java.net.URI;
import java.net.URISyntaxException;

/** URI syntax checks shared by identifier profiles that select a GS1 or IEC URL representation. */
final class IdentifierUri {
    private IdentifierUri() { }

    static URI parseGs1WebUrl(String value) {
        if (value == null || !hasValidAsciiPercentSyntax(value)) {
            return null;
        }
        try {
            URI uri = new URI(value);
            if (!("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getRawUserInfo() != null
                    || uri.getRawFragment() != null
                    || uri.getRawPath() == null
                    || uri.getRawPath().isEmpty()) {
                return null;
            }
            return uri;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean hasValidAsciiPercentSyntax(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x21 || c > 0x7e) {
                return false;
            }
            if (c == '%') {
                if (i + 2 >= value.length()
                        || Character.digit(value.charAt(i + 1), 16) < 0
                        || Character.digit(value.charAt(i + 2), 16) < 0) {
                    return false;
                }
                i += 2;
            }
        }
        return true;
    }

    static String decodeComponent(String raw) {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream(raw.length());
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
            return java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes.toByteArray())).toString();
        } catch (java.nio.charset.CharacterCodingException e) {
            return null;
        }
    }

    static boolean hasValidGs1QuerySyntax(String query) {
        if (query == null) {
            return true;
        }
        if (query.isEmpty()) {
            return false;
        }
        for (String parameter : query.split("&", -1)) {
            int equals = parameter.indexOf('=');
            if (equals <= 0) {
                return false;
            }
            String key = decodeComponent(parameter.substring(0, equals));
            String value = decodeComponent(parameter.substring(equals + 1));
            if (key == null || value == null || key.isEmpty()) {
                return false;
            }
            boolean numeric = true;
            for (int i = 0; i < key.length(); i++) {
                numeric &= key.charAt(i) >= '0' && key.charAt(i) <= '9';
            }
            if (numeric && (key.length() < 2 || key.length() > 4 || value.isEmpty())) {
                return false;
            }
            if (key.indexOf('=') >= 0) {
                return false;
            }
        }
        return true;
    }

    static boolean hasGs1NumericQueryParameter(String query) {
        if (query == null) {
            return false;
        }
        for (String parameter : query.split("&", -1)) {
            int equals = parameter.indexOf('=');
            String key = decodeComponent(parameter.substring(0, equals));
            if (key != null && !key.isEmpty()) {
                boolean numeric = true;
                for (int i = 0; i < key.length(); i++) {
                    numeric &= key.charAt(i) >= '0' && key.charAt(i) <= '9';
                }
                if (numeric) {
                    return true;
                }
            }
        }
        return false;
    }
}
