package com.digitalqueue.api.exception;

import com.digitalqueue.api.dto.response.AuthResponse;
import com.digitalqueue.constants.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler - Centralized exception handling for entire API.
 * 
 * @RestControllerAdvice: Applies to all @RestController classes
 *   - Catches exceptions thrown from controllers/services
 *   - Returns standardized error responses
 *   - Prevents stack traces from leaking to client
 * 
 * Benefits:
 * - Consistent error response format across API
 * - Centralized error handling logic
 * - Easy to add new exception types
 * - Logs all errors for debugging
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotation.
     * 
     * When: @Valid on DTO detects validation failures
     *   - Example: @Email fails, @Size fails, etc.
     * 
     * Response (400):
     * {
     *   "success": false,
     *   "message": "Validation failed",
     *   "data": {
     *     "email": "Email must be valid",
     *     "password": "Password must be between 8 and 100 characters"
     *   },
     *   "timestamp": "2024-05-26T10:30:00"
     * }
     * 
     * @param ex: Spring's exception containing all validation errors
     * @return: ResponseEntity with 400 status and error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getFieldError());

        // Extract field errors: field name -> error message
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                error -> error.getField(),
                error -> error.getDefaultMessage(),
                (existing, replacement) -> existing // Keep first error if duplicate field
            ));

        AuthResponse response = AuthResponse.error("Validation failed", errors);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }

    /**
     * Handle ResourceAlreadyExistsException.
     * 
     * When: Trying to register with existing email/username
     * 
     * Response (409):
     * {
     *   "success": false,
     *   "message": "Email already registered",
     *   "timestamp": "2024-05-26T10:30:00"
     * }
     * 
     * @param ex: Custom exception
     * @return: ResponseEntity with 409 status
     */
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<AuthResponse> handleResourceAlreadyExistsException(
            ResourceAlreadyExistsException ex) {
        log.warn("Resource already exists: {}", ex.getMessage());

        AuthResponse response = AuthResponse.error(ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(response);
    }

    /**
     * Handle ValidationException (custom business logic validation).
     * 
     * When: Password mismatch, custom business rule fails
     * 
     * Response (400):
     * {
     *   "success": false,
     *   "message": "Passwords do not match",
     *   "timestamp": "2024-05-26T10:30:00"
     * }
     * 
     * @param ex: Custom exception
     * @return: ResponseEntity with 400 status
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<AuthResponse> handleValidationException(
            ValidationException ex) {
        log.warn("Validation exception: {}", ex.getMessage());

        AuthResponse response = AuthResponse.error(ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }

    /**
     * Handle ResourceNotFoundException.
     * 
     * When: Trying to access non-existent user/queue
     * 
     * Response (404):
     * {
     *   "success": false,
     *   "message": "User not found",
     *   "timestamp": "2024-05-26T10:30:00"
     * }
     * 
     * @param ex: Custom exception
     * @return: ResponseEntity with 404 status
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<AuthResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        AuthResponse response = AuthResponse.error(ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response);
    }

    /**
     * Handle all other exceptions (catch-all).
     * 
     * When: Unexpected exceptions occur (database errors, NPE, etc.)
     * 
     * Response (500):
     * {
     *   "success": false,
     *   "message": "An internal server error occurred",
     *   "timestamp": "2024-05-26T10:30:00"
     * }
     * 
     * Note: Never expose actual exception message to client (security risk)
     *       Log the actual error for debugging
     * 
     * @param ex: Any exception
     * @return: ResponseEntity with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponse> handleGlobalException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        AuthResponse response = AuthResponse.error(AppConstants.INTERNAL_SERVER_ERROR);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}
