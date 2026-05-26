package com.matador.shared.audit;

import com.matador.shared.security.CurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Logs one structured line per request and propagates {@code trace_id} / {@code user_id}
 * into the MDC so every downstream log statement is correlated. Request/response bodies
 * are never logged (avoids leaking PII, passwords, and payment tokens).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("com.matador.request");
    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        String traceId = resolveTraceId(request);
        long start = System.nanoTime();
        MDC.put("trace_id", traceId);
        response.setHeader(TRACE_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            // user id is only known after authentication has run.
            CurrentUser.find().ifPresent(u -> MDC.put("user_id", u.id().toString()));
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            log.info(
                "{} {} -> {} ({} ms) ip={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMs,
                clientIp(request));
            MDC.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String incoming = request.getHeader(TRACE_HEADER);
        return (incoming != null && !incoming.isBlank()) ? incoming : UUID.randomUUID().toString();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
