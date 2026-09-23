package com.digitalqueue.domain.model;

/**
 * Enum representing different queue statuses.
 * 
 * Status Flow:
 * ACTIVE → PAUSED → ACTIVE → CLOSED
 * 
 * ACTIVE: Queue is accepting new members
 * PAUSED: Queue temporarily suspended (no new joins)
 * CLOSED: Queue permanently closed (service ended)
 */
public enum QueueStatus {
    ACTIVE("Queue is actively accepting members"),
    PAUSED("Queue is temporarily paused"),
    CLOSED("Queue is closed");

    private final String description;

    QueueStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
