package demo.registry;

import dpp.registry.payloads.DppApiResponse;
import dpp.registry.payloads.DppStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
class GlobalExceptionHandler {

    private final ApiResponseFactory responseFactory;

    GlobalExceptionHandler(ApiResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<DppApiResponse<Void>> malformedRequest(HttpMessageNotReadableException exception) {
        return responseFactory.error(HttpStatus.BAD_REQUEST, DppStatusCode.ClientErrorBadRequest,
                "MALFORMED_JSON", "Malformed JSON payload");
    }

    @ExceptionHandler(RegistryApiException.class)
    ResponseEntity<DppApiResponse<Void>> registryError(RegistryApiException exception) {
        return responseFactory.error(httpStatus(exception.statusCode()), exception.statusCode(),
                exception.errorCode(), exception.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<DppApiResponse<Void>> routeNotFound(NoResourceFoundException exception) {
        return responseFactory.error(HttpStatus.NOT_FOUND, DppStatusCode.ClientErrorResourceNotFound,
                "ROUTE_NOT_FOUND", "Registry route not found");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<DppApiResponse<Void>> unexpected(Exception exception) {
        return responseFactory.error(HttpStatus.INTERNAL_SERVER_ERROR, DppStatusCode.ServerInternalError,
                "UNEXPECTED_ERROR", "Unexpected registry error");
    }

    private HttpStatus httpStatus(DppStatusCode statusCode) {
        return switch (statusCode) {
            case Success, SuccessCreated, SuccessAccepted, SuccessNoContent -> HttpStatus.OK;
            case ClientErrorBadRequest -> HttpStatus.BAD_REQUEST;
            case ClientNotAuthorized -> HttpStatus.UNAUTHORIZED;
            case ClientForbidden -> HttpStatus.FORBIDDEN;
            case ClientMethodNotAllowed -> HttpStatus.METHOD_NOT_ALLOWED;
            case ClientErrorResourceNotFound -> HttpStatus.NOT_FOUND;
            case ClientResourceConflict -> HttpStatus.CONFLICT;
            case ServerNotImplemented -> HttpStatus.NOT_IMPLEMENTED;
            case ServerErrorBadGateway -> HttpStatus.BAD_GATEWAY;
            case ServerInternalError -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
