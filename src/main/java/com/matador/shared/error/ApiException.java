package com.matador.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Base for all domain-level exceptions translated to RFC 7807 ProblemDetail responses.
 * Each subclass carries a stable HTTP status and a machine-readable {@code code}.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
