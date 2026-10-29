package dppsdk.core.validation.identifier;

/** Validates GS1 XCHAR values as represented in Digital Link URI components. */
final class Gs1XcharValidation {
    private static final String XCHAR_SYMBOLS = "!\"%&'()*+,-./:;<=>?ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz0123456789";

    private Gs1XcharValidation() { }

    static boolean isValidValue(String raw, String decoded, int maximumLength) {
        if (decoded == null || decoded.isEmpty() || decoded.length() > maximumLength) {
            return false;
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '%') {
                if (i + 2 >= raw.length()) {
                    return false;
                }
                int high = Character.digit(raw.charAt(i + 1), 16);
                int low = Character.digit(raw.charAt(i + 2), 16);
                if (high < 0 || low < 0) {
                    return false;
                }
                char decodedOctet = (char) (high * 16 + low);
                if (isUnreservedXchar(decodedOctet) || XCHAR_SYMBOLS.indexOf(decodedOctet) < 0) {
                    return false;
                }
                i += 2;
            } else if (!isUnreservedXchar(c)) {
                return false;
            }
        }
        for (int i = 0; i < decoded.length(); i++) {
            if (XCHAR_SYMBOLS.indexOf(decoded.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isUnreservedXchar(char c) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
                || c >= '0' && c <= '9' || c == '-' || c == '.' || c == '_';
    }
}
