package demo.repo;

import dpp.repo.payloads.DppApiResponse;
import dpp.repo.payloads.DppStatusCode;
import dppsdk.core.mapper.MappingException;
import dppsdk.core.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApiResponseFactory responseFactory;

    GlobalExceptionHandler(ApiResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<DppApiResponse<Void>> validationFailure(ValidationException exception) {
        return responseFactory.error(HttpStatus.BAD_REQUEST, DppStatusCode.ClientErrorBadRequest,
                "DPP_VALIDATION_FAILED", "Validation failed: " + exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<DppApiResponse<Void>> malformedRequest(HttpMessageNotReadableException exception) {
        return responseFactory.error(HttpStatus.BAD_REQUEST, DppStatusCode.ClientErrorBadRequest,
                "MALFORMED_JSON", "Malformed JSON payload");
    }

    @ExceptionHandler({IllegalArgumentException.class, MappingException.class})
    ResponseEntity<DppApiResponse<Void>> mappingFailure(Exception exception) {
        return responseFactory.error(HttpStatus.BAD_REQUEST, DppStatusCode.ClientErrorBadRequest,
                "MALFORMED_JSON", exception.getMessage() == null ? "Malformed JSON payload" : exception.getMessage());
    }

    @ExceptionHandler(RepoApiException.class)
    ResponseEntity<DppApiResponse<Void>> repoError(RepoApiException exception) {
        return responseFactory.error(httpStatus(exception.statusCode()), exception.statusCode(),
                exception.errorCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<DppApiResponse<Void>> unexpected(Exception exception) {
        log.error("Unexpected repository error", exception);
        return responseFactory.error(HttpStatus.INTERNAL_SERVER_ERROR, DppStatusCode.ServerInternalError,
                "UNEXPECTED_ERROR", "Unexpected repository error");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<DppApiResponse<Void>> routeNotFound(NoResourceFoundException exception) {
        return responseFactory.error(HttpStatus.NOT_FOUND, DppStatusCode.ClientErrorResourceNotFound,
                "ROUTE_NOT_FOUND", "Repository route not found");
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
            case ServerErrorBadGateway -> HttpStatus.BAD_GATEWAY;
            case ServerNotImplemented -> HttpStatus.NOT_IMPLEMENTED;
            case ServerInternalError -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
