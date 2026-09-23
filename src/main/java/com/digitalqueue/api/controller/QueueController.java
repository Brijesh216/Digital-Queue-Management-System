package com.digitalqueue.api.controller;

import com.digitalqueue.api.dto.request.CreateQueueRequest;
import com.digitalqueue.api.dto.request.JoinQueueRequest;
import com.digitalqueue.api.dto.response.AuthResponse;
import com.digitalqueue.api.dto.response.QueueResponse;
import com.digitalqueue.api.dto.response.QueueTokenResponse;
import com.digitalqueue.api.exception.ResourceNotFoundException;
import com.digitalqueue.application.service.QueueService;
import com.digitalqueue.constants.AppConstants;
import com.digitalqueue.infrastructure.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * QueueController - REST endpoints for queue management.
 * 
 * Endpoints:
 * - POST /api/v1/queues (create queue - ADMIN/OPERATOR)
 * - GET /api/v1/queues (list active queues - PUBLIC)
 * - GET /api/v1/queues/{queueId} (get queue details - PUBLIC)
 * - GET /api/v1/queues/service/{serviceType} (filter by service - PUBLIC)
 * - POST /api/v1/queues/{queueId}/join (join queue - CUSTOMER)
 * - GET /api/v1/queues/{queueId}/tokens (list all tokens - OPERATOR)
 * - GET /api/v1/queues/{queueId}/waiting-tokens (list waiting - CUSTOMER)
 * - GET /api/v1/queues/{queueId}/active-token (current serving - PUBLIC)
 * - GET /api/v1/queues/{queueId}/my-token (user's token - CUSTOMER)
 * - POST /api/v1/queues/{queueId}/next-token (call next - OPERATOR)
 * 
 * @RestController: HTTP request handler
 * @RequestMapping: Base path "/api/v1/queues"
 * @Slf4j: Logger
 */
@RestController
@RequestMapping(AppConstants.QUEUE_ENDPOINT)
@Slf4j
public class QueueController {

    private final QueueService queueService;
    private final UserRepository userRepository;

    public QueueController(QueueService queueService, UserRepository userRepository) {
        this.queueService = queueService;
        this.userRepository = userRepository;
        log.debug("QueueController initialized");
    }

    // ==========================================
    // Queue Management Endpoints
    // ==========================================

    /**
     * Create a new queue.
     * 
     * Accessible by: ADMIN, OPERATOR
     * 
     * HTTP: POST /api/v1/queues
     * 
     * Example Request:
     * {
     *   "queueName": "Counter 1",
     *   "serviceType": "BANKING",
     *   "queueCode": "BANK_001",
     *   "description": "General Banking Services",
     *   "averageServiceTime": 5
     * }
     * 
     * Response (201):
     * {
     *   "success": true,
     *   "message": "Queue created successfully",
     *   "data": { ... QueueResponse ... },
     *   "timestamp": "..."
     * }
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<AuthResponse> createQueue(
            @Valid @RequestBody CreateQueueRequest request) {
        log.info("Create queue request: {}", request.getQueueCode());

        QueueResponse queueResponse = queueService.createQueue(request);
        AuthResponse response = AuthResponse.success("Queue created successfully", queueResponse);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    /**
     * Get all active queues.
     * 
     * Accessible by: PUBLIC (no auth required)
     * 
     * HTTP: GET /api/v1/queues
     * 
     * Use Cases:
     * - Customer browsing available queues
     * - Display all service counters
     * 
     * Response (200):
     * {
     *   "success": true,
     *   "message": "...",
     *   "data": [ { QueueResponse }, { QueueResponse }, ... ],
     *   "timestamp": "..."
     * }
     */
    @GetMapping
    public ResponseEntity<AuthResponse> getAllQueues() {
        log.debug("Fetching all active queues");

        List<QueueResponse> queues = queueService.getAllActiveQueues();
        AuthResponse response = AuthResponse.success("Queues retrieved successfully", queues);

        return ResponseEntity.ok(response);
    }

    /**
     * Get queue by ID.
     * 
     * Accessible by: PUBLIC
     * 
     * HTTP: GET /api/v1/queues/{queueId}
     * 
     * @param queueId: ID of queue
     */
    @GetMapping("/{queueId}")
    public ResponseEntity<AuthResponse> getQueueById(@PathVariable Long queueId) {
        log.debug("Fetching queue: {}", queueId);

        QueueResponse queueResponse = queueService.getQueueById(queueId);
        AuthResponse response = AuthResponse.success("Queue retrieved successfully", queueResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Get queues by service type.
     * 
     * Accessible by: PUBLIC
     * 
     * HTTP: GET /api/v1/queues/service/{serviceType}
     * 
     * Example: GET /api/v1/queues/service/BANKING
     * 
     * @param serviceType: Type of service (BANKING, MEDICAL, etc.)
     */
    @GetMapping("/service/{serviceType}")
    public ResponseEntity<AuthResponse> getQueuesByServiceType(
            @PathVariable String serviceType) {
        log.debug("Fetching queues for service type: {}", serviceType);

        List<QueueResponse> queues = queueService.getQueuesByServiceType(serviceType);
        AuthResponse response = AuthResponse.success("Queues retrieved successfully", queues);

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Token Management Endpoints
    // ==========================================

    /**
     * Join a queue (get a token).
     * 
     * Accessible by: CUSTOMER (authenticated users)
     * 
     * HTTP: POST /api/v1/queues/{queueId}/join
     * 
     * Business Logic:
     * 1. Check queue exists and is ACTIVE
     * 2. Check user doesn't have active token in queue
     * 3. Generate next token number
     * 4. Create QueueToken entity
     * 5. Return token details
     * 
     * Example Request: {} (empty body)
     * 
     * Response (201):
     * {
     *   "success": true,
     *   "message": "Joined queue successfully",
     *   "data": { ... QueueTokenResponse ... },
     *   "timestamp": "..."
     * }
     * 
     * @param queueId: Queue to join
     * @param authentication: Spring Security authentication (contains user ID)
     */
    @PostMapping("/{queueId}/join")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AuthResponse> joinQueue(
            @PathVariable Long queueId,
            @Valid @RequestBody JoinQueueRequest request,
            Authentication authentication) {
        log.info("User {} joining queue {}", authentication.getName(), queueId);

        // Extract user ID from authentication
        // In real implementation, you'd have a method to get user ID from auth
        // For now, we'll need to get it from the authenticated user
        Long userId = getUserIdFromAuthentication(authentication);

        QueueTokenResponse tokenResponse = queueService.joinQueue(queueId, userId);
        AuthResponse response = AuthResponse.success("Joined queue successfully", tokenResponse);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    /**
     * Get all tokens in a queue.
     * 
     * Accessible by: OPERATOR (for queue management)
     * 
     * HTTP: GET /api/v1/queues/{queueId}/tokens
     * 
     * Shows all tokens (waiting, serving, completed) in a queue.
     * Used by operator display system.
     * 
     * @param queueId: Queue ID
     */
    @GetMapping("/{queueId}/tokens")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> getQueueTokens(@PathVariable Long queueId) {
        log.debug("Fetching tokens for queue: {}", queueId);

        List<QueueTokenResponse> tokens = queueService.getQueueTokens(queueId);
        AuthResponse response = AuthResponse.success("Tokens retrieved successfully", tokens);

        return ResponseEntity.ok(response);
    }

    /**
     * Get waiting tokens in a queue.
     * 
     * Accessible by: PUBLIC
     * 
     * HTTP: GET /api/v1/queues/{queueId}/waiting-tokens
     * 
     * Shows only waiting tokens in order.
     * Used for customer display: "Position in queue: 5 of 12"
     * 
     * @param queueId: Queue ID
     */
    @GetMapping("/{queueId}/waiting-tokens")
    public ResponseEntity<AuthResponse> getWaitingTokens(@PathVariable Long queueId) {
        log.debug("Fetching waiting tokens for queue: {}", queueId);

        List<QueueTokenResponse> waitingTokens = queueService.getWaitingTokens(queueId);
        AuthResponse response = AuthResponse.success("Waiting tokens retrieved successfully", waitingTokens);

        return ResponseEntity.ok(response);
    }

    /**
     * Get currently serving token.
     * 
     * Accessible by: PUBLIC
     * 
     * HTTP: GET /api/v1/queues/{queueId}/active-token
     * 
     * Shows which token is currently being served.
     * Used for display: "Now serving: A005"
     * 
     * @param queueId: Queue ID
     */
    @GetMapping("/{queueId}/active-token")
    public ResponseEntity<AuthResponse> getActiveToken(@PathVariable Long queueId) {
        log.debug("Fetching active token for queue: {}", queueId);

        QueueTokenResponse activeToken = queueService.getActiveToken(queueId);
        AuthResponse response = AuthResponse.success("Active token retrieved successfully", activeToken);

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's token in a queue.
     * 
     * Accessible by: CUSTOMER
     * 
     * HTTP: GET /api/v1/queues/{queueId}/my-token
     * 
     * Shows the authenticated user's token in a specific queue.
     * Used for customer to check their position.
     * 
     * @param queueId: Queue ID
     * @param authentication: Spring Security authentication
     */
    @GetMapping("/{queueId}/my-token")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AuthResponse> getMyToken(
            @PathVariable Long queueId,
            Authentication authentication) {
        log.debug("Fetching token for user {} in queue {}", authentication.getName(), queueId);

        Long userId = getUserIdFromAuthentication(authentication);
        QueueTokenResponse userToken = queueService.getUserToken(queueId, userId);
        AuthResponse response = AuthResponse.success("Your token retrieved successfully", userToken);

        return ResponseEntity.ok(response);
    }

    /**
     * Call next token (operator action).
     * 
     * Accessible by: OPERATOR
     * 
     * HTTP: POST /api/v1/queues/{queueId}/next-token
     * 
     * Operator clicks this when ready to serve next customer.
     * - Updates queue's currentToken
     * - Changes token status from WAITING to CALLED
     * - Decrements waitingCount
     * 
     * Response (200):
     * {
     *   "success": true,
     *   "message": "Next token called",
     *   "data": { ... QueueTokenResponse ... next token details ... },
     *   "timestamp": "..."
     * }
     * 
     * @param queueId: Queue ID
     */
    @PostMapping("/{queueId}/next-token")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> callNextToken(@PathVariable Long queueId) {
        log.info("Calling next token for queue: {}", queueId);

        QueueTokenResponse nextToken = queueService.callNextToken(queueId);
        AuthResponse response = AuthResponse.success("Next token called successfully", nextToken);

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    /**
     * Extract user ID from Spring Security Authentication.
     * 
     * Method:
     * 1. Get username from authentication.getName()
     * 2. Load user from database by username
     * 3. Return user ID
     * 
     * @param authentication: Spring Security Authentication object
     * @return: User ID
     * @throws ResourceNotFoundException: If user not found (shouldn't happen if auth is valid)
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.warn("User not found for username: {}", username);
                return new ResourceNotFoundException("User not found");
            })
            .getId();
    }
}
