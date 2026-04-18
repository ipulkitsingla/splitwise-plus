package com.splitwiseplusplus.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitwiseplusplus.dto.NotificationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.stereotype.Component;
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

/**
 * WebSocket Notification Handler — sends real-time notifications to specific users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Push a notification to a specific user's WebSocket subscription.
     * Client subscribes to: /user/{userId}/queue/notifications
     */
    public void sendToUser(Long userId, NotificationDTO notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification
            );
            log.debug("WebSocket notification sent to user {}: {}", userId, notification.getTitle());
        } catch (Exception e) {
            log.warn("WebSocket send failed for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Broadcast a notification to all subscribers of a group topic.
     * Clients subscribe to: /topic/group/{groupId}
     */
    public void broadcastToGroup(Long groupId, NotificationDTO notification) {
        messagingTemplate.convertAndSend("/topic/group/" + groupId, notification);
    }
}
