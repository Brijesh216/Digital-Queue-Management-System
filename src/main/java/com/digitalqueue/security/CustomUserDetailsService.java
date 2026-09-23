package com.digitalqueue.security;

import com.digitalqueue.domain.entity.User;
import com.digitalqueue.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

/**
 * CustomUserDetailsService - Loads user details from database.
 * 
 * Spring Security Interface:
 * UserDetailsService is a core Spring Security interface
 * Used by authentication providers to load user information
 * 
 * Flow:
 * 1. User submits credentials (username/password)
 * 2. AuthenticationManager calls loadUserByUsername()
 * 3. This service loads user from database
 * 4. AuthenticationManager compares passwords
 * 5. If match: creates Authentication object
 * 6. If fail: throws BadCredentialsException
 * 
 * @Service: Spring component, autowired where needed
 * @Slf4j: Generates logger field
 */
@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Constructor Injection
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Load user by username (or email - flexible).
     * 
     * Called by Spring Security AuthenticationProvider
     * to fetch user details for authentication.
     * 
     * @param usernameOrEmail: Can be username or email (we support both)
     * @return: UserDetails object for authentication
     * @throws UsernameNotFoundException: If user not found
     */
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Loading user details for: {}", usernameOrEmail);

        // Try to find user by email or username
        User user = userRepository.findActiveByEmailOrUsername(usernameOrEmail)
            .orElseThrow(() -> {
                log.warn("User not found: {}", usernameOrEmail);
                return new UsernameNotFoundException("User not found: " + usernameOrEmail);
            });

        // Check if account is active
        if (!user.getActive()) {
            log.warn("User account is disabled: {}", usernameOrEmail);
            throw new UsernameNotFoundException("User account is disabled");
        }

        log.debug("User details loaded successfully for: {}", usernameOrEmail);

        // Return Spring Security UserDetails object
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            true, // enabled
            true, // accountNonExpired
            true, // credentialsNonExpired
            true, // accountNonLocked
            getAuthorities(user)
        );
    }

    /**
     * Convert user role to Spring Security authorities.
     * 
     * Authorities = Permissions/Roles that user has.
     * Spring Security prefix: "ROLE_" is standard convention.
     * 
     * Example:
     * User role: ADMIN
     * Authority: ROLE_ADMIN
     * 
     * This allows using @PreAuthorize("hasRole('ADMIN')")
     * 
     * @param user: User entity from database
     * @return: Collection of authorities
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // Convert user role to authority with ROLE_ prefix
        String authorityName = "ROLE_" + user.getRole().name();
        authorities.add(new SimpleGrantedAuthority(authorityName));

        log.debug("Authorities for user {}: {}", user.getUsername(), authorities);
        return authorities;
    }
}
