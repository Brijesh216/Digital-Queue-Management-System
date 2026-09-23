package com.digitalqueue.infrastructure.repository;

import com.digitalqueue.domain.entity.Queue;
import com.digitalqueue.domain.model.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * QueueRepository - Data Access Layer for Queue entity.
 * 
 * Provides CRUD operations and custom queries for queues.
 */
@Repository
public interface QueueRepository extends JpaRepository<Queue, Long> {

    /**
     * Find queue by unique code.
     * Code examples: "BANK_001", "HOSP_OPD_01"
     * 
     * @param queueCode: Unique queue code
     * @return: Queue with that code
     */
    Optional<Queue> findByQueueCode(String queueCode);

    /**
     * Find queue by name.
     * 
     * @param queueName: Name of the queue
     * @return: Queue with that name
     */
    Optional<Queue> findByQueueName(String queueName);

    /**
     * Find all active queues.
     * ACTIVE: Queue accepting new members
     * 
     * @return: List of active queues
     */
    List<Queue> findByStatus(QueueStatus status);

    /**
     * Find all queues for a specific service type.
     * Examples: "BANKING", "MEDICAL", "DMV"
     * 
     * @param serviceType: Type of service
     * @return: List of queues for that service
     */
    List<Queue> findByServiceType(String serviceType);

    /**
     * Custom query: Find all active queues for a service type.
     * Combines two conditions.
     * 
     * @param serviceType: Type of service
     * @param status: Queue status
     * @return: Filtered queues
     */
    @Query("SELECT q FROM Queue q WHERE q.serviceType = :serviceType AND q.status = :status")
    List<Queue> findActiveQueuesByServiceType(
        @Param("serviceType") String serviceType,
        @Param("status") QueueStatus status
    );

    /**
     * Find queues with waiting customers.
     * Join with tokens to find queues with WAITING tokens.
     * 
     * @return: Queues that have customers waiting
     */
    @Query("SELECT DISTINCT q FROM Queue q " +
           "JOIN q.tokens t " +
           "WHERE q.status = 'ACTIVE' AND t.status = 'WAITING'")
    List<Queue> findQueuesWithWaitingCustomers();

    /**
     * Check if queue code already exists.
     * 
     * @param queueCode: Code to check
     * @return: true if exists
     */
    boolean existsByQueueCode(String queueCode);
}
