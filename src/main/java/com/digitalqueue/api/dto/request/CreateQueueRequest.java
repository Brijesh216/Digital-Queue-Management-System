package com.digitalqueue.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateQueueRequest DTO - Request to create a new queue.
 * 
 * Used by: Admin/Operator to create service queues
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQueueRequest {

    /**
     * Display name of queue.
     * Examples: "Counter 1", "Window A", "Service Desk"
     */
    @NotBlank(message = "Queue name is required")
    @Size(max = 100, message = "Queue name cannot exceed 100 characters")
    private String queueName;

    /**
     * Type of service.
     * Examples: "BANKING", "MEDICAL", "DMV"
     */
    @NotBlank(message = "Service type is required")
    @Size(max = 50, message = "Service type cannot exceed 50 characters")
    private String serviceType;

    /**
     * Unique code identifier.
     * Examples: "BANK_001", "HOSP_OPD_01"
     */
    @NotBlank(message = "Queue code is required")
    @Size(max = 50, message = "Queue code cannot exceed 50 characters")
    private String queueCode;

    /**
     * Optional description
     */
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    /**
     * Average service time in minutes.
     * Used to estimate wait time.
     */
    private Integer averageServiceTime;
}
