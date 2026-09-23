package com.digitalqueue.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AuthResponse DTO - Standardized response for all authentication endpoints.
 * 
 * JSON Annotation:
 * @JsonInclude(INCLUDE.NON_NULL): Excludes null fields from JSON output
 * This keeps response compact and clean
 * 
 * Example Response (Register):
 * {
 *   "success": true,
 *   "message": "User registered successfully",
 *   "data": { "id": 1, "username": "john", "email": "john@example.com" },
 *   "timestamp": "2024-05-26T10:30:00"
 * }
 * 
 * Example Response (Error):
 * {
 *   "success": false,
 *   "message": "Email already exists",
 *   "timestamp": "2024-05-26T10:30:00"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    /**
     * Indicates whether request was successful
     */
    private Boolean success;

    /**
     * Human-readable message about the operation result
     * Examples: "User registered successfully", "Email already exists"
     */
    private String message;

    /**
     * Response data - typically contains:
     * - For registration: User ID, username, email, role
     * - For login: Access token, refresh token, user info
     * - For errors: null (excluded from JSON due to @JsonInclude)
     * 
     * Object type allows flexibility - can be UserDTO, TokenDTO, etc.
     */
    private Object data;

    /**
     * ISO timestamp of when response was generated
     * Useful for debugging and synchronization
     */
    private String timestamp;

    // ==========================================
    // Static Builder Methods (convenience)
    // ==========================================
    /**
     * Create a successful response with data
     * Usage: AuthResponse.success("Registration successful", userData)
     */
    public static AuthResponse success(String message, Object data) {
        return AuthResponse.builder()
            .success(true)
            .message(message)
            .data(data)
            .timestamp(java.time.LocalDateTime.now().toString())
            .build();
    }

    /**
     * Create a successful response without data
     * Usage: AuthResponse.success("Login successful")
     */
    public static AuthResponse success(String message) {
        return AuthResponse.builder()
            .success(true)
            .message(message)
            .timestamp(java.time.LocalDateTime.now().toString())
            .build();
    }

    /**
     * Create an error response
     * Usage: AuthResponse.error("Email already exists")
     */
    public static AuthResponse error(String message) {
        return AuthResponse.builder()
            .success(false)
            .message(message)
            .timestamp(java.time.LocalDateTime.now().toString())
            .build();
    }

    /**
     * Create an error response with data (for validation errors)
     * Usage: AuthResponse.error("Validation failed", errorDetails)
     */
    public static AuthResponse error(String message, Object data) {
        return AuthResponse.builder()
            .success(false)
            .message(message)
            .data(data)
            .timestamp(java.time.LocalDateTime.now().toString())
            .build();
    }
}
