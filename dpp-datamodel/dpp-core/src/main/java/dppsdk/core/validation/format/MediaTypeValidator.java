package dppsdk.core.validation.format;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Validates an open-ended media type without consulting the IANA registry. */
final class MediaTypeValidator {

    private MediaTypeValidator() { }

    static boolean isValid(String value) {
        if (value == null) return false;
        int slash = value.indexOf('/');
        if (slash <= 0 || slash != value.lastIndexOf('/')) return false;

        int semicolon = value.indexOf(';', slash + 1);
        int subtypeEnd = semicolon < 0 ? value.length() : semicolon;
        if (semicolon >= 0) {
            while (subtypeEnd > slash + 1
                    && (value.charAt(subtypeEnd - 1) == ' ' || value.charAt(subtypeEnd - 1) == '\t')) {
                subtypeEnd--;
            }
        }
        if (!isRestrictedName(value, 0, slash)
                || !isRestrictedName(value, slash + 1, subtypeEnd)) {
            return false;
        }
        if (semicolon < 0) return true;

        Set<String> names = new HashSet<>();
        int cursor = semicolon;
        while (cursor < value.length()) {
            if (value.charAt(cursor++) != ';') return false;
            cursor = skipOptionalWhitespace(value, cursor);
            int nameStart = cursor;
            while (cursor < value.length() && isRestrictedNameCharacter(value.charAt(cursor))) {
                cursor++;
            }
            if (!isRestrictedName(value, nameStart, cursor)
                    || !names.add(value.substring(nameStart, cursor).toLowerCase(Locale.ROOT))) {
                return false;
            }
            if (cursor >= value.length() || value.charAt(cursor++) != '=') return false;
            if (cursor >= value.length()) return false;
            if (value.charAt(cursor) == '"') {
                cursor = consumeQuotedString(value, cursor);
                if (cursor < 0) return false;
            } else {
                int tokenStart = cursor;
                while (cursor < value.length() && isToken(value.charAt(cursor))) cursor++;
                if (cursor == tokenStart) return false;
            }
            int beforeWhitespace = cursor;
            cursor = skipOptionalWhitespace(value, cursor);
            if (cursor == value.length() && cursor != beforeWhitespace) return false;
            if (cursor < value.length() && value.charAt(cursor) != ';') return false;
        }
        return true;
    }

    private static boolean isRestrictedName(String value, int start, int end) {
        if (end <= start || end - start > 127 || !isAlphaNumeric(value.charAt(start))) return false;
        for (int i = start + 1; i < end; i++) {
            if (!isRestrictedNameCharacter(value.charAt(i))) return false;
        }
        return true;
    }

    private static boolean isRestrictedNameCharacter(char character) {
        return isAlphaNumeric(character) || "!#$&^_.+-".indexOf(character) >= 0;
    }

    private static boolean isAlphaNumeric(char character) {
        return character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9';
    }

    private static boolean isToken(char character) {
        return isAlphaNumeric(character) || "!#$%&'*+-.^_`|~".indexOf(character) >= 0;
    }

    private static int skipOptionalWhitespace(String value, int cursor) {
        while (cursor < value.length()
                && (value.charAt(cursor) == ' ' || value.charAt(cursor) == '\t')) {
            cursor++;
        }
        return cursor;
    }

    private static int consumeQuotedString(String value, int cursor) {
        cursor++;
        while (cursor < value.length()) {
            char character = value.charAt(cursor++);
            if (character == '"') return cursor;
            if (character == '\\') {
                if (cursor >= value.length() || !isQuotedPairCharacter(value.charAt(cursor++))) {
                    return -1;
                }
            } else if (!isQuotedTextCharacter(character)) {
                return -1;
            }
        }
        return -1;
    }

    private static boolean isQuotedTextCharacter(char character) {
        return character == '\t' || character == ' ' || character == '!' || character >= '#'
                && character <= '[' || character >= ']' && character <= '~'
                || character >= 0x80 && character <= 0xff;
    }

    private static boolean isQuotedPairCharacter(char character) {
        return character == '\t' || character == ' ' || character >= '!' && character <= '~'
                || character >= 0x80 && character <= 0xff;
    }
}
