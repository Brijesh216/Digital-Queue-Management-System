package com.digitalqueue.domain.entity;

import com.digitalqueue.domain.model.QueueStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Queue Entity - Represents a service queue.
 * 
 * Example: Bank counter, Hospital OPD, DMV, etc.
 * Each queue has:
 * - Name: "Counter 1", "Service A", etc.
 * - Service Type: "Bank", "Hospital", "DMV"
 * - Status: ACTIVE, PAUSED, CLOSED
 * - Current Token: Which token is being served
 * - List of tokens: All tokens in this queue
 * 
 * Relationships:
 * @OneToMany to QueueToken: One queue has many tokens
 * 
 * Entity Lifecycle:
 * 1. Admin creates queue
 * 2. Status: ACTIVE
 * 3. Users join queue (tokens created)
 * 4. Operator calls next token
 * 5. Status can change to PAUSED/CLOSED
 */
@Entity
@Table(
    name = "queues",
    indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_service_type", columnList = "service_type"),
        @Index(name = "idx_created_at", columnList = "created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Queue {

    // ==========================================
    // Primary Key
    // ==========================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==========================================
    // Queue Information
    // ==========================================
    /**
     * Display name of the queue.
     * Examples: "Counter 1", "Window A", "Service Desk"
     */
    @Column(nullable = false, length = 100)
    private String queueName;

    /**
     * Type of service provided by this queue.
     * Examples: "BANKING", "MEDICAL", "INSURANCE", "DMV"
     * Used to categorize and filter queues
     */
    @Column(nullable = false, length = 50)
    private String serviceType;

    /**
     * Unique code/identifier for the queue.
     * Examples: "BANK_001", "HOSP_OPD_01"
     * Used in display: "A001", "B042" (for tokens)
     */
    @Column(nullable = false, unique = true, length = 50)
    private String queueCode;

    /**
     * Description of service offered.
     * Examples: "General Banking", "Emergency Care"
     */
    @Column(length = 255)
    private String description;

    // ==========================================
    // Queue Status
    // ==========================================
    /**
     * Current status of the queue.
     * ACTIVE: Accepting new members
     * PAUSED: Temporarily closed (don't accept new members)
     * CLOSED: Permanently closed
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QueueStatus status = QueueStatus.ACTIVE;

    /**
     * Current token being served.
     * NULL when no one is being served
     * Updated when operator calls next token
     * Example: If currentToken = 5, it means token #5 is being served
     */
    @Column
    private Integer currentToken;

    /**
     * Highest token number issued so far.
     * Used to generate next token number.
     * Example: If lastTokenNumber = 25, next token will be 26
     * 
     * This is important for concurrency:
     * Multiple users joining simultaneously shouldn't get same token
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer lastTokenNumber = 0;

    /**
     * Number of people currently in queue (WAITING status).
     * Denormalized for performance (could be calculated from tokens).
     * Cache that gets updated when tokens added/removed.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer waitingCount = 0;

    /**
     * Average service time in minutes.
     * Used to estimate wait time for new customers.
     * Example: If averageServiceTime = 5, new customer sees ~5 min estimate
     */
    @Column
    private Integer averageServiceTime;

    // ==========================================
    // Relationships
    // ==========================================
    /**
     * All tokens issued in this queue.
     * @OneToMany: One queue has many tokens
     * @Cascade PERSIST: When queue saved, tokens saved too
     * @Cascade REMOVE: When queue deleted, tokens deleted too
     * @Cascade REFRESH: When queue refreshed, tokens refreshed
     * orphanRemoval: If token removed from list, delete from DB
     * 
     * fetch = LAZY: Don't load tokens automatically
     * Only load when explicitly accessed (lazy loading)
     * More efficient than EAGER loading all tokens
     */
    @OneToMany(
        mappedBy = "queue",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<QueueToken> tokens = new ArrayList<>();

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==========================================
    // Business Logic Methods
    // ==========================================

    /**
     * Generate next token number for new customer.
     * Increments lastTokenNumber.
     * 
     * Why synchronized in production?
     * Multiple threads might call this simultaneously.
     * Database locks handle concurrency in real deployment.
     * 
     * @return: Next token number (lastTokenNumber + 1)
     */
    public Integer getNextTokenNumber() {
        this.lastTokenNumber++;
        return this.lastTokenNumber;
    }

    /**
     * Add a token to this queue.
     * Also updates waiting count.
     * 
     * @param token: QueueToken to add
     */
    public void addToken(QueueToken token) {
        this.tokens.add(token);
        token.setQueue(this);
        if (token.isWaiting()) {
            this.waitingCount++;
        }
    }

    /**
     * Remove a token from this queue.
     * 
     * @param token: QueueToken to remove
     */
    public void removeToken(QueueToken token) {
        this.tokens.remove(token);
        if (token.isWaiting()) {
            this.waitingCount--;
            if (this.waitingCount < 0) {
                this.waitingCount = 0;
            }
        }
    }

    /**
     * Get next waiting token (after current token).
     * Used when operator clicks "Next" button.
     * 
     * @return: Next QueueToken with WAITING status
     */
    public QueueToken getNextWaitingToken() {
        return tokens.stream()
            .filter(QueueToken::isWaiting)
            .findFirst()
            .orElse(null);
    }

    /**
     * Call next token (update current token and status).
     * 
     * @return: Token that was called, or null if no waiting tokens
     */
    public QueueToken callNextToken() {
        QueueToken nextToken = getNextWaitingToken();
        if (nextToken != null) {
            this.currentToken = nextToken.getTokenNumber();
            nextToken.setStatus(com.digitalqueue.domain.model.TokenStatus.CALLED);
            this.waitingCount--;
            if (this.waitingCount < 0) {
                this.waitingCount = 0;
            }
        }
        return nextToken;
    }

    /**
     * Get count of currently waiting tokens.
     * 
     * @return: Number of tokens with WAITING status
     */
    public long getWaitingTokenCount() {
        return tokens.stream()
            .filter(QueueToken::isWaiting)
            .count();
    }

    /**
     * Check if queue is accepting new members.
     * 
     * @return: true if status is ACTIVE
     */
    public boolean isActive() {
        return this.status == QueueStatus.ACTIVE;
    }

    /**
     * Get list of all waiting tokens (in order).
     * 
     * @return: List of waiting tokens sorted by token number
     */
    public List<QueueToken> getWaitingTokens() {
        return tokens.stream()
            .filter(QueueToken::isWaiting)
            .sorted((a, b) -> a.getTokenNumber().compareTo(b.getTokenNumber()))
            .toList();
    }

    /**
     * Estimate wait time for new customer.
     * Formula: (waitingCount * averageServiceTime) + buffer
     * 
     * @return: Estimated minutes to wait
     */
    public Integer estimateWaitTime() {
        if (averageServiceTime == null || averageServiceTime == 0) {
            return 5; // Default estimate
        }
        return (int) getWaitingTokenCount() * averageServiceTime + 2;
    }
}
