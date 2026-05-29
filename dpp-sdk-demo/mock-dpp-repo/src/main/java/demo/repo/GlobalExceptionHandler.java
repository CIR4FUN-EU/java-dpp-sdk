package demo.repo;

import dpp.repo.payloads.DppApiResponse;
import dpp.repo.payloads.DppStatusCode;
import dppsdk.core.mapper.MappingException;
import dppsdk.core.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

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
        return responseFactory.error(HttpStatus.INTERNAL_SERVER_ERROR, DppStatusCode.ServerInternalError,
                "UNEXPECTED_ERROR", "Unexpected repository error");
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
            case ServerInternalError -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
