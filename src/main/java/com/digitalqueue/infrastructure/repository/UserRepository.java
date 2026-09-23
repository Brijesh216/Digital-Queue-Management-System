package com.digitalqueue.infrastructure.repository;

import com.digitalqueue.domain.entity.User;
import com.digitalqueue.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository - Data Access Layer for User entity.
 * 
 * What is JpaRepository?
 * - Provides built-in CRUD methods: save(), findById(), findAll(), delete(), etc.
 * - Generic type <User, Long>: Entity class and Primary Key type
 * - Spring automatically creates an implementation at runtime
 * 
 * Benefits:
 * - No need to write boilerplate SQL queries
 * - Automatic transaction management
 * - Support for custom query methods
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email address.
     * Method name convention: Spring Data automatically generates query
     * Equivalent SQL: SELECT * FROM users WHERE email = ?
     * 
     * Returns empty Optional if not found (safe, no null checks needed)
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username.
     * Equivalent SQL: SELECT * FROM users WHERE username = ?
     */
    Optional<User> findByUsername(String username);

    /**
     * Find all users with a specific role.
     * Equivalent SQL: SELECT * FROM users WHERE role = ?
     * 
     * Returns List (can be empty if no matches)
     */
    List<User> findByRole(UserRole role);

    /**
     * Check if user exists by email.
     * Equivalent SQL: SELECT COUNT(*) > 0 FROM users WHERE email = ?
     * More efficient than findByEmail when you only need boolean
     */
    boolean existsByEmail(String email);

    /**
     * Check if user exists by username.
     * Equivalent SQL: SELECT COUNT(*) > 0 FROM users WHERE username = ?
     */
    boolean existsByUsername(String username);

    /**
     * Custom query using @Query annotation.
     * Use JPQL (Java Persistence Query Language) syntax
     * Equivalent SQL: SELECT * FROM users WHERE active = true AND role = ?
     * 
     * :role is a named parameter (safer than positional)
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND u.role = :role")
    List<User> findActiveUsersByRole(@Param("role") UserRole role);

    /**
     * Find user by email or username.
     * More specific query for authentication
     */
    @Query("SELECT u FROM User u WHERE (u.email = :identifier OR u.username = :identifier) AND u.active = true")
    Optional<User> findActiveByEmailOrUsername(@Param("identifier") String identifier);
}
