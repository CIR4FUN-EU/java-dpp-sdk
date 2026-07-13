package demo.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

/**
 * Fine-granular endpoint facade for the bounded RFC 9535-compatible singular-path subset.
 */
@Service
class DppElementPathService {

    private final DppJsonPathEvaluator evaluator = new DppJsonPathEvaluator();

    JsonNode read(JsonNode root, String elementIdPath) {
        return evaluator.read(root, elementIdPath);
    }

    JsonNode update(ObjectNode root, String elementIdPath, JsonNode payload) {
        return evaluator.replace(root, elementIdPath, payload);
    }
}
