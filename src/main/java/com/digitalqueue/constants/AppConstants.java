package com.digitalqueue.constants;

/**
 * Application-wide constants for validation rules and messages.
 * 
 * Centralized constants prevent magic strings scattered throughout code.
 * Changes here automatically apply everywhere they're referenced.
 */
public class AppConstants {

    // ==========================================
    // Validation Rules
    // ==========================================
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 50;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 100;
    public static final int EMAIL_MAX_LENGTH = 100;

    // ==========================================
    // Success Messages
    // ==========================================
    public static final String USER_REGISTERED_SUCCESS = "User registered successfully";
    public static final String LOGIN_SUCCESS = "Login successful";
    public static final String OPERATION_SUCCESS = "Operation completed successfully";

    // ==========================================
    // Error Messages - Authentication
    // ==========================================
    public static final String EMAIL_ALREADY_EXISTS = "Email already registered";
    public static final String USERNAME_ALREADY_EXISTS = "Username already taken";
    public static final String INVALID_CREDENTIALS = "Invalid email/username or password";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String USER_ACCOUNT_DISABLED = "User account is disabled";

    // ==========================================
    // Error Messages - Validation
    // ==========================================
    public static final String INVALID_EMAIL_FORMAT = "Invalid email format";
    public static final String USERNAME_REQUIRED = "Username is required";
    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String PASSWORD_REQUIRED = "Password is required";
    public static final String USERNAME_LENGTH_ERROR = "Username must be between " + USERNAME_MIN_LENGTH + " and " + USERNAME_MAX_LENGTH + " characters";
    public static final String PASSWORD_LENGTH_ERROR = "Password must be at least " + PASSWORD_MIN_LENGTH + " characters";
    public static final String PASSWORD_STRENGTH_ERROR = "Password must contain uppercase, lowercase, number, and special character";

    // ==========================================
    // Error Messages - General
    // ==========================================
    public static final String INTERNAL_SERVER_ERROR = "An internal server error occurred";
    public static final String UNAUTHORIZED_ACCESS = "Unauthorized access";
    public static final String RESOURCE_NOT_FOUND = "Requested resource not found";

    // ==========================================
    // API Endpoints
    // ==========================================
    public static final String API_PREFIX = "/api/v1";
    public static final String AUTH_ENDPOINT = API_PREFIX + "/auth";
    public static final String QUEUE_ENDPOINT = API_PREFIX + "/queues";
    public static final String ADMIN_ENDPOINT = API_PREFIX + "/admin";

    // ==========================================
    // HTTP Status Codes
    // ==========================================
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_INTERNAL_ERROR = 500;
}
