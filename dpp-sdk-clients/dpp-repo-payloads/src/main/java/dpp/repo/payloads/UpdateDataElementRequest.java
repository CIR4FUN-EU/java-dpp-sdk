package dpp.repo.payloads;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Request payload used by the fine-granular element update endpoint.
 */
public class UpdateDataElementRequest {
    private JsonNode payload;

    public UpdateDataElementRequest() {
    }

    public UpdateDataElementRequest(JsonNode payload) {
        this.payload = payload;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}
