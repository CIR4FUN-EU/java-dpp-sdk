package dpp.registry.payloads;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload sent to {@code POST /v1/registerDPP}.
 */
public class RegisterDppRequest {
    @JsonProperty("uniqueProductIdentifier")
    @JsonAlias("productIdentifier")
    private String uniqueProductIdentifier;

    @JsonProperty("digitalProductPassportId")
    @JsonAlias("dppIdentifier")
    private String digitalProductPassportId;

    @JsonProperty("uniqueEconomicOperatorIdentifier")
    @JsonAlias("operatorIdentifier")
    private String uniqueEconomicOperatorIdentifier;

    @JsonProperty("dppApiEndpoint")
    @JsonAlias("repoUrl")
    private String dppApiEndpoint;

    public RegisterDppRequest() {
    }

    public RegisterDppRequest(
            String productIdentifier,
            String dppIdentifier,
            String operatorIdentifier,
            String repoUrl
    ) {
        this.uniqueProductIdentifier = productIdentifier;
        this.digitalProductPassportId = dppIdentifier;
        this.uniqueEconomicOperatorIdentifier = operatorIdentifier;
        this.dppApiEndpoint = repoUrl;
    }

    public String getUniqueProductIdentifier() {
        return uniqueProductIdentifier;
    }

    public void setUniqueProductIdentifier(String uniqueProductIdentifier) {
        this.uniqueProductIdentifier = uniqueProductIdentifier;
    }

    public String getDigitalProductPassportId() {
        return digitalProductPassportId;
    }

    public void setDigitalProductPassportId(String digitalProductPassportId) {
        this.digitalProductPassportId = digitalProductPassportId;
    }

    public String getUniqueEconomicOperatorIdentifier() {
        return uniqueEconomicOperatorIdentifier;
    }

    public void setUniqueEconomicOperatorIdentifier(String uniqueEconomicOperatorIdentifier) {
        this.uniqueEconomicOperatorIdentifier = uniqueEconomicOperatorIdentifier;
    }

    public String getDppApiEndpoint() {
        return dppApiEndpoint;
    }

    public void setDppApiEndpoint(String dppApiEndpoint) {
        this.dppApiEndpoint = dppApiEndpoint;
    }

    /**
     * @deprecated Transitional source-compatibility alias.
     */
    @Deprecated
    @JsonIgnore
    public String getProductIdentifier() {
        return uniqueProductIdentifier;
    }

    /**
     * @deprecated Transitional source-compatibility alias.
     */
    @Deprecated
    @JsonIgnore
    public void setProductIdentifier(String productIdentifier) {
        this.uniqueProductIdentifier = productIdentifier;
    }

    /**
     * @deprecated Transitional source-compatibility alias.
     */
    @Deprecated
    @JsonIgnore
    public String getDppIdentifier() {
        return digitalProductPassportId;
    }

    /**
     * @deprecated Transitional source-compatibility alias.
     */
    @Deprecated
    @JsonIgnore
    public void setDppIdentifier(String dppIdentifier) {
        this.digitalProductPassportId = dppIdentifier;
    }

    /**
     * @deprecated Transitional source-compatibility alias.
     */
    @Deprecated
    @JsonIgnore
    public String getOperatorIdentifier() {
        return uniqueEconomicOperatorIdentifier;
    }

    /**
     * @deprecated Transitional source-compatibility alias.
     */
    @Deprecated
    @JsonIgnore
    public void setOperatorIdentifier(String operatorIdentifier) {
        this.uniqueEconomicOperatorIdentifier = operatorIdentifier;
    }

    /**
     * @deprecated Transitional source-compatibility alias.
     */
    @Deprecated
    @JsonIgnore
    public String getRepoUrl() {
        return dppApiEndpoint;
    }

    /**
     * @deprecated Transitional source-compatibility alias.
     */
    @Deprecated
    @JsonIgnore
    public void setRepoUrl(String repoUrl) {
        this.dppApiEndpoint = repoUrl;
    }
}
