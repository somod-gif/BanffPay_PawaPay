package com.banffpay.pawapay.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that adds a correlation ID to every request.
 * <p>
 * If the client provides an X-Correlation-ID header, it will be used.
 * Otherwise, a new UUID is generated. The correlation ID is:
 * <ul>
 *   <li>Added to the response header X-Correlation-ID</li>
 *   <li>Stored in MDC for structured logging</li>
 *   <li>Used for distributed tracing across services</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_RESPONSE_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String correlationId = resolveCorrelationId(request);
            response.setHeader(CORRELATION_ID_RESPONSE_HEADER, correlationId);

            // Add to MDC for structured logging
            org.slf4j.MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            log.debug("Request started: method={} uri={} correlationId={}",
                    request.getMethod(), request.getRequestURI(), correlationId);

            filterChain.doFilter(request, response);

            log.debug("Request completed: method={} uri={} status={} correlationId={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), correlationId);
        } finally {
            // Clean up MDC to prevent memory leaks
            org.slf4j.MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    /**
     * Resolves the correlation ID from the request header or generates a new one.
     */
    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId.trim();
        }
        return UUID.randomUUID().toString();
    }
}