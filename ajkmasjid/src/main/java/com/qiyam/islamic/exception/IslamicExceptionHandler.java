package com.qiyam.islamic.exception;

import com.qiyam.islamic.dto.PublicApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Dedicated error handling for the public Islamic API, per its own contract: every failure
 * comes back as {@code {"success": false, "message": "..."}}, distinct from the rest of the
 * app's {@link com.qiyam.shared.exception.ErrorResponse} shape used elsewhere.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.qiyam.islamic")
public class IslamicExceptionHandler {

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ResponseEntity<PublicApiErrorResponse> handleUnavailable(
            ExternalServiceUnavailableException ex, HttpServletRequest req) {
        log.error("Islamic data provider unavailable on {}: {}", req.getRequestURI(), ex.getMessage());
        // Each provider sets its own user-facing message (e.g. "Prayer time service
        // temporarily unavailable") — never fabricated data, always this exact 503 shape.
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(PublicApiErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PublicApiErrorResponse> handleBadRequest(
            IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Bad request on {}: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(PublicApiErrorResponse.of(ex.getMessage()));
    }
}
