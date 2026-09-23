package com.digitalqueue.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LoginRequest DTO - Data Transfer Object for user login.
 * 
 * Two ways to login:
 * 1. Using email
 * 2. Using username
 * 
 * This DTO accepts both - controller will handle either
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    /**
     * Email or Username for authentication.
     * Users can login with either email or username
     */
    @NotBlank(message = "Email or username is required")
    private String identifier;

    /**
     * Password for authentication.
     * Must match the encrypted password in database
     */
    @NotBlank(message = "Password is required")
    private String password;
}
