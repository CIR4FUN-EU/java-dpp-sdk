package dpp.repo.payloads;

/**
 * Payload returned after a successful DPP creation call.
 */
public class CreateDppResponse {
    private String dppId;

    public CreateDppResponse() {
    }

    public CreateDppResponse(String dppId) {
        this.dppId = dppId;
    }

    public String getDppId() {
        return dppId;
    }

    public void setDppId(String dppId) {
        this.dppId = dppId;
    }
}
