package com.digitalqueue.api.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * Examples: Email already registered, Username already taken
 * 
 * HTTP Status: 409 Conflict
 */
public class ResourceAlreadyExistsException extends RuntimeException {
    
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }

    public ResourceAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
