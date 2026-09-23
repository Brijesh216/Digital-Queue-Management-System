package com.digitalqueue.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import com.digitalqueue.domain.model.UserRole;
import java.time.LocalDateTime;

/**
 * User Entity - Represents a user in the Digital Queue System.
 * 
 * JPA Annotations:
 * @Entity: Marks this class as a JPA entity (maps to database table)
 * @Table: Specifies table name and indexes
 * @Id: Marks the primary key field
 * @GeneratedValue: Auto-generates ID values
 * @Column: Customizes column properties
 * @Enumerated: Tells JPA how to store enum values (STRING/ORDINAL)
 * 
 * Lombok Annotations:
 * @Data: Generates getters, setters, toString, equals, hashCode
 * @NoArgsConstructor: Generates no-argument constructor (required by JPA)
 * @AllArgsConstructor: Generates constructor with all fields
 * @Builder: Enables builder pattern for object creation
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_email", columnList = "email", unique = true),
        @Index(name = "idx_username", columnList = "username", unique = true)
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    // ==========================================
    // Primary Key
    // ==========================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==========================================
    // User Information
    // ==========================================
    @NotBlank(message = "Username cannot be empty")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email should be valid")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    @Column(length = 20)
    private String phoneNumber;

    // ==========================================
    // User Status & Role
    // ==========================================
    /**
     * Role of the user. Determines access level and permissions.
     * Stored as STRING in database (e.g., "ADMIN", "OPERATOR", "CUSTOMER")
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /**
     * Account status - true if active, false if disabled/deleted
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // ==========================================
    // Audit Fields (Track changes)
    // ==========================================
    /**
     * Timestamp when user was created
     * @CreationTimestamp would auto-populate this (in real production)
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when user was last updated
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ==========================================
    // Lifecycle Callbacks (JPA PrePersist, PreUpdate)
    // ==========================================
    /**
     * Automatically set timestamps before saving
     * Called by JPA before INSERT
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Automatically update timestamp before modification
     * Called by JPA before UPDATE
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
