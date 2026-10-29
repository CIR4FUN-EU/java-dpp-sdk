package demo.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dppsdk.core.semantic.SemanticPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Fine-granular endpoint facade for the bounded RFC 9535-compatible singular-path subset.
 */
@Service
class DppElementPathService {

    private final DppJsonPathEvaluator evaluator = new DppJsonPathEvaluator();
    private final DppMergePatchService mergePatchService;

    DppElementPathService() {
        this(new DppMergePatchService());
    }

    @Autowired
    DppElementPathService(DppMergePatchService mergePatchService) {
        this.mergePatchService = mergePatchService;
    }

    JsonNode read(JsonNode root, String elementIdPath) {
        return evaluator.read(root, elementIdPath);
    }

    SemanticPath semanticPath(JsonNode root, String elementIdPath) {
        return evaluator.semanticPath(root, elementIdPath);
    }

    JsonNode update(ObjectNode root, String elementIdPath, JsonNode payload) {
        return evaluator.replace(root, elementIdPath, payload);
    }

    JsonNode amend(ObjectNode root, String elementIdPath, JsonNode payload) {
        JsonNode current = evaluator.read(root, elementIdPath);
        JsonNode amended = mergePatchService.merge(current, payload);
        return evaluator.replace(root, elementIdPath, amended);
    }
}
