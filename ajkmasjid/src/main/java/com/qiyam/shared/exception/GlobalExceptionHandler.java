package com.qiyam.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError).toList();
        var resp = ErrorResponse.builder().status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed").message("One or more fields failed validation")
                .path(req.getRequestURI()).timestamp(Instant.now()).errors(errors).build();
        log.warn("Validation error: {}", errors);
        return ResponseEntity.badRequest().body(resp);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        var resp = ErrorResponse.builder().status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized").message("Invalid username or password")
                .path(req.getRequestURI()).timestamp(Instant.now()).build();
        log.warn("Bad credentials from {}", req.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        var resp = ErrorResponse.builder().status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized").message(ex.getMessage())
                .path(req.getRequestURI()).timestamp(Instant.now()).build();
        log.warn("Auth failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        var resp = ErrorResponse.builder().status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden").message("Access denied")
                .path(req.getRequestURI()).timestamp(Instant.now()).build();
        log.warn("Access denied: {}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resp);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        var resp = ErrorResponse.builder().status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request").message(ex.getMessage())
                .path(req.getRequestURI()).timestamp(Instant.now()).build();
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(resp);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        var resp = ErrorResponse.builder().status(HttpStatus.NOT_FOUND.value())
                .error("Not Found").message(ex.getMessage())
                .path(req.getRequestURI()).timestamp(Instant.now()).build();
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
    }

    @ExceptionHandler(SupabaseException.class)
    public ResponseEntity<ErrorResponse> handleSupabaseError(SupabaseException ex, HttpServletRequest req) {
        var status = resolveHttpStatus(ex.getStatusCode());
        var resp = ErrorResponse.builder().status(status.value())
                .error(status.getReasonPhrase()).message(ex.getMessage())
                .path(req.getRequestURI()).timestamp(Instant.now()).build();
        log.error("Supabase error [{}]: {}", ex.getStatusCode(), ex.getMessage());
        return ResponseEntity.status(status).body(resp);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        var resp = ErrorResponse.builder().status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request").message("Invalid value for parameter '" + ex.getName() + "': " + ex.getValue())
                .path(req.getRequestURI()).timestamp(Instant.now()).build();
        log.warn("Type mismatch: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(resp);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        var resp = ErrorResponse.builder().status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request").message("Missing required parameter: " + ex.getParameterName())
                .path(req.getRequestURI()).timestamp(Instant.now()).build();
        log.warn("Missing param: {}", ex.getParameterName());
        return ResponseEntity.badRequest().body(resp);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        var resp = ErrorResponse.builder().status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("Method Not Allowed").message(ex.getMessage())
                .path(req.getRequestURI()).timestamp(Instant.now()).build();
        log.warn("Method not allowed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(resp);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandled(Exception ex, HttpServletRequest req) {
        var resp = ErrorResponse.builder().status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error").message("An unexpected error occurred")
                .path(req.getRequestURI()).timestamp(Instant.now()).build();
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }

    private ErrorResponse.ValidationError toValidationError(FieldError fe) {
        return ErrorResponse.ValidationError.builder()
                .field(fe.getField()).message(fe.getDefaultMessage())
                .rejectedValue(fe.getRejectedValue()).build();
    }

    private HttpStatus resolveHttpStatus(int statusCode) {
        try {
            return HttpStatus.valueOf(statusCode);
        } catch (IllegalArgumentException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
