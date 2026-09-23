package com.digitalqueue.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QueueTokenResponse DTO - Response containing token information.
 * 
 * Used by: 
 * - GET /queues/{id}/tokens (list all tokens)
 * - POST /queues/{id}/join (when user joins, returns their token)
 * - GET /queues/{id}/active-token (current serving token)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueueTokenResponse {

    private Long id;

    private Integer tokenNumber;

    private String displayToken;

    private String status;

    private Integer estimatedWaitTime;

    private Long actualWaitTimeMinutes;

    private Integer remainingWaitTime;

    private String bookingTime;

    private String serviceStartTime;

    private String serviceEndTime;

    private String createdAt;

    private String updatedAt;

    /**
     * User information (non-sensitive)
     */
    private TokenUserInfo user;

    /**
     * Queue information
     */
    private TokenQueueInfo queue;

    // ==========================================
    // Inner Classes
    // ==========================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenUserInfo {
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenQueueInfo {
        private Long id;
        private String queueName;
        private String serviceType;
        private String queueCode;
    }
}
