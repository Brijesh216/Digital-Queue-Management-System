package com.digitalqueue.application.service;

import com.digitalqueue.api.dto.request.LoginRequest;
import com.digitalqueue.api.dto.request.RegisterRequest;
import com.digitalqueue.api.dto.response.AuthResponse;
import com.digitalqueue.api.dto.response.LoginResponse;
import com.digitalqueue.api.exception.ResourceAlreadyExistsException;
import com.digitalqueue.api.exception.ResourceNotFoundException;
import com.digitalqueue.api.exception.ValidationException;
import com.digitalqueue.constants.AppConstants;
import com.digitalqueue.domain.entity.User;
import com.digitalqueue.domain.model.UserRole;
import com.digitalqueue.infrastructure.jwt.JwtTokenProvider;
import com.digitalqueue.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * AuthService - Handles all authentication-related business logic.
 * 
 * Service Layer Responsibilities:
 * - Validate input
 * - Check business rules (email uniqueness, etc.)
 * - Coordinate with repositories
 * - Encrypt passwords
 * - Return results to controller
 * 
 * @Service: Spring component, instantiated by Spring at startup
 * @Slf4j: Lombok annotation - generates logger field (log object)
 * @Transactional: Wraps method in database transaction
 *   - If exception occurs: rollback database changes
 *   - If success: auto-commit
 */
@Service
@Slf4j
@Transactional
public class AuthService {

    // ==========================================
    // Dependencies (Constructor Injection)
    // ==========================================
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructor - All dependencies injected here.
     * 
     * Constructor Injection Benefits:
     * - Immutable fields (use final)
     * - Clear dependencies (visible at construction)
     * - Easy testing (pass mocks to constructor)
     * - Better than field @Autowired injection
     * 
     * Spring automatically calls this constructor,
     * finding matching beans for UserRepository and PasswordEncoder
     */
    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      AuthenticationManager authenticationManager,
                      JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        log.debug("AuthService initialized with dependencies");
    }

    // ==========================================
    // Public Methods
    // ==========================================

    /**
     * Register a new user.
     * 
     * Flow:
     * 1. Validate input (not null, email format, etc.)
     * 2. Check duplicate email/username
     * 3. Validate password match
     * 4. Encrypt password using BCrypt
     * 5. Create User entity
     * 6. Save to database
     * 7. Return created user info
     * 
     * @param registerRequest: DTO from API controller
     * @return: User data with ID and role
     * @throws ResourceAlreadyExistsException: If email/username exists
     * @throws ValidationException: If validation fails
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Registering new user with email: {}", registerRequest.getEmail());

        // Step 1: Validate input
        validateRegisterRequest(registerRequest);

        // Step 2: Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration failed - Email already exists: {}", registerRequest.getEmail());
            throw new ResourceAlreadyExistsException(AppConstants.EMAIL_ALREADY_EXISTS);
        }

        // Step 3: Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            log.warn("Registration failed - Username already taken: {}", registerRequest.getUsername());
            throw new ResourceAlreadyExistsException(AppConstants.USERNAME_ALREADY_EXISTS);
        }

        // Step 4: Create new User entity
        User user = User.builder()
            .username(registerRequest.getUsername())
            .email(registerRequest.getEmail())
            // Step 5: Encrypt password using BCrypt
            .password(passwordEncoder.encode(registerRequest.getPassword()))
            .firstName(registerRequest.getFirstName())
            .lastName(registerRequest.getLastName())
            .phoneNumber(registerRequest.getPhoneNumber())
            // New users are CUSTOMER by default
            .role(UserRole.CUSTOMER)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // Step 6: Save to database
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Step 7: Return response with user data
        UserRegisterResponse userResponse = UserRegisterResponse.builder()
            .id(savedUser.getId())
            .username(savedUser.getUsername())
            .email(savedUser.getEmail())
            .firstName(savedUser.getFirstName())
            .lastName(savedUser.getLastName())
            .role(savedUser.getRole().toString())
            .build();

        return AuthResponse.success(AppConstants.USER_REGISTERED_SUCCESS, userResponse);
    }

    /**
     * Login user with email/username and password.
     * 
     * Flow:
     * 1. Validate input
     * 2. Authenticate using Spring Security
     * 3. Generate JWT token
     * 4. Return token and user info
     * 
     * @param loginRequest: DTO with email/username and password
     * @return: AuthResponse with JWT token and user info
     * @throws ValidationException: If validation fails
     * @throws BadCredentialsException: If credentials invalid
     */
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for identifier: {}", loginRequest.getIdentifier());

        // Step 1: Validate input
        validateLoginRequest(loginRequest);

        // Step 2: Load user from database
        User user = userRepository.findActiveByEmailOrUsername(loginRequest.getIdentifier())
            .orElseThrow(() -> {
                log.warn("Login failed - User not found: {}", loginRequest.getIdentifier());
                return new ResourceNotFoundException(AppConstants.INVALID_CREDENTIALS);
            });

        // Step 3: Authenticate using Spring Security
        // This verifies password using DaoAuthenticationProvider
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    loginRequest.getPassword()
                )
            );

            // Step 4: Generate JWT token
            String jwtToken = jwtTokenProvider.generateToken(authentication);
            long expiresIn = jwtTokenProvider.getExpirationTimeInSeconds(jwtToken);

            log.info("User logged in successfully: {}", user.getUsername());

            // Step 5: Create response with token and user info
            LoginResponse.UserLoginInfo userInfo = LoginResponse.UserLoginInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().toString())
                .build();

            LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userInfo)
                .build();

            return AuthResponse.success(AppConstants.LOGIN_SUCCESS, loginResponse);

        } catch (Exception ex) {
            log.warn("Login failed - Invalid credentials for user: {}", user.getUsername());
            throw new ValidationException(AppConstants.INVALID_CREDENTIALS);
        }
    }

    public AuthResponse verify(String username) {
        User user = userRepository.findByUsername(username)
            .filter(User::getActive)
            .orElseThrow(() -> new ResourceNotFoundException(AppConstants.INVALID_CREDENTIALS));

        LoginResponse.UserLoginInfo userInfo = LoginResponse.UserLoginInfo.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole().toString())
            .build();

        return AuthResponse.success("Token verified successfully", userInfo);
    }

    // ==========================================
    // Private Validation Methods
    // ==========================================

    /**
     * Validate RegisterRequest data.
     * Checks business rules beyond basic @Valid annotations.
     * 
     * @param request: The registration request
     * @throws ValidationException: If any validation fails
     */
    private void validateRegisterRequest(RegisterRequest request) {
        log.debug("Validating register request");

        // Check if passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            log.warn("Password validation failed - passwords do not match");
            throw new ValidationException("Passwords do not match");
        }

        // Check password length (redundant with @Size, but explicit is good)
        if (request.getPassword().length() < AppConstants.PASSWORD_MIN_LENGTH) {
            throw new ValidationException(AppConstants.PASSWORD_LENGTH_ERROR);
        }

        log.debug("Register request validation passed");
    }

    /**
     * Validate LoginRequest data.
     * 
     * @param request: The login request
     * @throws ValidationException: If any validation fails
     */
    private void validateLoginRequest(LoginRequest request) {
        log.debug("Validating login request");

        if (request.getIdentifier() == null || request.getIdentifier().trim().isEmpty()) {
            throw new ValidationException("Email or username is required");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new ValidationException("Password is required");
        }

        log.debug("Login request validation passed");
    }

    // ==========================================
    // Inner DTO Class
    // ==========================================

    /**
     * Inner DTO for registration response.
     * Only includes non-sensitive user data to return to client.
     * Never return password, timestamps, or sensitive info.
     */
    @lombok.Data
    @lombok.Builder
    public static class UserRegisterResponse {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
    }
}
