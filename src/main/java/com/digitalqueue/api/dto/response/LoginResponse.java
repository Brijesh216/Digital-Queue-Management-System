package com.digitalqueue.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LoginResponse DTO - Response data after successful login.
 * 
 * Contains:
 * - JWT Access Token (for API requests)
 * - User information (id, username, email, role)
 * - Token type (Bearer)
 * - Expiration time (so frontend knows when to refresh)
 * 
 * Example Response:
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 86400,
 *   "user": {
 *     "id": 1,
 *     "username": "john_doe",
 *     "email": "john@example.com",
 *     "role": "CUSTOMER"
 *   }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    /**
     * JWT Access token to use in API requests.
     * Include in Authorization header: "Bearer <accessToken>"
     */
    private String accessToken;

    /**
     * Token type - always "Bearer" for JWT
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Token expiration time in seconds.
     * Used by frontend to know when to request new token
     */
    private Long expiresIn;

    /**
     * User information (non-sensitive data only)
     */
    private UserLoginInfo user;

    // ==========================================
    // Inner Class - User Info
    // ==========================================

    /**
     * User information to return after login.
     * Never return password or sensitive fields
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserLoginInfo {

        private Long id;

        private String username;

        private String email;

        private String firstName;

        private String lastName;

        private String role;
    }
}
