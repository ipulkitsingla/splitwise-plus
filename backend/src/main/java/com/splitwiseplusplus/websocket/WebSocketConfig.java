package com.splitwiseplusplus.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket Configuration — enables STOMP messaging with a simple in-memory broker.
 * Clients subscribe to /user/{userId}/queue/notifications for real-time alerts.
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker for /queue and /topic destinations
        registry.enableSimpleBroker("/queue", "/topic");
        // Client sends messages to /app/... prefix
        registry.setApplicationDestinationPrefixes("/app");
        // User-specific destination prefix
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // SockJS fallback for non-WebSocket browsers
    }
}
