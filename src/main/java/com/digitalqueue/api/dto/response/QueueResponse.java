package com.digitalqueue.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QueueResponse DTO - Response containing queue information.
 * 
 * Used by: GET /queues/{id} endpoint
 * Contains queue details but NOT detailed token list (use separate endpoint)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueueResponse {

    private Long id;

    private String queueName;

    private String serviceType;

    private String queueCode;

    private String description;

    private String status;

    private Integer currentToken;

    private Integer waitingCount;

    private Integer averageServiceTime;

    private String createdAt;

    private String updatedAt;
}
