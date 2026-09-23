package com.digitalqueue.domain.model;

/**
 * Enum representing different user roles in the system.
 * 
 * Roles:
 * - ADMIN: Full system access, user management, analytics
 * - OPERATOR: Queue management, token issuance, customer support
 * - CUSTOMER: Join queues, view status, receive notifications
 */
public enum UserRole {
    ADMIN,
    OPERATOR,
    CUSTOMER
}
