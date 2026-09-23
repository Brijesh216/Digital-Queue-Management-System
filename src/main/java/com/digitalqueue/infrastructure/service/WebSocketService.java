package com.digitalqueue.infrastructure.service;

import com.digitalqueue.api.dto.message.QueueUpdateMessage;
import com.digitalqueue.api.dto.response.QueueTokenResponse;
import com.digitalqueue.domain.entity.Queue;
import com.digitalqueue.domain.entity.QueueToken;
import com.digitalqueue.domain.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * WebSocketService - Handles real-time WebSocket broadcasts for queue updates.
 * 
 * Responsibilities:
 * 1. Broadcast token status changes to all subscribers
 * 2. Notify when queue state changes (pause/resume)
 * 3. Send user-specific notifications
 * 4. Send admin notifications
 * 
 * Topics:
 * - /topic/queue/{queueId}/updates: Broadcast to all queue subscribers
 * - /topic/queue/{queueId}/currentToken: Current serving token
 * - /user/{userId}/queue/notification: Private notifications
 * - /topic/admin/broadcast: Admin announcements
 * 
 * Message Types:
 * - TOKEN_CALLED: New token called for service
 * - TOKEN_COMPLETED: Token finished service
 * - TOKEN_SKIPPED: Token marked absent
 * - QUEUE_PAUSED: Queue stopped accepting customers
 * - QUEUE_RESUMED: Queue reopened
 * - CUSTOMER_JOINED: New customer joined
 * - ADMIN_NOTIFICATION: Admin message
 * 
 * Integration Points:
 * - AdminService.callNextToken() → broadcastTokenCalled()
 * - AdminService.completeToken() → broadcastTokenCompleted()
 * - AdminService.skipToken() → broadcastTokenSkipped()
 * - AdminService.pauseQueue() → broadcastQueuePaused()
 * - AdminService.resumeQueue() → broadcastQueueResumed()
 * - QueueService.joinQueue() → broadcastCustomerJoined()
 * 
 * SimpMessagingTemplate:
 * - Spring's template for sending WebSocket messages
 * - convertAndSend(destination, payload): Send to all subscribers
 * - convertAndSendToUser(user, destination, payload): Send to specific user
 * - Automatically serializes objects to JSON
 */
@Service
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Constructor Injection.
     * SimpMessagingTemplate automatically configured by Spring.
     */
    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        log.debug("WebSocketService initialized");
    }

    // ==========================================
    // Token Status Updates
    // ==========================================

    /**
     * Broadcast when next token is called for service.
     * 
     * Triggered by: Admin calls POST /api/v1/admin/queues/{queueId}/next
     * 
     * Message Flow:
     * 1. Admin clicks "Call Next" button
     * 2. AdminController calls AdminService.callNextToken()
     * 3. AdminService broadcasts via WebSocketService.broadcastTokenCalled()
     * 4. Message sent to /topic/queue/{queueId}/updates
     * 5. All connected customers/operators receive message
     * 6. Frontend updates:
     *    - Display "Now Serving: A015"
     *    - Update waiting queue list
     *    - Show service counter/window
     * 
     * @param queue Queue entity (contains ID, name, service type, status)
     * @param token Token just called (contains token number, status, user)
     */
    public void broadcastTokenCalled(Queue queue, QueueToken token) {
        log.debug("Broadcasting token called for queue: {}, token: {}", queue.getId(), token.getTokenNumber());

        QueueUpdateMessage message = QueueUpdateMessage.builder()
                .messageType("TOKEN_CALLED")
                .queueId(queue.getId())
                .queueName(queue.getQueueName())
                .serviceType(queue.getServiceType())
                .queueStatus(queue.getStatus().toString())
                .currentToken(token.getTokenNumber())
                .waitingCount(queue.getWaitingCount())
                .averageServiceTime(queue.getAverageServiceTime())
                .timestamp(LocalDateTime.now())
                .message("Token " + token.getDisplayToken() + " called for service")
                .token(mapTokenToDetailMessage(token))
                .build();

        // Broadcast to all subscribers of this queue
        String destination = "/topic/queue/" + queue.getId() + "/updates";
        messagingTemplate.convertAndSend(destination, message);

        log.info("Token called message sent to {}", destination);
    }

    /**
     * Broadcast when token service is completed.
     * 
     * Triggered by: Admin calls POST /api/v1/admin/tokens/{tokenId}/complete
     * 
     * Message Flow:
     * 1. Admin marks token as completed
     * 2. AdminService.completeToken() processes
     * 3. Broadcasts via WebSocketService.broadcastTokenCompleted()
     * 4. Message sent to subscribers
     * 5. Frontend updates:
     *    - Remove completed token from queue display
     *    - Update statistics (service time, completed count)
     *    - Clear "Now Serving" if this was current token
     * 
     * @param queue Queue entity
     * @param token Token just completed
     */
    public void broadcastTokenCompleted(Queue queue, QueueToken token) {
        log.debug("Broadcasting token completed for queue: {}, token: {}", queue.getId(), token.getTokenNumber());

        QueueUpdateMessage message = QueueUpdateMessage.builder()
                .messageType("TOKEN_COMPLETED")
                .queueId(queue.getId())
                .queueName(queue.getQueueName())
                .serviceType(queue.getServiceType())
                .queueStatus(queue.getStatus().toString())
                .currentToken(queue.getCurrentToken())
                .waitingCount(queue.getWaitingCount())
                .averageServiceTime(queue.getAverageServiceTime())
                .timestamp(LocalDateTime.now())
                .message("Token " + token.getDisplayToken() + " service completed")
                .token(mapTokenToDetailMessage(token))
                .build();

        String destination = "/topic/queue/" + queue.getId() + "/updates";
        messagingTemplate.convertAndSend(destination, message);

        log.info("Token completed message sent to {}", destination);
    }

    /**
     * Broadcast when token is skipped (no-show).
     * 
     * Triggered by: Admin calls POST /api/v1/admin/tokens/{tokenId}/skip
     * 
     * Message Flow:
     * 1. Customer doesn't show up for service
     * 2. Admin marks token as ABSENT
     * 3. Broadcasts via WebSocketService.broadcastTokenSkipped()
     * 4. Message sent to subscribers
     * 5. Frontend updates:
     *    - Remove skipped token from queue
     *    - Show next token (auto-called)
     *    - Log no-show for statistics
     * 
     * @param queue Queue entity
     * @param skippedToken Token marked as absent
     * @param nextCalled Next token called after skip (can be null)
     */
    public void broadcastTokenSkipped(Queue queue, QueueToken skippedToken, QueueToken nextCalled) {
        log.debug("Broadcasting token skipped for queue: {}, token: {}", queue.getId(), skippedToken.getTokenNumber());

        QueueUpdateMessage message = QueueUpdateMessage.builder()
                .messageType("TOKEN_SKIPPED")
                .queueId(queue.getId())
                .queueName(queue.getQueueName())
                .serviceType(queue.getServiceType())
                .queueStatus(queue.getStatus().toString())
                .currentToken(nextCalled != null ? nextCalled.getTokenNumber() : queue.getCurrentToken())
                .waitingCount(queue.getWaitingCount())
                .averageServiceTime(queue.getAverageServiceTime())
                .timestamp(LocalDateTime.now())
                .message("Token " + skippedToken.getDisplayToken() + " marked as absent. Next token called.")
                .token(mapTokenToDetailMessage(nextCalled != null ? nextCalled : skippedToken))
                .build();

        String destination = "/topic/queue/" + queue.getId() + "/updates";
        messagingTemplate.convertAndSend(destination, message);

        log.info("Token skipped message sent to {}", destination);
    }

    // ==========================================
    // Queue Status Updates
    // ==========================================

    /**
     * Broadcast when queue is paused by admin.
     * 
     * Triggered by: Admin calls POST /api/v1/admin/queues/{queueId}/pause
     * 
     * Effect: New customers cannot join, but already-waiting customers remain.
     * 
     * Message Flow:
     * 1. Admin pauses queue (e.g., for break, maintenance)
     * 2. Broadcasts via WebSocketService.broadcastQueuePaused()
     * 3. Message sent to all queue subscribers
     * 4. Frontend updates:
     *    - Disable "Join Queue" button
     *    - Show "Queue is paused" message
     *    - Alert waiting customers
     *    - No new customers accepted
     * 
     * @param queue Queue just paused
     */
    public void broadcastQueuePaused(Queue queue) {
        log.debug("Broadcasting queue paused: {}", queue.getId());

        QueueUpdateMessage message = QueueUpdateMessage.builder()
                .messageType("QUEUE_PAUSED")
                .queueId(queue.getId())
                .queueName(queue.getQueueName())
                .serviceType(queue.getServiceType())
                .queueStatus("PAUSED")
                .currentToken(queue.getCurrentToken())
                .waitingCount(queue.getWaitingCount())
                .averageServiceTime(queue.getAverageServiceTime())
                .timestamp(LocalDateTime.now())
                .message("Queue paused. New customers cannot join.")
                .build();

        String destination = "/topic/queue/" + queue.getId() + "/updates";
        messagingTemplate.convertAndSend(destination, message);

        log.info("Queue paused message sent to {}", destination);
    }

    /**
     * Broadcast when queue is resumed by admin.
     * 
     * Triggered by: Admin calls POST /api/v1/admin/queues/{queueId}/resume
     * 
     * Effect: Queue reopened for new customers.
     * 
     * Message Flow:
     * 1. Admin resumes queue (break/maintenance finished)
     * 2. Broadcasts via WebSocketService.broadcastQueueResumed()
     * 3. Message sent to all subscribers
     * 4. Frontend updates:
     *    - Enable "Join Queue" button
     *    - Clear "Queue Paused" message
     *    - Allow new customers
     * 
     * @param queue Queue just resumed
     */
    public void broadcastQueueResumed(Queue queue) {
        log.debug("Broadcasting queue resumed: {}", queue.getId());

        QueueUpdateMessage message = QueueUpdateMessage.builder()
                .messageType("QUEUE_RESUMED")
                .queueId(queue.getId())
                .queueName(queue.getQueueName())
                .serviceType(queue.getServiceType())
                .queueStatus("ACTIVE")
                .currentToken(queue.getCurrentToken())
                .waitingCount(queue.getWaitingCount())
                .averageServiceTime(queue.getAverageServiceTime())
                .timestamp(LocalDateTime.now())
                .message("Queue resumed. New customers can join.")
                .build();

        String destination = "/topic/queue/" + queue.getId() + "/updates";
        messagingTemplate.convertAndSend(destination, message);

        log.info("Queue resumed message sent to {}", destination);
    }

    // ==========================================
    // Customer Actions
    // ==========================================

    /**
     * Broadcast when new customer joins queue.
     * 
     * Triggered by: Customer calls POST /api/v1/queues/{queueId}/join
     * 
     * Message Flow:
     * 1. Customer joins queue
     * 2. Broadcasts via WebSocketService.broadcastCustomerJoined()
     * 3. Message sent to all subscribers
     * 4. Frontend updates:
     *    - Update waiting count
     *    - Show new customer's token
     *    - Update queue display/list
     *    - Recalculate estimated wait times
     * 
     * @param queue Queue customer joined
     * @param token New token issued to customer
     */
    public void broadcastCustomerJoined(Queue queue, QueueToken token) {
        log.debug("Broadcasting customer joined queue: {}, token: {}", queue.getId(), token.getTokenNumber());

        QueueUpdateMessage message = QueueUpdateMessage.builder()
                .messageType("CUSTOMER_JOINED")
                .queueId(queue.getId())
                .queueName(queue.getQueueName())
                .serviceType(queue.getServiceType())
                .queueStatus(queue.getStatus().toString())
                .currentToken(queue.getCurrentToken())
                .waitingCount(queue.getWaitingCount())
                .averageServiceTime(queue.getAverageServiceTime())
                .timestamp(LocalDateTime.now())
                .message("New customer joined. Total waiting: " + queue.getWaitingCount())
                .token(mapTokenToDetailMessage(token))
                .build();

        String destination = "/topic/queue/" + queue.getId() + "/updates";
        messagingTemplate.convertAndSend(destination, message);

        log.info("Customer joined message sent to {}", destination);
    }

    // ==========================================
    // User-Specific Notifications
    // ==========================================

    /**
     * Send notification to specific user (private message).
     * 
     * Use Cases:
     * 1. "Your token is called, please go to counter"
     * 2. "Queue is full, try again later"
     * 3. "Your service is ready"
     * 4. "Service interrupted, try again"
     * 
     * Message Flow:
     * 1. Event occurs (e.g., token called)
     * 2. WebSocketService.notifyUser() called with userId
     * 3. Message sent to /user/{userId}/queue/notification
     * 4. Only that user receives message
     * 5. Frontend displays in user's notification area
     * 
     * @param userId User to notify
     * @param message User notification details
     */
    public void notifyUser(Long userId, QueueUpdateMessage message) {
        log.debug("Notifying user: {} with message: {}", userId, message.getMessageType());

        String destination = "/user/" + userId + "/queue/notification";
        messagingTemplate.convertAndSendToUser(userId.toString(), destination, message);

        log.info("User notification sent to user {}", userId);
    }

    // ==========================================
    // Admin Announcements
    // ==========================================

    /**
     * Broadcast admin announcement to all connected clients.
     * 
     * Use Cases:
     * 1. System maintenance notice
     * 2. Service temporary closure
     * 3. Emergency alert
     * 4. Important information
     * 
     * @param message Admin announcement
     */
    public void broadcastAdminNotification(QueueUpdateMessage message) {
        log.debug("Broadcasting admin notification: {}", message.getMessage());

        String destination = "/topic/admin/broadcast";
        messagingTemplate.convertAndSend(destination, message);

        log.info("Admin notification sent to {}", destination);
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    /**
     * Map QueueToken entity to WebSocket message detail.
     * 
     * Converts database entity to DTO suitable for WebSocket transmission.
     * Includes only necessary information (no sensitive data).
     * 
     * @param token QueueToken entity
     * @return TokenDetail for inclusion in message
     */
    private QueueUpdateMessage.TokenDetail mapTokenToDetailMessage(QueueToken token) {
        if (token == null) {
            return null;
        }

        return QueueUpdateMessage.TokenDetail.builder()
                .id(token.getId())
                .tokenNumber(token.getTokenNumber())
                .displayToken(token.getDisplayToken())
                .status(token.getStatus().toString())
                .estimatedWaitTime(token.getEstimatedWaitTime())
                .bookingTime(token.getBookingTime() != null ? token.getBookingTime().toString() : null)
                .serviceStartTime(token.getServiceStartTime() != null ? token.getServiceStartTime().toString() : null)
                .serviceEndTime(token.getServiceEndTime() != null ? token.getServiceEndTime().toString() : null)
                .user(mapUserToDetailMessage(token.getUser()))
                .build();
    }

    /**
     * Map User entity to WebSocket message detail.
     * 
     * Includes only public information (no passwords, sensitive data).
     * 
     * @param user User entity
     * @return TokenUser for inclusion in token detail
     */
    private QueueUpdateMessage.TokenUser mapUserToDetailMessage(User user) {
        if (user == null) {
            return null;
        }

        return QueueUpdateMessage.TokenUser.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}
