package demo.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
final class CompressedDppMapper {
    JsonNode compress(JsonNode fullDpp) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("representation", "compressed");
        copy(fullDpp, result, "dppId", "passportMetadata", "uniqueProductIdentifier");
        copy(fullDpp, result, "productId", "nameplate", "gtinCode");
        copy(fullDpp, result, "productName", "characteristics", "productName");
        copy(fullDpp, result, "productCategory", "classification", "category");
        copy(fullDpp, result, "manufacturerName", "nameplate", "manufacturer", "name");
        ArrayNode available = result.putArray("availableRepresentations");
        available.add("full");
        available.add("compressed");
        return result;
    }

    private void copy(JsonNode source, ObjectNode target, String targetName, String... path) {
        JsonNode current = source;
        for (String segment : path) {
            current = current == null ? null : current.get(segment);
        }
        if (current != null && !current.isNull()) {
            target.set(targetName, current.deepCopy());
        }
    }
}
