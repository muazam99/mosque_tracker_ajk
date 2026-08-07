package com.qiyam.islamic.exception;

/**
 * Thrown whenever an upstream Islamic data provider (Aladhan, alquran.cloud, hadith dataset)
 * is unreachable or returns an error. Never caught to fall back to fabricated data — the
 * global handler turns this straight into a 503.
 */
public class ExternalServiceUnavailableException extends RuntimeException {
    public ExternalServiceUnavailableException(String message) {
        super(message);
    }

    public ExternalServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
