package dpp.registry.payloads;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload returned after a successful registry registration call.
 */
public class RegisterDppResponse {
    @JsonProperty("registrationId")
    @JsonAlias("registryIdentifier")
    private String registrationId;

    public RegisterDppResponse() {
    }

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
