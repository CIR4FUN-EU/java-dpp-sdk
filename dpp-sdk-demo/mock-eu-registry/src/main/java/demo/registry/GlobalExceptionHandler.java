package demo.registry;

import dpp.registry.payloads.DppApiResponse;
import dpp.registry.payloads.DppStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<DppApiResponse<Void>> methodNotAllowed(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        if (!"/v1/registerDPP".equals(request.getRequestURI())) {
            return null;
        }
        return responseFactory.error(HttpStatus.METHOD_NOT_ALLOWED, DppStatusCode.ClientMethodNotAllowed,
                "METHOD_NOT_ALLOWED", "HTTP method is not allowed for this registry endpoint");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<DppApiResponse<Void>> unexpected(Exception exception) {
        log.error("Unexpected registry error", exception);
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
