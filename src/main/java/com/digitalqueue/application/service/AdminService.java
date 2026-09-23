package com.digitalqueue.application.service;

import com.digitalqueue.api.dto.response.QueueTokenResponse;
import com.digitalqueue.api.exception.ResourceNotFoundException;
import com.digitalqueue.api.exception.ValidationException;
import com.digitalqueue.domain.entity.Queue;
import com.digitalqueue.domain.entity.QueueToken;
import com.digitalqueue.domain.entity.User;
import com.digitalqueue.domain.model.QueueStatus;
import com.digitalqueue.domain.model.TokenStatus;
import com.digitalqueue.infrastructure.repository.QueueRepository;
import com.digitalqueue.infrastructure.repository.QueueTokenRepository;
import com.digitalqueue.infrastructure.repository.UserRepository;
import com.digitalqueue.infrastructure.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * AdminService - Business logic for queue administration and token management.
 * 
 * Responsibilities:
 * 1. Call next waiting token and transition to CALLED/SERVING status
 * 2. Mark tokens as COMPLETED with service end time
 * 3. Skip tokens and mark as ABSENT
 * 4. Pause queues to stop accepting new tokens
 * 5. Resume paused queues
 * 
 * Queue Processing Flow:
 * 1. Customer joins queue → Token created with WAITING status
 * 2. Admin calls next token → Next WAITING token transitions to CALLED
 * 3. Token transitions from CALLED to SERVING (customer is being served)
 * 4. Admin marks token as COMPLETED → Token transitions to COMPLETED with end time
 * 5. Token lifecycle ends → Can query historical data
 * 
 * Alternative Flow (Skip):
 * 1. Customer joins queue → Token created with WAITING status
 * 2. Customer not present → Admin marks as ABSENT (skip)
 * 3. Next token is called automatically
 * 
 * Token Status Transitions:
 * WAITING → CALLED (next token called by admin)
 * CALLED → SERVING (customer called for service)
 * SERVING → COMPLETED (service finished)
 * WAITING → ABSENT (customer didn't show up, skip)
 * 
 * Transactional Behavior:
 * - All write operations use @Transactional for ACID guarantees
 * - Prevents race conditions in concurrent queue operations
 * - Ensures queue state consistency across updates
 * 
 * Dependencies: QueueRepository, QueueTokenRepository, UserRepository
 */
@Slf4j
@Service
public class AdminService {

    private final QueueRepository queueRepository;
    private final QueueTokenRepository queueTokenRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;

    /**
     * Constructor with dependency injection.
     * 
     * @param queueRepository - For queue data access
     * @param queueTokenRepository - For token data access
     * @param userRepository - For user data access (admin/operator validation)
     * @param webSocketService - For real-time WebSocket broadcasts
     */
    public AdminService(
            QueueRepository queueRepository,
            QueueTokenRepository queueTokenRepository,
            UserRepository userRepository,
            WebSocketService webSocketService) {
        this.queueRepository = queueRepository;
        this.queueTokenRepository = queueTokenRepository;
        this.userRepository = userRepository;
        this.webSocketService = webSocketService;
        log.debug("AdminService initialized with dependencies");
    }

    /**
     * Call next waiting token - Transitions token from WAITING to CALLED status.
     * 
     * Flow:
     * 1. Validate queue exists and is ACTIVE
     * 2. Find next WAITING token ordered by token number
     * 3. Transition token to CALLED status
     * 4. Update queue's current token reference
     * 5. Record call time for wait time calculation
     * 
     * @param queueId - Queue identifier
     * @return QueueTokenResponse with called token details
     * @throws ResourceNotFoundException if queue not found
     * @throws ValidationException if queue not active or no waiting tokens
     */
    @Transactional
    public QueueTokenResponse callNextToken(Long queueId) {
        log.debug("Admin calling next token for queue: {}", queueId);

        // Step 1: Validate queue exists
        Queue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue not found with id: " + queueId));

        // Step 2: Validate queue is ACTIVE
        if (queue.getStatus() != QueueStatus.ACTIVE) {
            throw new ValidationException("Queue is not active. Current status: " + queue.getStatus());
        }

        // Step 3: Find next waiting token
        QueueToken nextToken = queueTokenRepository.findNextWaitingToken(queueId)
                .orElseThrow(() -> new ValidationException("No waiting tokens in queue"));

        // Step 4: Transition token to CALLED status
        nextToken.setStatus(TokenStatus.CALLED);
        nextToken.setBookingTime(LocalDateTime.now()); // Update call time
        QueueToken savedToken = queueTokenRepository.save(nextToken);

        // Step 5: Update queue's current token
        queue.setCurrentToken(nextToken.getTokenNumber());
        queue.setWaitingCount(queueTokenRepository.countWaitingTokensByQueueId(queueId));
        queueRepository.save(queue);

        log.info("Token {} called for queue {}. Status: {} → {}", 
            nextToken.getTokenNumber(), queueId, TokenStatus.WAITING, TokenStatus.CALLED);

        // Step 6: Broadcast to WebSocket subscribers
        webSocketService.broadcastTokenCalled(queue, nextToken);

        return mapTokenToResponse(savedToken);
    }

    /**
     * Mark token as COMPLETED - Transitions token from CALLED/SERVING to COMPLETED.
     * 
     * Flow:
     * 1. Validate token exists
     * 2. Verify token is in CALLED or SERVING status (not already completed/cancelled)
     * 3. Transition to COMPLETED status
     * 4. Record service end time for actual service duration calculation
     * 5. Clear queue's current token reference
     * 
     * Token Lifecycle Stages:
     * - WAITING: Token issued but not yet served
     * - CALLED: Token called but not yet being served
     * - SERVING: Token is actively being served
     * - COMPLETED: Service finished, can be archived
     * 
     * @param tokenId - Token identifier
     * @return QueueTokenResponse with completed token details
     * @throws ResourceNotFoundException if token not found
     * @throws ValidationException if token not in CALLED/SERVING status
     */
    @Transactional
    public QueueTokenResponse completeToken(Long tokenId) {
        log.debug("Admin marking token as completed: {}", tokenId);

        // Step 1: Validate token exists
        QueueToken token = queueTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found with id: " + tokenId));

        // Step 2: Verify token is in valid state for completion
        if (token.getStatus() != TokenStatus.CALLED && token.getStatus() != TokenStatus.SERVING) {
            throw new ValidationException(
                "Token cannot be completed. Current status: " + token.getStatus() + 
                ". Token must be in CALLED or SERVING status."
            );
        }

        // Step 3: Record service end time
        LocalDateTime serviceEndTime = LocalDateTime.now();
        token.setServiceEndTime(serviceEndTime);

        // Step 4: Transition to COMPLETED
        token.setStatus(TokenStatus.COMPLETED);
        QueueToken savedToken = queueTokenRepository.save(token);

        // Step 5: Clear queue's current token if it matches
        Queue queue = token.getQueue();
        if (queue.getCurrentToken() != null && queue.getCurrentToken().equals(token.getTokenNumber())) {
            queue.setCurrentToken(null);
            queueRepository.save(queue);
        }

        log.info("Token {} marked as COMPLETED. Service duration: {} minutes",
            token.getTokenNumber(),
            calculateServiceDuration(token.getBookingTime(), serviceEndTime));

        // Step 6: Broadcast to WebSocket subscribers
        webSocketService.broadcastTokenCompleted(queue, token);

        return mapTokenToResponse(savedToken);
    }

    /**
     * Skip token (mark as ABSENT) - Transitions token from WAITING to ABSENT.
     * 
     * Use Cases:
     * 1. Customer didn't show up when called
     * 2. Customer left without completing service
     * 3. No-show scenarios
     * 
     * Flow:
     * 1. Validate token exists
     * 2. Verify token is in WAITING status
     * 3. Mark as ABSENT (skipped)
     * 4. Automatically call next waiting token
     * 
     * @param tokenId - Token identifier
     * @return QueueTokenResponse with skipped token details
     * @throws ResourceNotFoundException if token not found
     * @throws ValidationException if token not in WAITING status
     */
    @Transactional
    public QueueTokenResponse skipToken(Long tokenId) {
        log.debug("Admin skipping token: {}", tokenId);

        // Step 1: Validate token exists
        QueueToken token = queueTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found with id: " + tokenId));

        // Step 2: Verify token is WAITING
        if (token.getStatus() != TokenStatus.WAITING) {
            throw new ValidationException(
                "Token cannot be skipped. Current status: " + token.getStatus() + 
                ". Only WAITING tokens can be skipped."
            );
        }

        // Step 3: Mark as ABSENT
        token.setStatus(TokenStatus.ABSENT);
        token.setServiceEndTime(LocalDateTime.now());
        QueueToken skippedToken = queueTokenRepository.save(token);

        Queue queue = token.getQueue();
        queue.setWaitingCount(queueTokenRepository.countWaitingTokensByQueueId(queue.getId()));
        queueRepository.save(queue);

        log.info("Token {} marked as ABSENT (skipped)", token.getTokenNumber());

        // Step 4: Try to call next waiting token automatically
        QueueToken nextToken = null;
        try {
            nextToken = queueTokenRepository.findNextWaitingToken(queue.getId())
                    .orElse(null);
            
            if (nextToken != null) {
                nextToken.setStatus(TokenStatus.CALLED);
                nextToken.setBookingTime(LocalDateTime.now());
                queueTokenRepository.save(nextToken);
                queue.setCurrentToken(nextToken.getTokenNumber());
                queue.setWaitingCount(queueTokenRepository.countWaitingTokensByQueueId(queue.getId()));
                queueRepository.save(queue);
                log.info("Next token {} automatically called after skip", nextToken.getTokenNumber());
            }
        } catch (Exception e) {
            log.warn("Could not auto-call next token: {}", e.getMessage());
        }

        // Step 5: Broadcast to WebSocket subscribers
        webSocketService.broadcastTokenSkipped(queue, skippedToken, nextToken);

        return mapTokenToResponse(skippedToken);
    }

    /**
     * Pause queue - Prevents new customers from joining and pauses token serving.
     * 
     * Queue Status Transitions:
     * ACTIVE → PAUSED (can be resumed)
     * PAUSED → ACTIVE (resume)
     * ACTIVE → CLOSED (permanent close)
     * 
     * Pause Flow:
     * 1. Validate queue exists
     * 2. Verify queue is in ACTIVE status
     * 3. Transition to PAUSED status
     * 4. No new tokens will be issued
     * 5. Current service can finish but no new customers join
     * 
     * @param queueId - Queue identifier
     * @throws ResourceNotFoundException if queue not found
     * @throws ValidationException if queue not in ACTIVE status
     */
    @Transactional
    public void pauseQueue(Long queueId) {
        log.debug("Admin pausing queue: {}", queueId);

        // Step 1: Validate queue exists
        Queue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue not found with id: " + queueId));

        // Step 2: Verify queue is ACTIVE
        if (queue.getStatus() != QueueStatus.ACTIVE) {
            throw new ValidationException(
                "Queue cannot be paused. Current status: " + queue.getStatus() + 
                ". Only ACTIVE queues can be paused."
            );
        }

        // Step 3: Transition to PAUSED
        queue.setStatus(QueueStatus.PAUSED);
        queueRepository.save(queue);

        log.info("Queue {} paused. Status: {} → {}", queueId, QueueStatus.ACTIVE, QueueStatus.PAUSED);

        // Step 4: Broadcast to WebSocket subscribers
        webSocketService.broadcastQueuePaused(queue);
    }

    /**
     * Resume queue - Re-enables customer joining and token serving.
     * 
     * Resume Flow:
     * 1. Validate queue exists
     * 2. Verify queue is in PAUSED status
     * 3. Transition back to ACTIVE status
     * 4. Customers can join again
     * 5. Next token can be called
     * 
     * @param queueId - Queue identifier
     * @throws ResourceNotFoundException if queue not found
     * @throws ValidationException if queue not in PAUSED status
     */
    @Transactional
    public void resumeQueue(Long queueId) {
        log.debug("Admin resuming queue: {}", queueId);

        // Step 1: Validate queue exists
        Queue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue not found with id: " + queueId));

        // Step 2: Verify queue is PAUSED
        if (queue.getStatus() != QueueStatus.PAUSED) {
            throw new ValidationException(
                "Queue cannot be resumed. Current status: " + queue.getStatus() + 
                ". Only PAUSED queues can be resumed."
            );
        }

        // Step 3: Transition back to ACTIVE
        queue.setStatus(QueueStatus.ACTIVE);
        queueRepository.save(queue);

        log.info("Queue {} resumed. Status: {} → {}", queueId, QueueStatus.PAUSED, QueueStatus.ACTIVE);

        // Step 4: Broadcast to WebSocket subscribers
        webSocketService.broadcastQueueResumed(queue);
    }

    /**
     * Get token details with related queue and user information.
     * 
     * @param tokenId - Token identifier
     * @return QueueTokenResponse with full token details
     * @throws ResourceNotFoundException if token not found
     */
    @Transactional(readOnly = true)
    public QueueTokenResponse getTokenDetails(Long tokenId) {
        QueueToken token = queueTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found with id: " + tokenId));
        return mapTokenToResponse(token);
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    /**
     * Map QueueToken entity to QueueTokenResponse DTO.
     * Includes nested user and queue information.
     * 
     * @param token - Token entity
     * @return QueueTokenResponse DTO
     */
    private QueueTokenResponse mapTokenToResponse(QueueToken token) {
        return QueueTokenResponse.builder()
                .id(token.getId())
                .tokenNumber(token.getTokenNumber())
                .displayToken(token.getDisplayToken())
                .status(token.getStatus().toString())
                .estimatedWaitTime(token.getEstimatedWaitTime())
                .actualWaitTimeMinutes(token.getActualWaitTimeInMinutes())
                .remainingWaitTime(token.getRemainingWaitTime())
                .bookingTime(token.getBookingTime() != null ? token.getBookingTime().toString() : null)
                .serviceStartTime(token.getServiceStartTime() != null ? token.getServiceStartTime().toString() : null)
                .serviceEndTime(token.getServiceEndTime() != null ? token.getServiceEndTime().toString() : null)
                .createdAt(token.getCreatedAt() != null ? token.getCreatedAt().toString() : null)
                .updatedAt(token.getUpdatedAt() != null ? token.getUpdatedAt().toString() : null)
                .user(QueueTokenResponse.TokenUserInfo.builder()
                        .id(token.getUser().getId())
                        .username(token.getUser().getUsername())
                        .email(token.getUser().getEmail())
                        .firstName(token.getUser().getFirstName())
                        .lastName(token.getUser().getLastName())
                        .build())
                .queue(QueueTokenResponse.TokenQueueInfo.builder()
                        .id(token.getQueue().getId())
                        .queueName(token.getQueue().getQueueName())
                        .serviceType(token.getQueue().getServiceType())
                        .queueCode(token.getQueue().getQueueCode())
                        .build())
                .build();
    }

    /**
     * Calculate service duration in minutes.
     * 
     * @param startTime - Service start time
     * @param endTime - Service end time
     * @return Duration in minutes
     */
    private long calculateServiceDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }
}
