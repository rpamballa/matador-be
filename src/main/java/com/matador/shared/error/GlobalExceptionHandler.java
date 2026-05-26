package com.matador.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates every exception into an RFC 7807 {@link ProblemDetail}. Each response
 * carries a stable {@code type} URI and a machine-readable {@code code} for clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_BASE = "https://matador.app/errors/";

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException ex) {
        if (ex.status().is5xxServerError()) {
            log.error("API error [{}]: {}", ex.code(), ex.getMessage(), ex);
        } else {
            log.debug("API error [{}]: {}", ex.code(), ex.getMessage());
        }
        return problem(ex.status(), ex.code(), title(ex.status()), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .toList();
        ProblemDetail pd = problem(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "VALIDATION_FAILED",
            "Validation failed",
            "One or more fields are invalid.");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "MALFORMED_REQUEST",
            "Malformed request",
            "Request body could not be read.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return problem(
            HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized", "Authentication required.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(
            HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden", "You do not have access to this resource.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "Internal server error",
            "An unexpected error occurred.");
    }

    private ProblemDetail problem(HttpStatus status, String code, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create(TYPE_BASE + code.toLowerCase().replace('_', '-')));
        pd.setProperty("code", code);
        return pd;
    }

    private String formatFieldError(FieldError fe) {
        return "%s: %s".formatted(fe.getField(), fe.getDefaultMessage());
    }

    private String title(HttpStatus status) {
        return status.getReasonPhrase();
    }
}
