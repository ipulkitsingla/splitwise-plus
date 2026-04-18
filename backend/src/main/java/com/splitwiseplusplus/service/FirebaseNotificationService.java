package com.splitwiseplusplus.service;

import com.google.firebase.messaging.*;
import com.splitwiseplusplus.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Firebase Cloud Messaging service.
 * Sends push notifications to all registered devices of a user.
 * Disabled gracefully if Firebase is not configured.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Value("${app.firebase.enabled:false}")
    private String firebaseEnabled;

    @Async
    public void sendPushNotification(Long userId, String title, String body) {
        if (!isFirebaseEnabled()) {
            log.debug("Firebase disabled — skipping push for user {}", userId);
            return;
        }

        List<String> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(dt -> dt.getToken())
                .toList();

        if (tokens.isEmpty()) return;

        for (String token : tokens) {
            try {
                Message message = Message.builder()
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                        .setToken(token)
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);
                log.debug("FCM sent to user {}: {}", userId, response);
            } catch (FirebaseMessagingException e) {
                log.warn("FCM failed for token {}: {}", token.substring(0, 10), e.getMessage());
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    deviceTokenRepository.deleteByToken(token);
                }
            }
        }
    }

    private boolean isFirebaseEnabled() {
        return Boolean.parseBoolean(firebaseEnabled == null ? "false" : firebaseEnabled.trim());
    }
}
