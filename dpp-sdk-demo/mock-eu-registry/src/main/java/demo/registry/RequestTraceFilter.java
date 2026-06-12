package demo.registry;

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
 * Assigns and logs correlation IDs for registry requests.
 *
 * <p>The registry mock mirrors the repository mock here so clients see the same correlation header and
 * error-message behavior across both services.</p>
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
            LOGGER.info("[{}] [{}] REGISTRY {} {} -> {}",
                    LocalTime.now().truncatedTo(ChronoUnit.SECONDS),
                    correlationId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus());
            CorrelationIdHolder.clear();
        }
    }
}
