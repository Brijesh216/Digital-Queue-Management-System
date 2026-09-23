package com.digitalqueue.api.dto.request;

import lombok.Builder;
import lombok.Data;

/**
 * JoinQueueRequest DTO - Request to join a queue.
 * 
 * Used by: Customers to get a token in a queue
 * Note: Most fields empty - just contains queueId
 * Could be extended later for preferences, etc.
 */
@Data
@Builder
public class JoinQueueRequest {
    // Fields can be added if needed for future extensions
    // Example: priority level, service preferences, etc.
}
