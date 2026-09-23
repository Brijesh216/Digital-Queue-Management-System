package com.digitalqueue.config;

import com.digitalqueue.infrastructure.jwt.JwtAuthenticationFilter;
import com.digitalqueue.infrastructure.jwt.JwtTokenProvider;
import com.digitalqueue.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * SecurityConfig - Spring Security configuration for the application.
 * 
 * Stateless Authentication (JWT):
 * - No sessions stored on server
 * - Each request includes token in header
 * - Scalable for microservices and mobile apps
 * 
 * @Configuration: Spring configuration class
 * @EnableMethodSecurity: Enables @PreAuthorize, @PostAuthorize annotations
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructor Injection
     */
    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                         JwtTokenProvider jwtTokenProvider) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // ==========================================
    // PasswordEncoder Bean
    // ==========================================

    /**
     * PasswordEncoder Bean - Configures BCrypt for password hashing.
     * 
     * BCrypt:
     * - One-way hashing algorithm (not reversible)
     * - Automatically generates random salt per password
     * - Includes salt in hash output (no separate storage needed)
     * - Configurable work factor (strength) - default is 10
     * 
     * @return PasswordEncoder bean (injected via constructor in AuthService)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // ==========================================
    // AuthenticationManager Bean
    // ==========================================

    /**
     * AuthenticationManager Bean - Spring Security's authentication gateway.
     * 
     * Responsibilities:
     * - Manages authentication process
     * - Delegates to AuthenticationProviders
     * - Throws exceptions if auth fails
     * 
     * Usage in AuthService.login():
     * authenticationManager.authenticate(
     *     new UsernamePasswordAuthenticationToken(username, password)
     * )
     * 
     * @param authenticationConfiguration: Auto-configured by Spring
     * @return: AuthenticationManager
     * @throws Exception: Configuration error
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // ==========================================
    // DaoAuthenticationProvider Bean
    // ==========================================

    /**
     * DaoAuthenticationProvider Bean - DAO-based authentication.
     * 
     * Flow:
     * 1. Receives username/password from login request
     * 2. Calls CustomUserDetailsService.loadUserByUsername()
     * 3. Compares plain password against hashed password
     * 4. Uses passwordEncoder.matches() for comparison
     * 5. Returns Authentication if match, throws if not
     * 
     * @return: DaoAuthenticationProvider configured
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ==========================================
    // JWT Authentication Filter Bean
    // ==========================================

    /**
     * JWT Authentication Filter Bean.
     * 
     * Purpose:
     * - Intercept HTTP requests
     * - Extract and validate JWT tokens
     * - Set SecurityContext for authenticated requests
     * 
     * @return: JwtAuthenticationFilter instance
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    // ==========================================
    // CORS Configuration Bean
    // ==========================================

    /**
     * CORS (Cross-Origin Resource Sharing) Configuration.
     * 
     * Purpose:
     * - Allow frontend (different domain) to call backend API
     * - Frontend on http://localhost:3000
     * - Backend on http://localhost:8080
     * 
     * CORS headers tell browser:
     * - Which origins can access API
     * - Which HTTP methods allowed
     * - Which headers can be sent
     * 
     * @return: CorsConfigurationSource for CORS setup
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins (where frontend runs)
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://localhost:5173",
            "http://localhost:5175",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5175"
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // Allowed request headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Content-Type",
            "Authorization",
            "X-Requested-With"
        ));

        // Exposed response headers
        configuration.setExposedHeaders(List.of(
            "Authorization"
        ));

        // Allow credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);

        // Max age for preflight cache (1 hour)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ==========================================
    // Security Filter Chain Bean
    // ==========================================

    /**
     * Security Filter Chain Bean - Main Spring Security configuration.
     * 
     * Configures:
     * 1. CORS settings
     * 2. CSRF (disabled for stateless API)
     * 3. Session management (stateless)
     * 4. Authorization rules (which endpoints need auth)
     * 5. Authentication filter (JWT)
     * 
     * @param http: HttpSecurity for configuration
     * @return: SecurityFilterChain
     * @throws Exception: Configuration error
     */
//     @Bean
//     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//         http
//             // ========== CORS Configuration ==========
//             .cors(cors -> cors.configurationSource(corsConfigurationSource()))

//             // ========== CSRF Protection ==========
//             // Disabled for stateless API (JWT doesn't need CSRF)
//             // CSRF needed for session-based auth with cookies
//             .csrf(csrf -> csrf.disable())

//             // ========== Session Management (Stateless) ==========
//             // STATELESS: No session stored on server
//             // Each request includes token (scalable)
//             .sessionManagement(session ->
//                 session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//             )

//             // ========== Authorization Rules ==========
//             // Order matters: more specific rules first
//             .authorizeHttpRequests(authz -> authz
//                 // ========== Public/Welcome ==========
//                 .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()
                
//                 // ========== Auth Endpoints ==========
//                 // Public: Health check, register, login
//                 .requestMatchers(HttpMethod.GET, "/api/v1/auth/health").permitAll()
//                 .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
//                 .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

//                 // ========== Queue Endpoints ==========
//                 // Public: Browse queues (no auth required)
//                 .requestMatchers(HttpMethod.GET, "/api/v1/queues").permitAll()
//                 .requestMatchers(HttpMethod.GET, "/api/v1/queues/**").permitAll()
                
//                 // Admin/Operator: Create, manage queues
//                 .requestMatchers(HttpMethod.POST, "/api/v1/queues").hasAnyRole("ADMIN", "OPERATOR")
//                 .requestMatchers(HttpMethod.PUT, "/api/v1/queues/**").hasAnyRole("ADMIN", "OPERATOR")
//                 .requestMatchers(HttpMethod.DELETE, "/api/v1/queues/**").hasAnyRole("ADMIN", "OPERATOR")
                
//                 // Operator: Call next token, view tokens
//                 .requestMatchers(HttpMethod.POST, "/api/v1/queues/*/next-token").hasAnyRole("ADMIN", "OPERATOR")
//                 .requestMatchers(HttpMethod.GET, "/api/v1/queues/*/tokens").hasAnyRole("ADMIN", "OPERATOR")
                
//                 // Customer: Join queue
//                 .requestMatchers(HttpMethod.POST, "/api/v1/queues/*/join").hasRole("CUSTOMER")
                
//                 // ========== Admin Endpoints ==========
//                 // Admin/Operator: Queue administration
//                 .requestMatchers(HttpMethod.POST, "/api/v1/admin/queues/**").hasAnyRole("ADMIN", "OPERATOR")
//                 .requestMatchers(HttpMethod.POST, "/api/v1/admin/tokens/**").hasAnyRole("ADMIN", "OPERATOR")
//                 .requestMatchers(HttpMethod.GET, "/api/v1/admin/tokens/**").hasAnyRole("ADMIN", "OPERATOR")
                
//                 // All other endpoints - require authentication
//                 .anyRequest().authenticated()
//             )

//             // ========== Authentication Provider ==========
//             // Use DaoAuthenticationProvider for username/password
//             .authenticationProvider(authenticationProvider())

//             // ========== Add JWT Filter ==========
//             // Add JWT filter before UsernamePasswordAuthenticationFilter
//             // This ensures token is validated before other filters run
//             .addFilterBefore(
//                 jwtAuthenticationFilter(),
//                 UsernamePasswordAuthenticationFilter.class
//             );

//         return http.build();
//     }
// }

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http

        // ==========================================
        // CORS CONFIGURATION
        // ==========================================
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // ==========================================
        // CSRF DISABLED (JWT is stateless)
        // ==========================================
        .csrf(csrf -> csrf.disable())

        // ==========================================
        // SESSION MANAGEMENT
        // ==========================================
        .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )

        // ==========================================
        // AUTHORIZATION RULES
        // ==========================================
        .authorizeHttpRequests(authz -> authz

                // ==========================================
                // PUBLIC ENDPOINTS
                // ==========================================

                .requestMatchers(
                        "/",
                        "/index.html",
                        "/favicon.ico",
                        "/ws/**"
                ).permitAll()

                // ==========================================
                // AUTH ENDPOINTS (PUBLIC)
                // ==========================================

                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register")
                .permitAll()

                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                .permitAll()

                .requestMatchers(HttpMethod.GET, "/api/v1/auth/health")
                .permitAll()

                // ==========================================
                // QUEUE ENDPOINTS
                // ==========================================

                // Public queue viewing
                .requestMatchers(HttpMethod.GET, "/api/v1/queues/**")
                .permitAll()

                // Create queue
                .requestMatchers(HttpMethod.POST, "/api/v1/queues")
                .hasAnyRole("ADMIN", "OPERATOR")

                // Update queue
                .requestMatchers(HttpMethod.PUT, "/api/v1/queues/**")
                .hasAnyRole("ADMIN", "OPERATOR")

                // Delete queue
                .requestMatchers(HttpMethod.DELETE, "/api/v1/queues/**")
                .hasAnyRole("ADMIN", "OPERATOR")

                // Call next token
                .requestMatchers(HttpMethod.POST, "/api/v1/queues/*/next-token")
                .hasAnyRole("ADMIN", "OPERATOR")

                // View queue tokens
                .requestMatchers(HttpMethod.GET, "/api/v1/queues/*/tokens")
                .hasAnyRole("ADMIN", "OPERATOR")

                // Join queue
                .requestMatchers(HttpMethod.POST, "/api/v1/queues/*/join")
                .hasRole("CUSTOMER")

                // ==========================================
                // ADMIN ENDPOINTS
                // ==========================================

                .requestMatchers("/api/v1/admin/**")
                .hasAnyRole("ADMIN", "OPERATOR")

                // ==========================================
                // EVERYTHING ELSE REQUIRES AUTH
                // ==========================================

                .anyRequest().authenticated()
        )

        // ==========================================
        // AUTHENTICATION PROVIDER
        // ==========================================
        .authenticationProvider(authenticationProvider())

        // ==========================================
        // JWT FILTER
        // ==========================================
        .addFilterBefore(
                jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class
        );

    return http.build();
}
}