package demo.registry;

import dpp.registry.payloads.DppApiMessage;
import dpp.registry.payloads.DppApiResponse;
import dpp.registry.payloads.DppStatusCode;
import dpp.registry.payloads.MessageType;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Builds the standard API wrapper used by the mock registry service.
 *
 * <p>The registry and repository mocks intentionally share the same envelope shape so the client can
 * parse both services uniformly.</p>
 */
@Component
class ApiResponseFactory {

    <T> ResponseEntity<DppApiResponse<T>> success(HttpStatus httpStatus, DppStatusCode statusCode, T payload) {
        DppApiResponse<T> response = new DppApiResponse<>();
        response.setStatusCode(statusCode);
        response.setPayload(payload);
        return ResponseEntity.status(httpStatus).body(response);
    }

    ResponseEntity<DppApiResponse<Void>> error(HttpStatus httpStatus, DppStatusCode statusCode, String code, String message) {
        // Error responses include correlation IDs for traceability across client logs and server logs.
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
