package demo.registry;

import dpp.registry.payloads.DppStatusCode;

class RegistryApiException extends RuntimeException {

    private final DppStatusCode statusCode;
    private final String errorCode;

    RegistryApiException(DppStatusCode statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    DppStatusCode statusCode() {
        return statusCode;
    }

    String errorCode() {
        return errorCode;
    }
}
