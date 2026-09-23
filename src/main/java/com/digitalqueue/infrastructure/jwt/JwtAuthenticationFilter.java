package com.digitalqueue.infrastructure.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JwtAuthenticationFilter - Intercepts HTTP requests and validates JWT tokens.
 * 
 * Filter Flow:
 * 1. Request arrives at API endpoint
 * 2. Filter intercepts (before controller)
 * 3. Extract JWT from Authorization header
 * 4. Validate token signature and expiration
 * 5. Extract username and roles
 * 6. Create Authentication object
 * 7. Store in SecurityContext (Spring Security can access)
 * 8. Let request proceed to controller
 * 
 * OncePerRequestFilter:
 * - Ensures filter runs exactly once per request
 * - Better than Filter interface for this use case
 * - Handles forwards/includes properly
 * 
 * @Slf4j: Generates logger field
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructor
     * 
     * @param jwtTokenProvider: Injected by Spring
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Main filter logic - executes for each request.
     * 
     * @param request: HTTP request
     * @param response: HTTP response
     * @param filterChain: Next filter in chain
     * @throws ServletException: Servlet error
     * @throws IOException: IO error
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        log.debug("JWT Filter processing request: {} {}", request.getMethod(), request.getRequestURI());

        try {
            // Step 1: Extract JWT token from Authorization header
            String jwt = extractTokenFromRequest(request);

            // Step 2: If token exists, validate and authenticate
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Step 3: Extract username
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                
                // Step 4: Extract roles from token claims
                List<String> roles = getRolesFromToken(jwt);

                // Step 5: Create Authentication object
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        username,
                        null, // No password needed (token already validated)
                        roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList()
                    );

                // Step 6: Set authentication in SecurityContext
                // This makes authenticated user accessible throughout request
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("JWT authentication set for user: {} with roles: {}", username, roles);
            } else {
                log.debug("No valid JWT token found in request");
            }
        } catch (Exception ex) {
            log.error("JWT authentication failed: {}", ex.getMessage());
            // Continue to next filter (unauthenticated request)
            // Spring Security will deny access if endpoint requires authentication
        }

        // Continue to next filter (or endpoint if last filter)
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header.
     * 
     * Expected format:
     * Authorization: Bearer <token>
     * 
     * Example:
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * 
     * @param request: HTTP request
     * @return: Token string (empty if not found or invalid format)
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authorizationHeader) && 
            authorizationHeader.startsWith("Bearer ")) {
            // Remove "Bearer " prefix (7 characters)
            return authorizationHeader.substring(7);
        }

        return null;
    }

    /**
     * Extract user roles from JWT token.
     * Roles are stored in token claims as list.
     * 
     * @param jwt: JWT token
     * @return: List of role strings (with ROLE_ prefix)
     */
    @SuppressWarnings("unchecked")
    private List<String> getRolesFromToken(String jwt) {
        try {
            List<String> roles = (List<String>) jwtTokenProvider.getAllClaimsFromToken(jwt)
                .get("roles");
            return roles != null ? roles : Collections.emptyList();
        } catch (Exception ex) {
            log.warn("Failed to extract roles from token: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}
