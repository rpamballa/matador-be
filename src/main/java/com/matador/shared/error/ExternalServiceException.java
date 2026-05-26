package com.matador.shared.error;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends ApiException {

    public ExternalServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, "EXTERNAL_SERVICE_ERROR", message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "EXTERNAL_SERVICE_ERROR", message);
        initCause(cause);
    }
}
