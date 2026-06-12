package dpp.registry.payloads;

/**
 * Payload returned after a successful registry registration call.
 */
public class RegisterDppResponse {
    private String registryIdentifier;

    public RegisterDppResponse() {
    }

    public String getRegistryIdentifier() {
        return registryIdentifier;
    }

    public void setRegistryIdentifier(String registryIdentifier) {
        this.registryIdentifier = registryIdentifier;
    }
}
