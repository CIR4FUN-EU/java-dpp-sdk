package dpp.repo.payloads;

import java.util.List;

/**
 * Request payload for resolving DPP identifiers from product identifiers.
 */
public class ReadDppIdsRequest {
    private List<String> productIdentifiers;
    private Integer limit;
    private String cursor;

    public ReadDppIdsRequest() {
    }

    public ReadDppIdsRequest(List<String> productIdentifiers, Integer limit, String cursor) {
        this.productIdentifiers = productIdentifiers;
        this.limit = limit;
        this.cursor = cursor;
    }

    public List<String> getProductIdentifiers() {
        return productIdentifiers;
    }

    public void setProductIdentifiers(List<String> productIdentifiers) {
        this.productIdentifiers = productIdentifiers;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }
}
