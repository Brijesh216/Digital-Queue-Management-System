package com.digitalqueue.domain.entity;

import com.digitalqueue.domain.model.TokenStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * QueueToken Entity - Represents a token in a queue.
 * 
 * A token is issued when a user joins a queue.
 * It has a unique token number, status, and tracks service progress.
 * 
 * Relationships:
 * @ManyToOne to User: Many tokens belong to one user
 * @ManyToOne to Queue: Many tokens belong to one queue
 * 
 * Entity Lifecycle:
 * 1. User joins queue
 * 2. QueueToken created with status WAITING
 * 3. Token number auto-generated
 * 4. When user's turn: status changed to CALLED
 * 5. During service: status SERVING
 * 6. After service: status COMPLETED
 */
@Entity
@Table(
    name = "queue_tokens",
    indexes = {
        @Index(name = "idx_queue_id", columnList = "queue_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_token_number", columnList = "queue_id, token_number", unique = true),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_booking_time", columnList = "booking_time")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueToken {

    // ==========================================
    // Primary Key
    // ==========================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==========================================
    // Token Information
    // ==========================================
    /**
     * Unique token number within a queue.
     * Example: Queue 1 has tokens 1, 2, 3, 4...
     * Format: AUTO_A001, AUTO_A002 (operator displays this)
     * 
     * Stored as integer for arithmetic (to find next token)
     */
    @Column(nullable = false)
    private Integer tokenNumber;

    /**
     * Current status of this token.
     * Stored as STRING enum (e.g., "WAITING", "CALLED", "COMPLETED")
     * 
     * Transitions:
     * WAITING → CALLED → SERVING → COMPLETED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus status;

    // ==========================================
    // Time Information
    // ==========================================
    /**
     * When user joined queue (got this token).
     * Used to calculate wait time.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime bookingTime;

    /**
     * Estimated wait time in minutes when token was issued.
     * Used to show customer: "Your estimated wait: 15 minutes"
     * Can be updated if queue moves faster/slower
     */
    @Column
    private Integer estimatedWaitTime;

    /**
     * When user's service started (status changed to SERVING).
     * NULL until service starts.
     */
    @Column
    private LocalDateTime serviceStartTime;

    /**
     * When user's service ended (status changed to COMPLETED).
     * NULL until service completes.
     */
    @Column
    private LocalDateTime serviceEndTime;

    // ==========================================
    // Relationships
    // ==========================================
    /**
     * User who got this token.
     * @ManyToOne: Many tokens → One User
     * @JoinColumn: Foreign key column name
     * @NonNull: Required field
     * 
     * Cascade: PERSIST (new token persists user if needed)
     *         MERGE (updates propagate)
     *         NOT DELETE (don't delete user if token deleted)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NonNull
    private User user;

    /**
     * Queue this token belongs to.
     * @ManyToOne: Many tokens → One Queue
     * 
     * Cascade: PERSIST, MERGE
     *         NOT DELETE (don't delete queue if token deleted)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false)
    @NonNull
    private Queue queue;

    // ==========================================
    // Audit Fields
    // ==========================================
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ==========================================
    // JPA Lifecycle Callbacks
    // ==========================================
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TokenStatus.WAITING;
        }
        if (this.bookingTime == null) {
            this.bookingTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==========================================
    // Business Logic Methods
    // ==========================================

    /**
     * Get display token number (with prefix).
     * Example: "A001", "B042"
     * Can be customized per queue
     * 
     * @return: Formatted token number
     */
    public String getDisplayToken() {
        // Format: First letter of service type + 3-digit number
        String prefix = queue.getServiceType().substring(0, 1).toUpperCase();
        return String.format("%s%03d", prefix, tokenNumber);
    }

    /**
     * Calculate actual wait time (duration from booking to now).
     * 
     * @return: Minutes waited
     */
    public long getActualWaitTimeInMinutes() {
        return java.time.temporal.ChronoUnit.MINUTES.between(bookingTime, LocalDateTime.now());
    }

    /**
     * Get remaining wait time (estimated - actual).
     * 
     * @return: Minutes remaining (can be negative if estimate was wrong)
     */
    public Integer getRemainingWaitTime() {
        if (estimatedWaitTime == null) {
            return null;
        }
        return (int) (estimatedWaitTime - getActualWaitTimeInMinutes());
    }

    /**
     * Check if token is still waiting for service.
     * 
     * @return: true if status is WAITING
     */
    public boolean isWaiting() {
        return this.status == TokenStatus.WAITING;
    }

    /**
     * Check if token is currently being served.
     * 
     * @return: true if status is SERVING
     */
    public boolean isServing() {
        return this.status == TokenStatus.SERVING;
    }

    /**
     * Check if service is completed.
     * 
     * @return: true if status is COMPLETED
     */
    public boolean isCompleted() {
        return this.status == TokenStatus.COMPLETED;
    }
}
