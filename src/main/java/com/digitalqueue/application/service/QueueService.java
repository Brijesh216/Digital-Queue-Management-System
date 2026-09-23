package com.digitalqueue.application.service;

import com.digitalqueue.api.dto.request.CreateQueueRequest;
import com.digitalqueue.api.dto.response.QueueResponse;
import com.digitalqueue.api.dto.response.QueueTokenResponse;
import com.digitalqueue.api.exception.ResourceAlreadyExistsException;
import com.digitalqueue.api.exception.ResourceNotFoundException;
import com.digitalqueue.api.exception.ValidationException;
import com.digitalqueue.constants.AppConstants;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * QueueService - Business logic for queue management.
 * 
 * Responsibilities:
 * - Create queues
 * - Join queues (issue tokens)
 * - Call next token
 * - Get queue status
 * - Manage token lifecycle
 * 
 * @Service: Spring component
 * @Slf4j: Logger
 * @Transactional: DB transaction management
 */
@Service
@Slf4j
@Transactional
public class QueueService {

    // ==========================================
    // Dependencies
    // ==========================================
    private final QueueRepository queueRepository;
    private final QueueTokenRepository queueTokenRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public QueueService(QueueRepository queueRepository,
                       QueueTokenRepository queueTokenRepository,
                       UserRepository userRepository,
                       WebSocketService webSocketService) {
        this.queueRepository = queueRepository;
        this.queueTokenRepository = queueTokenRepository;
        this.userRepository = userRepository;
        this.webSocketService = webSocketService;
    }

    // ==========================================
    // Queue Management
    // ==========================================

    /**
     * Create a new queue.
     * 
     * Only ADMIN and OPERATOR can create queues.
     * Queue code must be unique.
     * 
     * @param request: CreateQueueRequest
     * @return: QueueResponse
     * @throws ResourceAlreadyExistsException: If queue code exists
     */
    public QueueResponse createQueue(CreateQueueRequest request) {
        log.info("Creating new queue: {}", request.getQueueCode());

        // Check if queue code already exists
        if (queueRepository.existsByQueueCode(request.getQueueCode())) {
            log.warn("Queue creation failed - Code already exists: {}", request.getQueueCode());
            throw new ResourceAlreadyExistsException("Queue code already exists");
        }

        // Create queue entity
        Queue queue = Queue.builder()
            .queueName(request.getQueueName())
            .serviceType(request.getServiceType())
            .queueCode(request.getQueueCode())
            .description(request.getDescription())
            .status(QueueStatus.ACTIVE)
            .currentToken(null)
            .lastTokenNumber(0)
            .waitingCount(0)
            .averageServiceTime(request.getAverageServiceTime())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        Queue savedQueue = queueRepository.save(queue);
        log.info("Queue created successfully: {}", savedQueue.getId());

        return mapQueueToResponse(savedQueue);
    }

    /**
     * Get queue by ID.
     * 
     * @param queueId: Queue ID
     * @return: QueueResponse
     * @throws ResourceNotFoundException: If queue not found
     */
    public QueueResponse getQueueById(Long queueId) {
        log.debug("Fetching queue: {}", queueId);

        Queue queue = queueRepository.findById(queueId)
            .orElseThrow(() -> {
                log.warn("Queue not found: {}", queueId);
                return new ResourceNotFoundException("Queue not found");
            });

        return mapQueueToResponse(queue);
    }

    /**
     * Get all active queues.
     * 
     * @return: List of QueueResponse
     */
    public List<QueueResponse> getAllActiveQueues() {
        log.debug("Fetching all active queues");

        return queueRepository.findByStatus(QueueStatus.ACTIVE)
            .stream()
            .map(this::mapQueueToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get all queues for a service type.
     * 
     * @param serviceType: Type of service
     * @return: List of QueueResponse
     */
    public List<QueueResponse> getQueuesByServiceType(String serviceType) {
        log.debug("Fetching queues for service type: {}", serviceType);

        return queueRepository.findActiveQueuesByServiceType(serviceType, QueueStatus.ACTIVE)
            .stream()
            .map(this::mapQueueToResponse)
            .collect(Collectors.toList());
    }

    // ==========================================
    // Token Management
    // ==========================================

    /**
     * Join a queue (get a token).
     * 
     * Business Rules:
     * 1. Queue must exist and be ACTIVE
     * 2. User cannot have active token in same queue
     * 3. Generate next token number
     * 4. Create QueueToken entity
     * 5. Update queue waiting count
     * 
     * @param queueId: Queue to join
     * @param userId: User joining queue
     * @return: QueueTokenResponse (the new token)
     * @throws ResourceNotFoundException: If queue not found
     * @throws ValidationException: If validation fails
     */
    public QueueTokenResponse joinQueue(Long queueId, Long userId) {
        log.info("User {} joining queue {}", userId, queueId);

        // Step 1: Get queue
        Queue queue = queueRepository.findById(queueId)
            .orElseThrow(() -> new ResourceNotFoundException("Queue not found"));

        // Step 2: Check if queue is active
        if (!queue.isActive()) {
            log.warn("Cannot join non-active queue: {}", queueId);
            throw new ValidationException("Queue is not accepting new members");
        }

        // Step 3: Get user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Step 4: Check if user already has active token in this queue
        if (queueTokenRepository.existsActiveTokenForUserInQueue(userId, queueId)) {
            log.warn("User {} already has active token in queue {}", userId, queueId);
            throw new ValidationException("You already have an active token in this queue");
        }

        // Step 5: Generate next token number
        Integer tokenNumber = queue.getNextTokenNumber();
        log.debug("Generated token number {} for queue {}", tokenNumber, queueId);

        // Step 6: Create token
        QueueToken token = QueueToken.builder()
            .tokenNumber(tokenNumber)
            .status(TokenStatus.WAITING)
            .bookingTime(LocalDateTime.now())
            .estimatedWaitTime(queue.estimateWaitTime())
            .user(user)
            .queue(queue)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // Step 7: Save token
        QueueToken savedToken = queueTokenRepository.save(token);

        // Step 8: Update queue
        queue.addToken(savedToken);
        queue.setWaitingCount(queueTokenRepository.countWaitingTokensByQueueId(queueId));
        queueRepository.save(queue);

        log.info("User {} joined queue {} with token {}", userId, queueId, tokenNumber);

        // Step 9: Broadcast to WebSocket subscribers
        webSocketService.broadcastCustomerJoined(queue, savedToken);

        return mapTokenToResponse(savedToken);
    }

    /**
     * Get all tokens in a queue.
     * 
     * @param queueId: Queue ID
     * @return: List of QueueTokenResponse
     * @throws ResourceNotFoundException: If queue not found
     */
    public List<QueueTokenResponse> getQueueTokens(Long queueId) {
        log.debug("Fetching tokens for queue: {}", queueId);

        // Verify queue exists
        if (!queueRepository.existsById(queueId)) {
            throw new ResourceNotFoundException("Queue not found");
        }

        return queueTokenRepository.findByQueueId(queueId)
            .stream()
            .map(this::mapTokenToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get waiting tokens in a queue (in order).
     * Used for customer display.
     * 
     * @param queueId: Queue ID
     * @return: List of waiting QueueTokenResponse
     */
    public List<QueueTokenResponse> getWaitingTokens(Long queueId) {
        log.debug("Fetching waiting tokens for queue: {}", queueId);

        if (!queueRepository.existsById(queueId)) {
            throw new ResourceNotFoundException("Queue not found");
        }

        return queueTokenRepository.findWaitingTokensByQueueId(queueId)
            .stream()
            .map(this::mapTokenToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get currently serving token in a queue.
     * 
     * @param queueId: Queue ID
     * @return: QueueTokenResponse (current token)
     * @throws ResourceNotFoundException: If queue not found or no active token
     */
    public QueueTokenResponse getActiveToken(Long queueId) {
        log.debug("Fetching active token for queue: {}", queueId);

        Queue queue = queueRepository.findById(queueId)
            .orElseThrow(() -> new ResourceNotFoundException("Queue not found"));

        if (queue.getCurrentToken() == null) {
            log.warn("No active token in queue: {}", queueId);
            throw new ResourceNotFoundException("No token is currently being served");
        }

        QueueToken activeToken = queueTokenRepository.findByQueueAndTokenNumber(queueId, queue.getCurrentToken())
            .orElseThrow(() -> new ResourceNotFoundException("Active token not found"));

        return mapTokenToResponse(activeToken);
    }

    /**
     * Get user's current token in a queue.
     * 
     * @param queueId: Queue ID
     * @param userId: User ID
     * @return: QueueTokenResponse
     * @throws ResourceNotFoundException: If no active token found
     */
    public QueueTokenResponse getUserToken(Long queueId, Long userId) {
        log.debug("Fetching user {} token in queue {}", userId, queueId);

        List<QueueToken> activeTokens = queueTokenRepository.findActiveTokensByUserId(userId);

        QueueToken userToken = activeTokens.stream()
            .filter(t -> t.getQueue().getId().equals(queueId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("You don't have an active token in this queue"));

        return mapTokenToResponse(userToken);
    }

    /**
     * Call next token (operator action).
     * Updates current token and changes token status.
     * 
     * @param queueId: Queue ID
     * @return: QueueTokenResponse of next token
     * @throws ResourceNotFoundException: If queue not found
     * @throws ValidationException: If no waiting tokens
     */
    public QueueTokenResponse callNextToken(Long queueId) {
        log.info("Calling next token for queue: {}", queueId);

        Queue queue = queueRepository.findById(queueId)
            .orElseThrow(() -> new ResourceNotFoundException("Queue not found"));

        QueueToken nextToken = queue.callNextToken();
        if (nextToken == null) {
            log.warn("No waiting tokens in queue: {}", queueId);
            throw new ValidationException("No waiting tokens in queue");
        }

        // Save updates
        queueTokenRepository.save(nextToken);
        queueRepository.save(queue);

        log.info("Token {} called in queue {}", nextToken.getTokenNumber(), queueId);
        return mapTokenToResponse(nextToken);
    }

    // ==========================================
    // Mapping Methods
    // ==========================================

    /**
     * Convert Queue entity to QueueResponse DTO.
     */
    private QueueResponse mapQueueToResponse(Queue queue) {
        Integer waitingCount = queueTokenRepository.countWaitingTokensByQueueId(queue.getId());
        queue.setWaitingCount(waitingCount);

        return QueueResponse.builder()
            .id(queue.getId())
            .queueName(queue.getQueueName())
            .serviceType(queue.getServiceType())
            .queueCode(queue.getQueueCode())
            .description(queue.getDescription())
            .status(queue.getStatus().toString())
            .currentToken(queue.getCurrentToken())
            .waitingCount(waitingCount)
            .averageServiceTime(queue.getAverageServiceTime())
            .createdAt(queue.getCreatedAt().format(dateFormatter))
            .updatedAt(queue.getUpdatedAt().format(dateFormatter))
            .build();
    }

    /**
     * Convert QueueToken entity to QueueTokenResponse DTO.
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
            .bookingTime(token.getBookingTime().format(dateFormatter))
            .serviceStartTime(token.getServiceStartTime() != null ? 
                token.getServiceStartTime().format(dateFormatter) : null)
            .serviceEndTime(token.getServiceEndTime() != null ? 
                token.getServiceEndTime().format(dateFormatter) : null)
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
}
