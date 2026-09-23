package com.digitalqueue.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RegisterRequest DTO - Data Transfer Object for user registration.
 * 
 * Purpose:
 * - Validates incoming request data from client
 * - Decouples API contract from domain entity
 * - If we change User entity, API stays unchanged
 * 
 * Validation Annotations (from jakarta.validation):
 * @NotBlank: String cannot be null or whitespace
 * @NotEmpty: Collection cannot be empty
 * @Email: Must be valid email format
 * @Size: Collection or string length constraints
 * @Min/@Max: Numeric value constraints
 * 
 * Spring automatically validates this DTO when @Valid annotation is used in Controller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    /**
     * Username for login.
     * Requirements: 3-50 characters
     */
    @NotBlank(message = "Username is required")
    @Size(
        min = 3,
        max = 50,
        message = "Username must be between 3 and 50 characters"
    )
    private String username;

    /**
     * Email address for login and communication.
     * Requirements: Valid email format, unique in database
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(
        max = 100,
        message = "Email cannot exceed 100 characters"
    )
    private String email;

    /**
     * Password for authentication.
     * Requirements: Minimum 8 characters
     * 
     * In production: You might want stronger validation
     * (uppercase, lowercase, numbers, special chars)
     */
    @NotBlank(message = "Password is required")
    @Size(
        min = 8,
        max = 100,
        message = "Password must be between 8 and 100 characters"
    )
    private String password;

    /**
     * Password confirmation.
     * Requirements: Must match password field
     * 
     * Note: Custom validation in AuthService
     * (not available in annotation validations)
     */
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    /**
     * Optional: User's first name
     */
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    /**
     * Optional: User's last name
     */
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    /**
     * Optional: User's phone number
     */
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;
}
