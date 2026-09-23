package com.digitalqueue.api.controller;

import com.digitalqueue.api.dto.request.LoginRequest;
import com.digitalqueue.api.dto.request.RegisterRequest;
import com.digitalqueue.api.dto.response.AuthResponse;
import com.digitalqueue.application.service.AuthService;
import com.digitalqueue.constants.AppConstants;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController - REST endpoints for authentication.
 * 
 * @RestController: Combines @Controller + @ResponseBody
 *   - Marks class as Spring component handling HTTP requests
 *   - Returns JSON responses automatically (via Jackson)
 * 
 * @RequestMapping: Base path for all endpoints in this controller
 *   - "/api/v1/auth" means all methods start with this path
 * 
 * @Slf4j: Generates logger field
 */
@RestController
@RequestMapping(AppConstants.AUTH_ENDPOINT)
@Slf4j
public class AuthController {

    // ==========================================
    // Dependencies (Constructor Injection)
    // ==========================================
    private final AuthService authService;

    /**
     * Constructor Injection.
     * Spring finds AuthService bean and injects it.
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
        log.debug("AuthController initialized");
    }

    // ==========================================
    // Public Endpoints
    // ==========================================

    /**
     * User Registration Endpoint.
     * 
     * HTTP Method: POST
     * Endpoint: POST /api/v1/auth/register
     * Content-Type: application/json
     * 
     * Request Body:
     * {
     *   "username": "john_doe",
     *   "email": "john@example.com",
     *   "password": "SecurePass123!",
     *   "confirmPassword": "SecurePass123!",
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "phoneNumber": "9876543210"
     * }
     * 
     * Response (Success - 201):
     * {
     *   "success": true,
     *   "message": "User registered successfully",
     *   "data": {
     *     "id": 1,
     *     "username": "john_doe",
     *     "email": "john@example.com",
     *     "firstName": "John",
     *     "lastName": "Doe",
     *     "role": "CUSTOMER"
     *   },
     *   "timestamp": "2024-05-26T10:30:00"
     * }
     * 
     * Response (Error - 409 Conflict):
     * {
     *   "success": false,
     *   "message": "Email already registered",
     *   "timestamp": "2024-05-26T10:30:00"
     * }
     * 
     * @PostMapping: Handles POST requests to "/register"
     * @Valid: Triggers validation of RegisterRequest DTO
     *   - Checks @NotBlank, @Email, @Size annotations
     *   - If validation fails: throws MethodArgumentNotValidException
     *   - GlobalExceptionHandler catches and formats response
     * @RequestBody: Deserializes JSON request body to RegisterRequest object
     * 
     * @param registerRequest: DTO with user registration data
     * @return: ResponseEntity<AuthResponse>
     *   - ResponseEntity allows setting HTTP status code
     *   - 201 CREATED: New resource created successfully
     *   - 400 BAD_REQUEST: Validation errors (handled by exception handler)
     *   - 409 CONFLICT: Email/username already exists (handled by exception handler)
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Register request received for email: {}", registerRequest.getEmail());

        // Call service to handle business logic
        AuthResponse response = authService.register(registerRequest);

        // Return 201 CREATED status (new resource created)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<AuthResponse> verify(Authentication authentication) {
        return ResponseEntity.ok(authService.verify(authentication.getName()));
    }

    /**
     * User Login Endpoint.
     * 
     * HTTP Method: POST
     * Endpoint: POST /api/v1/auth/login
     * Content-Type: application/json
     * 
     * Request Body:
     * {
     *   "identifier": "john@example.com",  // Can be email or username
     *   "password": "SecurePass123!"
     * }
     * 
     * Response (Success - 200):
     * {
     *   "success": true,
     *   "message": "Login successful",
     *   "data": {
     *     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *     "tokenType": "Bearer",
     *     "expiresIn": 86400,
     *     "user": {
     *       "id": 1,
     *       "username": "john_doe",
     *       "email": "john@example.com",
     *       "firstName": "John",
     *       "lastName": "Doe",
     *       "role": "CUSTOMER"
     *     }
     *   },
     *   "timestamp": "2024-05-26T10:30:00"
     * }
     * 
     * Response (Error - 400):
     * {
     *   "success": false,
     *   "message": "Invalid email/username or password",
     *   "timestamp": "2024-05-26T10:30:00"
     * }
     * 
     * How to use the token:
     * 1. Extract accessToken from response
     * 2. For all protected requests, add header:
     *    Authorization: Bearer <accessToken>
     * 3. Server validates token signature and expiration
     * 4. If valid: request processed
     * 5. If invalid/expired: 401 Unauthorized returned
     * 
     * @param loginRequest: DTO with email/username and password
     * @return: ResponseEntity<AuthResponse> with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request received for identifier: {}", loginRequest.getIdentifier());

        // Call service to handle authentication
        AuthResponse response = authService.login(loginRequest);

        // Return 200 OK with token
        return ResponseEntity
            .ok(response);
    }

    /**
     * Health Check Endpoint (for testing).
     * 
     * HTTP Method: GET
     * Endpoint: GET /api/v1/auth/health
     * 
     * Response:
     * {
     *   "success": true,
     *   "message": "Auth service is running",
     *   "timestamp": "2024-05-26T10:30:00"
     * }
     * 
     * Usage: Test if API is accessible before registration
     */
    @GetMapping("/health")
    public ResponseEntity<AuthResponse> health() {
        log.debug("Health check requested");
        return ResponseEntity.ok(
            AuthResponse.success("Auth service is running")
        );
    }
}
