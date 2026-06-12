package demo.repo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a correlation ID to every repository request and mirrors it back in the response.
 *
 * <p>The same ID is also written into error messages via {@link CorrelationIdHolder} so client reports
 * and server logs can be matched without a separate tracing system.</p>
 */
@Component
class RequestTraceFilter extends OncePerRequestFilter {

    static final String CORRELATION_HEADER = "X-Correlation-Id";

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTraceFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        CorrelationIdHolder.set(correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            LOGGER.info("[{}] [{}] REPO {} {} -> {}",
                    LocalTime.now().truncatedTo(ChronoUnit.SECONDS),
                    correlationId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus());
            CorrelationIdHolder.clear();
        }
    }
}
