package demo.repo;

import dpp.repo.payloads.DppStatusCode;

class RepoApiException extends RuntimeException {

    private final DppStatusCode statusCode;
    private final String errorCode;

    RepoApiException(DppStatusCode statusCode, String errorCode, String message) {
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
