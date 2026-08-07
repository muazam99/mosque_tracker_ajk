package com.qiyam.shared.exception;

/** Thrown when reading the uploaded file or writing it to object storage (R2) fails. */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(String message) {
        super(message);
    }
}
