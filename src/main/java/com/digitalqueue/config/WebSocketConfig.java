package com.digitalqueue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration - STOMP over WebSocket.
 * 
 * STOMP (Simple Text Oriented Messaging Protocol):
 * - Frame-based protocol for messaging
 * - Works over WebSocket, TCP, or other connections
 * - Provides publish/subscribe messaging patterns
 * - Better than raw WebSocket for complex messaging needs
 * 
 * Key Components:
 * 1. Message Broker: Routes messages between clients
 * 2. WebSocket Endpoint: Client connection point
 * 3. Application Destination: @Controller message handlers
 * 4. User Destination: /user/* for specific user messages
 * 
 * Flow:
 * 1. Client opens WebSocket connection to /ws endpoint
 * 2. Client subscribes to topics like /topic/queue/1/updates
 * 3. Server broadcasts messages to all subscribers
 * 4. Client receives updates in real-time
 * 
 * Topics in this application:
 * - /topic/queue/{queueId}/updates: General queue updates
 * - /topic/queue/{queueId}/currentToken: Current serving token
 * - /user/{userId}/queue/notification: User-specific notifications
 * - /topic/admin/broadcast: Admin announcements
 * 
 * Configuration Benefits:
 * - In-memory message broker for development/testing
 * - STOMP protocol for structured messaging
 * - Fallback to SockJS for browsers without WebSocket support
 * - Path rewriting to handle proxy/load balancer scenarios
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Create a TaskScheduler bean for handling WebSocket heartbeats.
     * 
     * Required by SimpleBrokerMessageHandler when heartbeats are configured.
     * The TaskScheduler manages background threads for sending periodic heartbeat messages
     * to keep WebSocket connections alive and detect stale connections.
     * 
     * ThreadPoolTaskScheduler Configuration:
     * - poolSize: Number of threads for heartbeat scheduling
     * - threadNamePrefix: Identifies scheduler threads in logs
     * - awaitTerminationSeconds: Graceful shutdown timeout
     * - waitForTasksToCompleteOnShutdown: Allow tasks to complete before shutdown
     * - initialize: Start the scheduler on application startup
     * 
     * @return TaskScheduler for managing scheduled tasks
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Configure MessageBroker for handling subscriptions and broadcasting.
     * 
     * Responsibility:
     * - Define destination prefixes clients can subscribe to
     * - Enable STOMP broker relay or in-memory broker
     * - Configure heartbeat to keep connections alive
     * 
     * Destination Prefixes:
     * - /topic: Broadcast to all subscribers (one-to-many)
     * - /queue: Point-to-point messaging (one-to-one) - for private messages
     * - /user: User-specific topics
     * 
     * In-Memory Broker:
     * - Suitable for development and single-instance deployments
     * - Messages stored in memory (not persistent)
     * - No external message broker needed (like RabbitMQ, ActiveMQ)
     * 
     * For Production:
     * - Use Message Broker Relay with RabbitMQ, ActiveMQ, or Kafka
     * - Allows clustering across multiple servers
     * - Persistent message queuing
     * 
     * @param registry MessageBrokerRegistry for configuration
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory message broker
        // This stores subscriptions and routes messages to subscribers
        registry.enableSimpleBroker("/topic", "/queue");

        // Set application destination prefix
        // Controllers use @MessageMapping("/endpoint")
        // Full destination becomes /app/endpoint
        registry.setApplicationDestinationPrefixes("/app");

        // User destination prefix for private messages
        // @SendToUser sends to /user/{userId}/queue/notifications
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints for WebSocket connections.
     * 
     * Endpoint: /ws
     * - WebSocket connection URL: ws://localhost:3000/ws
     * - Client establishes persistent connection
     * - Can fall back to SockJS if WebSocket not supported
     * 
     * Allowed Origins:
     * - localhost:3000: Development
     * - localhost:3001: Frontend dev server (if separate)
     * - Production: Replace with actual domain
     * 
     * SockJS Fallback:
     * - Provides fallback for browsers without WebSocket support
     * - Uses long-polling, iframe-based, or other methods
     * - Transparent to client code
     * 
     * Without SockJS:
     * - WebSocket required (fails on IE10, some proxies)
     * 
     * With SockJS:
     * - Falls back automatically if WebSocket unavailable
     * - Better compatibility but slight overhead
     * 
     * @param registry StompEndpointRegistry for registration
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("http://localhost:3000", "http://localhost:3001", "http://localhost:5173", "http://localhost:5175",
                "http://127.0.0.1:3000", "http://127.0.0.1:3001", "http://127.0.0.1:5173", "http://127.0.0.1:5175")
                .withSockJS();
    }
}
