package demo.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dpp.repo.payloads.DppStatusCode;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates the repository's bounded RFC 9535-compatible absolute singular-path subset.
 *
 * <p>Supported selectors are root ({@code $}), RFC member-name shorthand, quoted bracket member
 * selectors, and non-negative array indexes. Selectors that can produce multiple nodes are
 * deliberately outside this mock repository subset.</p>
 */
final class DppJsonPathEvaluator {

    private static final int MAX_PATH_LENGTH = 4_096;
    private static final int MAX_SEGMENTS = 64;

    JsonNode read(JsonNode root, String elementIdPath) {
        return select(root, elementIdPath).value().deepCopy();
    }

    JsonNode replace(ObjectNode root, String elementIdPath, JsonNode replacement) {
        Selection selection = select(root, elementIdPath);
        if (selection.isRoot()) {
            throw badRequest("ROOT_REPLACEMENT_NOT_ALLOWED",
                    "Replacing the complete DPP is not supported by the fine-granular endpoint");
        }
        JsonNode copy = replacement.deepCopy();
        if (selection.objectParent() != null) {
            selection.objectParent().set(selection.memberName(), copy);
        } else {
            selection.arrayParent().set(selection.arrayIndex(), copy);
        }
        return copy.deepCopy();
    }

    private Selection select(JsonNode root, String elementIdPath) {
        List<Selector> selectors = new Parser(elementIdPath).parse();
        JsonNode current = root;
        Selection selection = Selection.root(root);
        for (Selector selector : selectors) {
            if (selector instanceof MemberSelector member) {
                if (!(current instanceof ObjectNode objectNode)) {
                    throw notFound(elementIdPath);
                }
                JsonNode next = objectNode.get(member.name());
                if (next == null) {
                    throw notFound(elementIdPath);
                }
                selection = Selection.member(next, objectNode, member.name());
                current = next;
            } else {
                IndexSelector index = (IndexSelector) selector;
                if (!(current instanceof ArrayNode arrayNode) || index.index() >= arrayNode.size()) {
                    throw notFound(elementIdPath);
                }
                JsonNode next = arrayNode.get(index.index());
                selection = Selection.index(next, arrayNode, index.index());
                current = next;
            }
        }
        return selection;
    }

    private static RepoApiException notFound(String path) {
        return new RepoApiException(DppStatusCode.ClientErrorResourceNotFound, "ELEMENT_NOT_FOUND",
                "No element found for path " + path);
    }

    private static RepoApiException badRequest(String code, String message) {
        return new RepoApiException(DppStatusCode.ClientErrorBadRequest, code, message);
    }

    private static RepoApiException notImplemented(String path) {
        return new RepoApiException(DppStatusCode.ServerNotImplemented, "JSONPATH_FEATURE_NOT_IMPLEMENTED",
                "JSONPath feature is not supported by this mock repository: " + path);
    }

    private sealed interface Selector permits MemberSelector, IndexSelector {
    }

    private record MemberSelector(String name) implements Selector {
    }

    private record IndexSelector(int index) implements Selector {
    }

    private record Selection(JsonNode value, ObjectNode objectParent, ArrayNode arrayParent,
                             String memberName, int arrayIndex, boolean isRoot) {
        static Selection root(JsonNode value) {
            return new Selection(value, null, null, null, -1, true);
        }

        static Selection member(JsonNode value, ObjectNode parent, String memberName) {
            return new Selection(value, parent, null, memberName, -1, false);
        }

        static Selection index(JsonNode value, ArrayNode parent, int arrayIndex) {
            return new Selection(value, null, parent, null, arrayIndex, false);
        }
    }

    private static final class Parser {
        private final String path;
        private int position;

        Parser(String path) {
            this.path = path;
        }

        List<Selector> parse() {
            if (path == null || path.isBlank()) {
                throw badRequest("INVALID_ELEMENT_ID_PATH", "elementIdPath must not be blank");
            }
            if (path.length() > MAX_PATH_LENGTH) {
                throw badRequest("ELEMENT_ID_PATH_TOO_LONG", "elementIdPath exceeds " + MAX_PATH_LENGTH + " characters");
            }
            if (path.charAt(position++) != '$') {
                throw badRequest("INVALID_ELEMENT_ID_PATH", "elementIdPath must start with $");
            }

            List<Selector> selectors = new ArrayList<>();
            while (position < path.length()) {
                if (selectors.size() == MAX_SEGMENTS) {
                    throw badRequest("ELEMENT_ID_PATH_TOO_DEEP", "elementIdPath exceeds " + MAX_SEGMENTS + " segments");
                }
                char next = path.charAt(position);
                if (next == '.') {
                    selectors.add(parseDotMember());
                } else if (next == '[') {
                    selectors.add(parseBracketSelector());
                } else {
                    throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
                }
            }
            return selectors;
        }

        private Selector parseDotMember() {
            position++;
            if (position >= path.length()) {
                throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
            }
            if (path.charAt(position) == '.' || path.charAt(position) == '*') {
                throw notImplemented(path);
            }
            int start = position;
            int first = path.codePointAt(position);
            if (!isNameFirst(first)) {
                throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
            }
            position += Character.charCount(first);
            while (position < path.length()) {
                int codePoint = path.codePointAt(position);
                if (!isNameChar(codePoint)) {
                    break;
                }
                position += Character.charCount(codePoint);
            }
            return new MemberSelector(path.substring(start, position));
        }

        private Selector parseBracketSelector() {
            position++;
            if (position >= path.length()) {
                throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
            }
            char first = path.charAt(position);
            if (first == '*' || first == '?' || first == ':') {
                throw notImplemented(path);
            }
            if (first == '-' || Character.isDigit(first)) {
                return parseIndexSelector();
            }
            if (first == '\'' || first == '"') {
                return new MemberSelector(parseQuotedMember(first));
            }
            throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
        }

        private Selector parseIndexSelector() {
            int start = position;
            if (path.charAt(position) == '-') {
                throw notImplemented(path);
            }
            if (path.charAt(position) == '0') {
                position++;
                if (position < path.length() && Character.isDigit(path.charAt(position))) {
                    throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
                }
            } else {
                while (position < path.length() && Character.isDigit(path.charAt(position))) {
                    position++;
                }
            }
            if (position >= path.length()) {
                throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
            }
            if (path.charAt(position) == ':' || path.charAt(position) == ',') {
                throw notImplemented(path);
            }
            if (path.charAt(position) != ']') {
                throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
            }
            String number = path.substring(start, position++);
            try {
                return new IndexSelector(Integer.parseInt(number));
            } catch (NumberFormatException exception) {
                throw badRequest("INVALID_ELEMENT_ID_PATH", "Array index is outside the supported range");
            }
        }

        private String parseQuotedMember(char quote) {
            position++;
            StringBuilder result = new StringBuilder();
            while (position < path.length()) {
                char current = path.charAt(position++);
                if (current == quote) {
                    if (position >= path.length()) {
                        throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
                    }
                    if (path.charAt(position) == ',') {
                        throw notImplemented(path);
                    }
                    if (path.charAt(position++) != ']') {
                        throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
                    }
                    return result.toString();
                }
                if (current != '\\') {
                    result.append(current);
                    continue;
                }
                if (position >= path.length()) {
                    throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
                }
                char escaped = path.charAt(position++);
                if ((escaped == '\'' || escaped == '"') && escaped != quote) {
                    throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
                }
                switch (escaped) {
                    case '\\', '/', '\'', '"' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> result.append(parseUnicodeEscape());
                    default -> throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
                }
            }
            throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
        }

        private String parseUnicodeEscape() {
            if (position + 4 > path.length()) {
                throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
            }
            String hexadecimal = path.substring(position, position + 4);
            position += 4;
            try {
                char decoded = (char) Integer.parseInt(hexadecimal, 16);
                if (Character.isLowSurrogate(decoded)) {
                    throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
                }
                if (!Character.isHighSurrogate(decoded)) {
                    return String.valueOf(decoded);
                }
                if (position + 6 > path.length() || path.charAt(position) != '\\'
                        || path.charAt(position + 1) != 'u') {
                    throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
                }
                String lowHexadecimal = path.substring(position + 2, position + 6);
                char low = (char) Integer.parseInt(lowHexadecimal, 16);
                if (!Character.isLowSurrogate(low)) {
                    throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
                }
                position += 6;
                return new String(new char[]{decoded, low});
            } catch (NumberFormatException exception) {
                throw badRequest("INVALID_ELEMENT_ID_PATH", "Malformed elementIdPath " + path);
            }
        }

        private static boolean isNameFirst(int codePoint) {
            return codePoint == '_' || (codePoint >= 'A' && codePoint <= 'Z')
                    || (codePoint >= 'a' && codePoint <= 'z') || codePoint >= 0x80;
        }

        private static boolean isNameChar(int codePoint) {
            return isNameFirst(codePoint) || (codePoint >= '0' && codePoint <= '9');
        }
    }
}
