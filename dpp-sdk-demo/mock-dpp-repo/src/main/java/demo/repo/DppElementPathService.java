package demo.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dpp.repo.payloads.DppStatusCode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Resolves the limited element-path syntax supported by the fine-granular mock endpoints.
 *
 * <p>This is deliberately not a generic JSONPath engine. The supported path list is curated so the
 * mock service exposes only the fine-granular fields that are part of the internal demo contract.</p>
 */
@Service
class DppElementPathService {

    private static final Pattern SEGMENT_PATTERN = Pattern.compile("([A-Za-z0-9_]+)(?:\\[(\\d+)])?");

    JsonNode read(JsonNode root, String requestedPath) {
        String normalized = normalizePath(requestedPath);
        JsonNode current = root;
        for (PathSegment segment : tokenize(normalized)) {
            if (current == null || current.isMissingNode()) {
                throw new RepoApiException(DppStatusCode.ClientErrorResourceNotFound, "ELEMENT_NOT_FOUND",
                        "No element found for path " + requestedPath);
            }
            current = current.get(segment.name());
            if (current == null) {
                throw new RepoApiException(DppStatusCode.ClientErrorResourceNotFound, "ELEMENT_NOT_FOUND",
                        "No element found for path " + requestedPath);
            }
            if (segment.index() != null) {
                if (!current.isArray() || current.size() <= segment.index()) {
                    throw new RepoApiException(DppStatusCode.ClientErrorResourceNotFound, "ELEMENT_NOT_FOUND",
                            "No element found for path " + requestedPath);
                }
                current = current.get(segment.index());
            }
        }
        return current.deepCopy();
    }

    JsonNode update(ObjectNode root, String requestedPath, JsonNode payload) {
        String normalized = normalizePath(requestedPath);
        List<PathSegment> segments = tokenize(normalized);
        if (segments.isEmpty()) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "UNSUPPORTED_ELEMENT_PATH",
                    "Unsupported element path " + requestedPath);
        }

        JsonNode current = root;
        for (int index = 0; index < segments.size() - 1; index++) {
            PathSegment segment = segments.get(index);
            current = current.get(segment.name());
            if (current == null) {
                throw new RepoApiException(DppStatusCode.ClientErrorResourceNotFound, "ELEMENT_NOT_FOUND",
                        "No element found for path " + requestedPath);
            }
            if (segment.index() != null) {
                if (!current.isArray() || current.size() <= segment.index()) {
                    throw new RepoApiException(DppStatusCode.ClientErrorResourceNotFound, "ELEMENT_NOT_FOUND",
                            "No element found for path " + requestedPath);
                }
                current = current.get(segment.index());
            }
        }

        PathSegment leaf = segments.get(segments.size() - 1);
        if (leaf.index() == null) {
            if (!(current instanceof ObjectNode currentObject) || currentObject.get(leaf.name()) == null) {
                throw new RepoApiException(DppStatusCode.ClientErrorResourceNotFound, "ELEMENT_NOT_FOUND",
                        "No element found for path " + requestedPath);
            }
            currentObject.set(leaf.name(), payload.deepCopy());
            return currentObject.get(leaf.name()).deepCopy();
        }

        JsonNode arrayNode = current.get(leaf.name());
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.size() <= leaf.index()) {
            throw new RepoApiException(DppStatusCode.ClientErrorResourceNotFound, "ELEMENT_NOT_FOUND",
                    "No element found for path " + requestedPath);
        }
        ((com.fasterxml.jackson.databind.node.ArrayNode) arrayNode).set(leaf.index(), payload.deepCopy());
        return arrayNode.get(leaf.index()).deepCopy();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "UNSUPPORTED_ELEMENT_PATH",
                    "Element path must not be blank");
        }
        // The client may send either "coreDpp.*" aliases or the flattened JSON field names produced by the codec.
        String normalized = path.startsWith("coreDpp.") ? path.substring("coreDpp.".length()) : path;
        if (normalized.equals("passportMetadata.uniqueProductIdentifier")
                || normalized.equals("nameplate.gtinCode")
                || normalized.equals("characteristics.productName")
                || normalized.equals("characteristics.productType")
                || normalized.equals("classification.category")
                || normalized.equals("classification.sector")
                || normalized.equals("documentation.digitalInstructionsLink")
                || normalized.equals("passportMetadata.externalDocumentationLink")
                || normalized.equals("billOfMaterials")
                || normalized.startsWith("billOfMaterials.")) {
            return normalized;
        }
        throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "UNSUPPORTED_ELEMENT_PATH",
                "Unsupported element path " + path);
    }

    private List<PathSegment> tokenize(String path) {
        List<PathSegment> segments = new ArrayList<>();
        for (String rawSegment : path.split("\\.")) {
            Matcher matcher = SEGMENT_PATTERN.matcher(rawSegment);
            if (!matcher.matches()) {
                throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "UNSUPPORTED_ELEMENT_PATH",
                        "Unsupported element path " + path);
            }
            String name = matcher.group(1);
            String indexValue = matcher.group(2);
            segments.add(new PathSegment(name, indexValue == null ? null : Integer.parseInt(indexValue)));
        }
        return segments;
    }

    private record PathSegment(String name, Integer index) {
    }
}
