package com.digitalqueue.infrastructure.repository;

import com.digitalqueue.domain.entity.QueueToken;
import com.digitalqueue.domain.model.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * QueueTokenRepository - Data Access Layer for QueueToken entity.
 * 
 * Provides CRUD operations and custom queries for queue tokens.
 */
@Repository
public interface QueueTokenRepository extends JpaRepository<QueueToken, Long> {

    /**
     * Find all tokens in a specific queue.
     * 
     * @param queueId: ID of the queue
     * @return: List of all tokens in that queue
     */
    List<QueueToken> findByQueueId(Long queueId);

    /**
     * Find all waiting tokens in a queue (in order).
     * Used to show waiting customers.
     * 
     * @param queueId: ID of the queue
     * @return: Tokens sorted by token number
     */
    @Query("SELECT t FROM QueueToken t " +
           "WHERE t.queue.id = :queueId AND t.status = 'WAITING' " +
           "ORDER BY t.tokenNumber ASC")
    List<QueueToken> findWaitingTokensByQueueId(@Param("queueId") Long queueId);

    /**
     * Find next token to be called (first waiting token).
     * 
     * @param queueId: ID of the queue
     * @return: Next waiting token
     */
    @Query("SELECT t FROM QueueToken t " +
           "WHERE t.queue.id = :queueId AND t.status = 'WAITING' " +
           "ORDER BY t.tokenNumber ASC LIMIT 1")
    Optional<QueueToken> findNextWaitingToken(@Param("queueId") Long queueId);

    /**
     * Find currently serving token in a queue.
     * 
     * @param queueId: ID of the queue
     * @return: Token with SERVING status
     */
    Optional<QueueToken> findByQueueIdAndStatus(Long queueId, TokenStatus status);

    /**
     * Find token by queue and token number.
     * Token number is unique within a queue.
     * 
     * @param queueId: ID of the queue
     * @param tokenNumber: Token number
     * @return: QueueToken if found
     */
    @Query("SELECT t FROM QueueToken t " +
           "WHERE t.queue.id = :queueId AND t.tokenNumber = :tokenNumber")
    Optional<QueueToken> findByQueueAndTokenNumber(
        @Param("queueId") Long queueId,
        @Param("tokenNumber") Integer tokenNumber
    );

    /**
     * Find all tokens for a specific user.
     * User can have tokens in multiple queues.
     * 
     * @param userId: ID of the user
     * @return: All tokens of that user
     */
    List<QueueToken> findByUserId(Long userId);

    /**
     * Find active (WAITING or SERVING) tokens for a user.
     * Used to check if user is already in queue.
     * 
     * @param userId: ID of the user
     * @return: Active tokens of user
     */
    @Query("SELECT t FROM QueueToken t " +
           "WHERE t.user.id = :userId AND (t.status = 'WAITING' OR t.status = 'SERVING')")
    List<QueueToken> findActiveTokensByUserId(@Param("userId") Long userId);

    /**
     * Find completed tokens for a user (history).
     * 
     * @param userId: ID of the user
     * @return: Completed tokens
     */
    @Query("SELECT t FROM QueueToken t " +
           "WHERE t.user.id = :userId AND t.status = 'COMPLETED' " +
           "ORDER BY t.serviceEndTime DESC")
    List<QueueToken> findCompletedTokensByUserId(@Param("userId") Long userId);

    /**
     * Find all waiting tokens (across all queues).
     * Used for admin dashboard.
     * 
     * @return: All waiting tokens
     */
    @Query("SELECT t FROM QueueToken t " +
           "WHERE t.status = 'WAITING' " +
           "ORDER BY t.bookingTime ASC")
    List<QueueToken> findAllWaitingTokens();

    /**
     * Find tokens created after a certain time (recent registrations).
     * 
     * @param since: Time threshold
     * @return: Tokens created after this time
     */
    List<QueueToken> findByBookingTimeAfter(LocalDateTime since);

    /**
     * Count waiting tokens in a queue.
     * More efficient than loading all tokens.
     * 
     * @param queueId: ID of the queue
     * @return: Number of waiting tokens
     */
    @Query("SELECT COUNT(t) FROM QueueToken t " +
           "WHERE t.queue.id = :queueId AND t.status = 'WAITING'")
    Integer countWaitingTokensByQueueId(@Param("queueId") Long queueId);

    /**
     * Check if user already has active token in queue.
     * Prevents duplicate joins.
     * 
     * @param userId: ID of the user
     * @param queueId: ID of the queue
     * @return: true if user has active token
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END " +
           "FROM QueueToken t " +
           "WHERE t.user.id = :userId AND t.queue.id = :queueId " +
           "AND (t.status = 'WAITING' OR t.status = 'SERVING')")
    boolean existsActiveTokenForUserInQueue(
        @Param("userId") Long userId,
        @Param("queueId") Long queueId
    );
}
