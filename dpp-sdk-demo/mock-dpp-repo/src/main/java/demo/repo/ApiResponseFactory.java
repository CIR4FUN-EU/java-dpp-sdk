package demo.repo;

import dpp.repo.payloads.DppApiMessage;
import dpp.repo.payloads.DppApiResponse;
import dpp.repo.payloads.DppStatusCode;
import dpp.repo.payloads.MessageType;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Builds the standard API wrapper used by the mock repository service.
 *
 * <p>The factory keeps response construction consistent across controllers and exception handling,
 * including correlation IDs on error messages.</p>
 */
@Component
class ApiResponseFactory {

    <T> ResponseEntity<DppApiResponse<T>> success(HttpStatus httpStatus, DppStatusCode statusCode, T payload) {
        DppApiResponse<T> response = new DppApiResponse<>();
        response.setStatusCode(statusCode);
        response.setPayload(payload);
        return ResponseEntity.status(httpStatus).body(response);
    }

    ResponseEntity<DppApiResponse<Void>> successNoContent() {
        DppApiResponse<Void> response = new DppApiResponse<>();
        response.setStatusCode(DppStatusCode.SuccessNoContent);
        return ResponseEntity.ok(response);
    }

    ResponseEntity<DppApiResponse<Void>> error(HttpStatus httpStatus, DppStatusCode statusCode, String code, String message) {
        // Error messages carry the request correlation ID so client-side reports can be matched to server logs.
        DppApiMessage apiMessage = new DppApiMessage();
        apiMessage.setMessageType(MessageType.Error);
        apiMessage.setText(message);
        apiMessage.setCode(code);
        apiMessage.setCorrelationId(CorrelationIdHolder.get());
        apiMessage.setTimestamp(Instant.now());

        DppApiResponse<Void> response = new DppApiResponse<>();
        response.setStatusCode(statusCode);
        response.setMessages(List.of(apiMessage));
        return ResponseEntity.status(httpStatus).body(response);
    }
}
