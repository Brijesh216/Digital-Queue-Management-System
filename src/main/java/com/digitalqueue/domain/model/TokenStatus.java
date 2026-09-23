package com.digitalqueue.domain.model;

/**
 * Enum representing different token statuses.
 * 
 * Status Flow:
 * WAITING → CALLED → SERVING → COMPLETED
 * WAITING → CANCELLED (if user cancels)
 * 
 * WAITING: Token issued, user waiting for service
 * CALLED: User's turn called, currently being served
 * SERVING: User is being served
 * COMPLETED: Service completed successfully
 * CANCELLED: Token cancelled by user or operator
 * ABSENT: User didn't show up when called
 */
public enum TokenStatus {
    WAITING("Waiting for service"),
    CALLED("User's turn called"),
    SERVING("Currently being served"),
    COMPLETED("Service completed"),
    CANCELLED("Token cancelled"),
    ABSENT("User absent when called");

    private final String description;

    TokenStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
