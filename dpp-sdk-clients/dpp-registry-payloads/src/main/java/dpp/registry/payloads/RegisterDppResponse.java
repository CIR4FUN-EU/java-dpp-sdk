package dpp.registry.payloads;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload returned after a successful registry registration call.
 */
public class RegisterDppResponse {
    private DppStatusCode statusCode;
    private java.util.List<DppApiMessage> message;
    @JsonProperty("registrationId")
    @JsonAlias("registryIdentifier")
    private String registrationId;

    public RegisterDppResponse() {
    }

    @JsonIgnore
    public DppStatusCode getStatusCode() { return statusCode; }
    public void setStatusCode(DppStatusCode statusCode) { this.statusCode = statusCode; }
    @JsonIgnore
    public java.util.List<DppApiMessage> getMessage() { return message; }
    @JsonIgnore
    public void setMessage(java.util.List<DppApiMessage> message) { this.message = message; }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    /**
     * @deprecated Transitional source-compatibility alias.
     */
    @Deprecated
    @JsonIgnore
    public String getRegistryIdentifier() {
        return registrationId;
    }

    /**
     * @deprecated Transitional source-compatibility alias.
     */
    @Deprecated
    @JsonIgnore
    public void setRegistryIdentifier(String registryIdentifier) {
        this.registrationId = registryIdentifier;
    }
}
