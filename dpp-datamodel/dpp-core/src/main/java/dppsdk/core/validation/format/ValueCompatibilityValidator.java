package dppsdk.core.validation.format;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Set;

/** Applies only the explicit JSON mappings in EN 18223 Table 7. */
final class ValueCompatibilityValidator {

    private static final Set<LocalDate> POSITIVE_UTC_LEAP_SECOND_DATES = Set.of(
            LocalDate.parse("1972-06-30"), LocalDate.parse("1972-12-31"),
            LocalDate.parse("1973-12-31"), LocalDate.parse("1974-12-31"),
            LocalDate.parse("1975-12-31"), LocalDate.parse("1976-12-31"),
            LocalDate.parse("1977-12-31"), LocalDate.parse("1978-12-31"),
            LocalDate.parse("1979-12-31"), LocalDate.parse("1981-06-30"),
            LocalDate.parse("1982-06-30"), LocalDate.parse("1983-06-30"),
            LocalDate.parse("1985-06-30"), LocalDate.parse("1987-12-31"),
            LocalDate.parse("1989-12-31"), LocalDate.parse("1990-12-31"),
            LocalDate.parse("1992-06-30"), LocalDate.parse("1993-06-30"),
            LocalDate.parse("1994-06-30"), LocalDate.parse("1995-12-31"),
            LocalDate.parse("1997-06-30"), LocalDate.parse("1998-12-31"),
            LocalDate.parse("2005-12-31"), LocalDate.parse("2008-12-31"),
            LocalDate.parse("2012-06-30"), LocalDate.parse("2015-06-30"),
            LocalDate.parse("2016-12-31"));

    record Failure(String rule, String message) { }

    private ValueCompatibilityValidator() { }

    static Failure validate(String builtInName, Object value) {
        return switch (builtInName) {
            case "string" -> value instanceof String ? null : incompatible("string");
            case "boolean" -> value instanceof Boolean ? null : incompatible("boolean");
            case "decimal" -> validateDecimal(value);
            case "integer" -> validateInteger(value);
            case "float", "double" -> value instanceof Number ? null : incompatible("number");
            case "date" -> validateDate(value);
            case "dateTime" -> validateDateTime(value);
            case "anyURI" -> validateUri(value);
            case "base64Binary" -> validateBase64(value);
            default -> validateDerivedInteger(builtInName, value);
        };
    }

    private static Failure validateDecimal(Object value) {
        if (value instanceof Number) return null;
        if (!(value instanceof String text)) return incompatible("number or decimal string");
        String normalized = collapseSchemaWhitespace(text);
        if (!normalized.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)")) {
            return invalid("decimal string");
        }
        try {
            new BigDecimal(normalized);
            return null;
        } catch (NumberFormatException exception) {
            return invalid("decimal string");
        }
    }

    private static Failure validateInteger(Object value) {
        if (!(value instanceof Number number)) return incompatible("integral number");
        return asInteger(number) == null ? incompatible("integral number") : null;
    }

    private static Failure validateDate(Object value) {
        if (!(value instanceof String text)) return incompatible("date string");
        if (!text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return invalid("YYYY-MM-DD date");
        try {
            LocalDate.parse(text);
            return null;
        } catch (DateTimeParseException exception) {
            return invalid("real calendar date");
        }
    }

    private static Failure validateDateTime(Object value) {
        if (!(value instanceof String text)) return incompatible("UTC dateTime string");
        if (!text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z")) {
            return invalid("YYYY-MM-DDThh:mm:ssZ dateTime");
        }
        try {
            LocalDate date = LocalDate.parse(text.substring(0, 10));
            int hour = Integer.parseInt(text.substring(11, 13));
            int minute = Integer.parseInt(text.substring(14, 16));
            int second = Integer.parseInt(text.substring(17, 19));
            if (hour == 24 && minute == 0 && second == 0) return null;
            if (hour > 23 || minute > 59) return invalid("real UTC dateTime");
            if (second <= 59) return null;
            if (second == 60 && hour == 23 && minute == 59
                    && POSITIVE_UTC_LEAP_SECOND_DATES.contains(date)) return null;
            return invalid("real UTC dateTime");
        } catch (DateTimeParseException exception) {
            return invalid("real UTC dateTime");
        }
    }

    private static Failure validateUri(Object value) {
        if (!(value instanceof String text)) return incompatible("URI string");
        return ResourceUrlValidator.isValid(text) ? null : invalid("RFC 3986 URI");
    }

    private static Failure validateBase64(Object value) {
        if (!(value instanceof String text)) return incompatible("base64 string");
        String collapsed = collapseSchemaWhitespace(text);
        String compact = collapsed.replace(" ", "");
        if (compact.length() % 4 != 0) return invalid("base64 encoding");
        try {
            byte[] decoded = Base64.getDecoder().decode(compact);
            return Base64.getEncoder().encodeToString(decoded).equals(compact)
                    ? null : invalid("base64 encoding");
        } catch (IllegalArgumentException exception) {
            return invalid("base64 encoding");
        }
    }

    private static String collapseSchemaWhitespace(String value) {
        StringBuilder collapsed = new StringBuilder(value.length());
        boolean pendingSpace = false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == ' ' || character == '\t' || character == '\r' || character == '\n') {
                pendingSpace = collapsed.length() > 0;
            } else {
                if (pendingSpace) collapsed.append(' ');
                collapsed.append(character);
                pendingSpace = false;
            }
        }
        return collapsed.toString();
    }

    private static Failure validateDerivedInteger(String type, Object value) {
        IntegerRange range = rangeFor(type);
        if (range == null || !(value instanceof Number number)) return null;
        BigInteger integer = asInteger(number);
        if (integer == null) return incompatible("integral number");
        if (range.minimum() != null && integer.compareTo(range.minimum()) < 0
                || range.maximum() != null && integer.compareTo(range.maximum()) > 0) {
            return new Failure("VALUE_FORMAT_INVALID", "value is outside the " + type + " integer range");
        }
        return null;
    }

    private static IntegerRange rangeFor(String type) {
        return switch (type) {
            case "nonPositiveInteger" -> new IntegerRange(null, BigInteger.ZERO);
            case "negativeInteger" -> new IntegerRange(null, BigInteger.ONE.negate());
            case "long" -> new IntegerRange(BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE));
            case "int" -> new IntegerRange(BigInteger.valueOf(Integer.MIN_VALUE), BigInteger.valueOf(Integer.MAX_VALUE));
            case "short" -> new IntegerRange(BigInteger.valueOf(Short.MIN_VALUE), BigInteger.valueOf(Short.MAX_VALUE));
            case "byte" -> new IntegerRange(BigInteger.valueOf(Byte.MIN_VALUE), BigInteger.valueOf(Byte.MAX_VALUE));
            case "nonNegativeInteger" -> new IntegerRange(BigInteger.ZERO, null);
            case "unsignedLong" -> new IntegerRange(BigInteger.ZERO, new BigInteger("18446744073709551615"));
            case "unsignedInt" -> new IntegerRange(BigInteger.ZERO, new BigInteger("4294967295"));
            case "unsignedShort" -> new IntegerRange(BigInteger.ZERO, BigInteger.valueOf(65535));
            case "unsignedByte" -> new IntegerRange(BigInteger.ZERO, BigInteger.valueOf(255));
            case "positiveInteger" -> new IntegerRange(BigInteger.ONE, null);
            default -> null;
        };
    }

    private static BigInteger asInteger(Number number) {
        try {
            return asDecimal(number).toBigIntegerExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            return null;
        }
    }

    private static BigDecimal asDecimal(Number number) {
        if (number instanceof BigDecimal decimal) return decimal;
        if (number instanceof BigInteger integer) return new BigDecimal(integer);
        if (number instanceof Float || number instanceof Double) return BigDecimal.valueOf(number.doubleValue());
        return BigDecimal.valueOf(number.longValue());
    }

    private static Failure incompatible(String expected) {
        return new Failure("VALUE_TYPE_INCOMPATIBLE", "value must be a JSON " + expected);
    }

    private static Failure invalid(String expected) {
        return new Failure("VALUE_FORMAT_INVALID", "value is not in the required " + expected + " format");
    }

    private record IntegerRange(BigInteger minimum, BigInteger maximum) { }
}
