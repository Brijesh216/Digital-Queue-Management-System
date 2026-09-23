package com.digitalqueue.api.dto.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * QueueUpdateMessage - WebSocket message for real-time queue updates.
 * 
 * Sent whenever:
 * 1. Admin calls next token → CurrentToken updated
 * 2. Token status changes → Token transition (WAITING→CALLED→COMPLETED)
 * 3. Customer joins queue → Waiting count increased
 * 4. Queue paused/resumed → Queue status changed
 * 5. Token skipped → Next token called automatically
 * 
 * Message Flow:
 * 1. Admin calls /api/v1/admin/queues/{id}/next (REST)
 * 2. AdminService.callNextToken() processes request
 * 3. WebSocketService.broadcastQueueUpdate() sends WebSocket message
 * 4. Message published to /topic/queue/{queueId}/updates
 * 5. All subscribers receive message in real-time
 * 6. Frontend updates UI without page refresh
 * 
 * JSON Format Example:
 * {
 *   "messageType": "TOKEN_CALLED",
 *   "queueId": 1,
 *   "queueName": "Counter A",
 *   "serviceType": "BANKING",
 *   "queueStatus": "ACTIVE",
 *   "currentToken": 5,
 *   "waitingCount": 12,
 *   "averageServiceTime": 10,
 *   "totalTokens": 17,
 *   "token": {
 *     "id": 5,
 *     "tokenNumber": 5,
 *     "displayToken": "A005",
 *     "status": "CALLED",
 *     "estimatedWaitTime": 0,
 *     "bookingTime": "2026-05-27T10:15:30",
 *     "user": {
 *       "id": 2,
 *       "username": "customer1",
 *       "email": "customer1@example.com",
 *       "firstName": "John",
 *       "lastName": "Doe"
 *     }
 *   },
 *   "timestamp": "2026-05-27T10:20:45",
 *   "message": "Token A005 called for service"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueueUpdateMessage {

    // ==========================================
    // Message Metadata
    // ==========================================
    /**
     * Type of update event.
     * Helps frontend determine how to process and display message.
     * 
     * Values:
     * - TOKEN_CALLED: New token is being called for service
     * - TOKEN_COMPLETED: Current token finished service
     * - TOKEN_SKIPPED: Token marked as absent/no-show
     * - TOKEN_SERVING: Token currently being served (optional, not used initially)
     * - QUEUE_PAUSED: Queue paused, no new customers
     * - QUEUE_RESUMED: Queue reopened for customers
     * - CUSTOMER_JOINED: New customer joined queue
     * - QUEUE_CLOSED: Queue closed/unavailable
     * - ADMIN_NOTIFICATION: Admin broadcast message
     */
    private String messageType;

    /**
     * Human-readable message for logging/display.
     * Examples:
     * - "Token A001 called for service"
     * - "Queue paused by administrator"
     * - "Your queue is full, please try later"
     */
    private String message;

    /**
     * Timestamp when message was created.
     * Used to detect old/duplicate messages on client side.
     */
    private LocalDateTime timestamp;

    // ==========================================
    // Queue Information
    // ==========================================
    /**
     * Queue identifier this message relates to.
     * Frontend uses this to determine which queue was affected.
     * Prevents updating wrong queue on client side.
     */
    private Long queueId;

    /**
     * Human-readable queue name.
     * Example: "Counter 1", "Service A"
     */
    private String queueName;

    /**
     * Service type this queue handles.
     * Example: "BANKING", "HOSPITAL", "DMV"
     */
    private String serviceType;

    /**
     * Current status of queue.
     * Values: ACTIVE, PAUSED, CLOSED
     * Helps frontend determine if queue is accepting customers.
     */
    private String queueStatus;

    // ==========================================
    // Current Queue State
    // ==========================================
    /**
     * Current token number being served.
     * Displayed on operator screen: "Now Serving: A015"
     * Null if no token is being served.
     */
    private Integer currentToken;

    /**
     * Number of customers waiting in queue.
     * Shown to customers: "15 people ahead of you"
     */
    private Integer waitingCount;

    /**
     * Average service time per customer (in minutes).
     * Used to calculate estimated wait time.
     * Example: 10 means ~10 minutes per customer
     */
    private Integer averageServiceTime;

    /**
     * Total tokens in this queue (completed + waiting + served).
     * For analytics/statistics.
     */
    private Integer totalTokens;

    // ==========================================
    // Affected Token Details
    // ==========================================
    /**
     * Nested token object affected by this update.
     * Contains:
     * - Token number and display format
     * - Current status
     * - Booking/service times
     * - User who booked token
     * 
     * Null for queue-level updates (pause/resume).
     */
    private TokenDetail token;

    // ==========================================
    // Inner Classes
    // ==========================================
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenDetail {
        private Long id;
        private Integer tokenNumber;
        private String displayToken; // "A001", "B005", etc.
        private String status;       // "WAITING", "CALLED", "COMPLETED", etc.
        private Integer estimatedWaitTime;
        private String bookingTime;
        private String serviceStartTime;
        private String serviceEndTime;
        
        private TokenUser user;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenUser {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
    }
}
