package dpp.registry.payloads;

/**
 * Request payload sent to {@code POST /registerDPP}.
 *
 * <p>{@code productIdentifier} and {@code operatorIdentifier} are draft-aligned registry fields.
 * {@code dppIdentifier} and {@code repoUrl} are intentional system/demo integration fields used by
 * the current repo-backed registration flow to verify that the referenced DPP is present before a
 * registry entry is accepted.</p>
 */
public class RegisterDppRequest {
    private String productIdentifier;
    private String dppIdentifier;
    private String operatorIdentifier;
    private String repoUrl;

    public RegisterDppRequest() {
    }

    public RegisterDppRequest(
            String productIdentifier,
            String dppIdentifier,
            String operatorIdentifier,
            String repoUrl
    ) {
        this.productIdentifier = productIdentifier;
        this.dppIdentifier = dppIdentifier;
        this.operatorIdentifier = operatorIdentifier;
        this.repoUrl = repoUrl;
    }

    public String getProductIdentifier() {
        return productIdentifier;
    }

    public void setProductIdentifier(String productIdentifier) {
        this.productIdentifier = productIdentifier;
    }

    public String getDppIdentifier() {
        return dppIdentifier;
    }

    public void setDppIdentifier(String dppIdentifier) {
        this.dppIdentifier = dppIdentifier;
    }

    public String getOperatorIdentifier() {
        return operatorIdentifier;
    }

    public void setOperatorIdentifier(String operatorIdentifier) {
        this.operatorIdentifier = operatorIdentifier;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }
}
