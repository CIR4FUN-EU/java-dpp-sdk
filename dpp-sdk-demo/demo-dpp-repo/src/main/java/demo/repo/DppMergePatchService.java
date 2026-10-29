package demo.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dpp.repo.payloads.DppStatusCode;
import org.springframework.stereotype.Service;

/**
 * Applies JSON Merge Patch semantics for {@code PATCH /dpps/{dppId}}.
 *
 * <p>The service operates on JSON trees only. Validation of the merged DPP happens afterwards in the
 * lifecycle service so invalid patch results never become persisted state.</p>
 */
@Service
class DppMergePatchService {

    JsonNode merge(JsonNode target, JsonNode patch) {
        if (patch == null || !patch.isObject()) {
            throw new RepoApiException(DppStatusCode.ClientErrorBadRequest, "INVALID_PATCH",
                    "DPP merge patch payload must be a JSON object");
        }
        if (!(target instanceof ObjectNode targetObject)) {
            throw new RepoApiException(DppStatusCode.ServerInternalError, "INVALID_STORED_DPP",
                    "Stored DPP JSON must be a JSON object");
        }
        mergeObject(targetObject, (ObjectNode) patch);
        return targetObject;
    }

    private void mergeObject(ObjectNode target, ObjectNode patch) {
        patch.fields().forEachRemaining(entry -> {
            JsonNode patchValue = entry.getValue();
            String fieldName = entry.getKey();
            if (patchValue.isNull()) {
                // JSON Merge Patch uses explicit null to remove a field from the target document.
                target.remove(fieldName);
                return;
            }

            JsonNode existing = target.get(fieldName);
            if (patchValue.isObject() && existing != null && existing.isObject()) {
                mergeObject((ObjectNode) existing, (ObjectNode) patchValue);
                return;
            }

            target.set(fieldName, patchValue.deepCopy());
        });
    }
}
