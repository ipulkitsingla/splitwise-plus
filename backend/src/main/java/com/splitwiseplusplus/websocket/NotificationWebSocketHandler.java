package com.splitwiseplusplus.websocket;

import com.splitwiseplusplus.dto.NotificationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * WebSocket Notification Handler — sends real-time notifications to specific users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

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

