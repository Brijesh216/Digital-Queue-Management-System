package com.digitalqueue.api.controller;

import com.digitalqueue.api.dto.response.AuthResponse;
import com.digitalqueue.api.dto.response.QueueTokenResponse;
import com.digitalqueue.application.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AdminController - REST API endpoints for queue administration and token management.
 * 
 * Base Path: /api/v1/admin
 * 
 * Authorization:
 * - All endpoints require ADMIN or OPERATOR role
 * - @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')") on all methods
 * - Security enforced at method level via Spring Security
 * 
 * Endpoints:
 * 1. POST /api/v1/admin/queues/{queueId}/next - Call next waiting token
 * 2. POST /api/v1/admin/tokens/{tokenId}/complete - Mark token as completed
 * 3. POST /api/v1/admin/tokens/{tokenId}/skip - Skip token (mark as absent)
 * 4. POST /api/v1/admin/queues/{queueId}/pause - Pause queue
 * 5. POST /api/v1/admin/queues/{queueId}/resume - Resume paused queue
 * 
 * Response Format:
 * All endpoints return wrapped responses:
 * {
 *   "success": true/false,
 *   "message": "Operation description",
 *   "data": {...},
 *   "timestamp": "2026-05-26T..."
 * }
 * 
 * Error Handling:
 * - GlobalExceptionHandler catches all exceptions
 * - Returns consistent error responses with HTTP status codes
 * - Validation errors: 400 BAD_REQUEST
 * - Resource not found: 404 NOT_FOUND
 * - Server errors: 500 INTERNAL_SERVER_ERROR
 * 
 * Dependencies: AdminService (injected)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    /**
     * Constructor with dependency injection.
     * 
     * @param adminService - Service layer for admin operations
     */
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
        log.debug("AdminController initialized");
    }

    /**
     * Call next waiting token - Transitions next WAITING token to CALLED status.
     * 
     * Endpoint: POST /api/v1/admin/queues/{queueId}/next
     * Authorization: ADMIN or OPERATOR only
     * 
     * Flow:
     * 1. Admin sends POST request with queue ID
     * 2. System finds next WAITING token
     * 3. Token transitions from WAITING → CALLED
     * 4. Queue's currentToken is updated
     * 5. Response includes token details and new status
     * 
     * Use Case: Admin calls next customer for service
     * 
     * @param queueId - Queue identifier
     * @return ResponseEntity with wrapped QueueTokenResponse
     * - Status: 200 OK on success
     * - Status: 404 if queue not found
     * - Status: 400 if queue not active or no waiting tokens
     */
    @PostMapping("/queues/{queueId}/next")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<AuthResponse> callNextToken(
            @PathVariable Long queueId) {
        
        log.debug("Admin endpoint: Call next token for queue {}", queueId);
        
        QueueTokenResponse token = adminService.callNextToken(queueId);
        
        AuthResponse response = AuthResponse.success(
            "Next token called successfully",
            token
        );
        
        log.info("Next token called for queue {}. Token: {}", queueId, token.getTokenNumber());
        return ResponseEntity.ok(response);
    }

    /**
     * Mark token as COMPLETED - Transitions token from CALLED/SERVING to COMPLETED.
     * 
     * Endpoint: POST /api/v1/admin/tokens/{tokenId}/complete
     * Authorization: ADMIN or OPERATOR only
     * 
     * Flow:
     * 1. Admin sends POST request with token ID
     * 2. System validates token exists and is in valid status
     * 3. Token transitions from CALLED/SERVING → COMPLETED
     * 4. Service end time is recorded
     * 5. Queue's currentToken is cleared
     * 6. Response includes token with completion details
     * 
     * Token Lifecycle:
     * WAITING → CALLED (callNextToken)
     * CALLED/SERVING → COMPLETED (completeToken) ← This endpoint
     * 
     * Use Case: Admin marks service as finished for customer
     * 
     * @param tokenId - Token identifier
     * @return ResponseEntity with wrapped QueueTokenResponse
     * - Status: 200 OK on success
     * - Status: 404 if token not found
     * - Status: 400 if token not in CALLED/SERVING status
     */
    @PostMapping("/tokens/{tokenId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<AuthResponse> completeToken(
            @PathVariable Long tokenId) {
        
        log.debug("Admin endpoint: Complete token {}", tokenId);
        
        QueueTokenResponse token = adminService.completeToken(tokenId);
        
        AuthResponse response = AuthResponse.success(
            "Token marked as completed successfully",
            token
        );
        
        log.info("Token {} marked as COMPLETED", tokenId);
        return ResponseEntity.ok(response);
    }

    /**
     * Skip token (mark as ABSENT) - Transitions WAITING token to ABSENT.
     * 
     * Endpoint: POST /api/v1/admin/tokens/{tokenId}/skip
     * Authorization: ADMIN or OPERATOR only
     * 
     * Flow:
     * 1. Admin sends POST request with token ID
     * 2. System validates token is in WAITING status
     * 3. Token transitions from WAITING → ABSENT
     * 4. Service end time is recorded
     * 5. Response includes skipped token details
     * 
     * Use Cases:
     * - Customer didn't show up when called
     * - Customer left without service
     * - No-show processing
     * 
     * Token Status Flow:
     * WAITING → ABSENT (skipToken) ← This endpoint
     * (Next system call will call next waiting token automatically)
     * 
     * @param tokenId - Token identifier
     * @return ResponseEntity with wrapped QueueTokenResponse
     * - Status: 200 OK on success
     * - Status: 404 if token not found
     * - Status: 400 if token not in WAITING status
     */
    @PostMapping("/tokens/{tokenId}/skip")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<AuthResponse> skipToken(
            @PathVariable Long tokenId) {
        
        log.debug("Admin endpoint: Skip token {}", tokenId);
        
        QueueTokenResponse token = adminService.skipToken(tokenId);
        
        AuthResponse response = AuthResponse.success(
            "Token skipped successfully (marked as absent)",
            token
        );
        
        log.info("Token {} marked as ABSENT (skipped)", tokenId);
        return ResponseEntity.ok(response);
    }

    /**
     * Pause queue - Transitions queue from ACTIVE to PAUSED status.
     * 
     * Endpoint: POST /api/v1/admin/queues/{queueId}/pause
     * Authorization: ADMIN or OPERATOR only
     * 
     * Flow:
     * 1. Admin sends POST request with queue ID
     * 2. System validates queue is in ACTIVE status
     * 3. Queue transitions from ACTIVE → PAUSED
     * 4. No new tokens will be issued
     * 5. Service can finish but new customers cannot join
     * 6. Response confirms pause operation
     * 
     * Queue Status Transitions:
     * ACTIVE → PAUSED (pauseQueue) ← This endpoint
     * PAUSED → ACTIVE (resumeQueue)
     * ACTIVE/PAUSED → CLOSED (permanent close)
     * 
     * Use Cases:
     * - Lunch break or maintenance
     * - Service interruption
     * - Administrative pause
     * 
     * @param queueId - Queue identifier
     * @return ResponseEntity with success message
     * - Status: 200 OK on success
     * - Status: 404 if queue not found
     * - Status: 400 if queue not in ACTIVE status
     */
    @PostMapping("/queues/{queueId}/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<AuthResponse> pauseQueue(
            @PathVariable Long queueId) {
        
        log.debug("Admin endpoint: Pause queue {}", queueId);
        
        adminService.pauseQueue(queueId);
        
        AuthResponse response = AuthResponse.success(
            "Queue paused successfully. New customers cannot join."
        );
        
        log.info("Queue {} paused by admin", queueId);
        return ResponseEntity.ok(response);
    }

    /**
     * Resume queue - Transitions queue from PAUSED back to ACTIVE status.
     * 
     * Endpoint: POST /api/v1/admin/queues/{queueId}/resume
     * Authorization: ADMIN or OPERATOR only
     * 
     * Flow:
     * 1. Admin sends POST request with queue ID
     * 2. System validates queue is in PAUSED status
     * 3. Queue transitions from PAUSED → ACTIVE
     * 4. New tokens can be issued again
     * 5. Customers can join the queue
     * 6. Service can resume
     * 7. Response confirms resume operation
     * 
     * Queue Status Transitions:
     * ACTIVE → PAUSED (pauseQueue)
     * PAUSED → ACTIVE (resumeQueue) ← This endpoint
     * ACTIVE/PAUSED → CLOSED (permanent close)
     * 
     * Use Cases:
     * - Resuming after break
     * - Maintenance complete
     * - Re-opening service
     * 
     * @param queueId - Queue identifier
     * @return ResponseEntity with success message
     * - Status: 200 OK on success
     * - Status: 404 if queue not found
     * - Status: 400 if queue not in PAUSED status
     */
    @PostMapping("/queues/{queueId}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<AuthResponse> resumeQueue(
            @PathVariable Long queueId) {
        
        log.debug("Admin endpoint: Resume queue {}", queueId);
        
        adminService.resumeQueue(queueId);
        
        AuthResponse response = AuthResponse.success(
            "Queue resumed successfully. Customers can join again."
        );
        
        log.info("Queue {} resumed by admin", queueId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get token details - Retrieve full information about a token.
     * 
     * Endpoint: GET /api/v1/admin/tokens/{tokenId}
     * Authorization: ADMIN or OPERATOR only
     * 
     * Response includes:
     * - Token ID and number
     * - Token status
     * - Service times (booking, start, end)
     * - Wait time calculations
     * - User information (who has the token)
     * - Queue information (which queue)
     * 
     * Use Cases:
     * - Check token status
     * - View service duration
     * - Monitor queue progress
     * 
     * @param tokenId - Token identifier
     * @return ResponseEntity with wrapped QueueTokenResponse
     * - Status: 200 OK on success
     * - Status: 404 if token not found
     */
    @GetMapping("/tokens/{tokenId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<AuthResponse> getTokenDetails(
            @PathVariable Long tokenId) {
        
        log.debug("Admin endpoint: Get token details {}", tokenId);
        
        QueueTokenResponse token = adminService.getTokenDetails(tokenId);
        
        AuthResponse response = AuthResponse.success(
            "Token details retrieved successfully",
            token
        );
        
        return ResponseEntity.ok(response);
    }
}
