package dpp.repo.payloads;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Application-level status values for the repository API wrapper {@code statusCode} field.
 */
public enum DppStatusCode {
    Success,
    SuccessCreated,
    SuccessAccepted,
    SuccessNoContent,
    ClientErrorBadRequest,
    ClientNotAuthorized,
    ClientForbidden,
    ClientMethodNotAllowed,
    ClientErrorResourceNotFound,
    ClientResourceConflict,
    ServerInternalError,
    ServerErrorBadGateway;

    @JsonCreator
    public static DppStatusCode fromValue(String value) {
        return switch (value) {
            case "ClientErrorNotAuthorized" -> ClientNotAuthorized;
            case "ClientErrorForbidden" -> ClientForbidden;
            default -> valueOf(value);
        };
    }

    public boolean isSuccess() {
        return switch (this) {
            case Success, SuccessCreated, SuccessAccepted, SuccessNoContent -> true;
            default -> false;
        };
    }
}
