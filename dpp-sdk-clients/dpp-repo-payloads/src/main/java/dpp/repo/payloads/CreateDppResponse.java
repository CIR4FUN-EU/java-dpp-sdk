package dpp.repo.payloads;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload returned after a successful DPP creation call.
 */
public class CreateDppResponse {
    private DppStatusCode statusCode;
    private java.util.List<DppApiMessage> message;
    @JsonProperty("digitalProductPassportId")
    @JsonAlias("dppId")
    private String digitalProductPassportId;

    public CreateDppResponse() {
    }

    public CreateDppResponse(String digitalProductPassportId) {
        this.digitalProductPassportId = digitalProductPassportId;
    }

    @JsonIgnore
    public DppStatusCode getStatusCode() { return statusCode; }
    public void setStatusCode(DppStatusCode statusCode) { this.statusCode = statusCode; }
    @JsonIgnore
    public java.util.List<DppApiMessage> getMessage() { return message; }
    @JsonIgnore
    public void setMessage(java.util.List<DppApiMessage> message) { this.message = message; }

    public String getDigitalProductPassportId() {
        return digitalProductPassportId;
    }

    public void setDigitalProductPassportId(String digitalProductPassportId) {
        this.digitalProductPassportId = digitalProductPassportId;
    }

}
