package dppsdk.core.validation.identifier;

/** GS1 modulo-10 check digit for numeric identification keys. */
final class Gs1KeyValidation {
    private Gs1KeyValidation() { }

    static boolean hasCheckDigit(String key) {
        int sum = 0;
        for (int i = key.length() - 2, weight = 3; i >= 0; i--, weight = 4 - weight) {
            sum += (key.charAt(i) - '0') * weight;
        }
        return (10 - sum % 10) % 10 == key.charAt(key.length() - 1) - '0';
    }

    static boolean isDigits(String value, int expectedLength) {
        if (value.length() != expectedLength) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < '0' || value.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }
}
