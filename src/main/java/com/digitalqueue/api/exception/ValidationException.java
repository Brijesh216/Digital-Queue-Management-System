package com.digitalqueue.api.exception;

/**
 * Exception thrown when input validation fails.
 * Examples: Password mismatch, invalid email format
 * 
 * Note: jakarta.validation annotations throw MethodArgumentNotValidException
 * This is for custom business logic validation
 * 
 * HTTP Status: 400 Bad Request
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
