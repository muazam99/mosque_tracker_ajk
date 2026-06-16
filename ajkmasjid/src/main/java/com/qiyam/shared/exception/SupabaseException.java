package com.qiyam.shared.exception;

public class SupabaseException extends RuntimeException {
    private final int statusCode;

    public SupabaseException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
