package com.digitalqueue.api.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Examples: User not found by ID, Queue not found
 * 
 * HTTP Status: 404 Not Found
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
