package com.splitwiseplusplus.service;

import com.splitwiseplusplus.dto.NotificationDTO;
import com.splitwiseplusplus.dto.PagedResponse;
import com.splitwiseplusplus.model.*;
import com.splitwiseplusplus.repository.*;
import com.splitwiseplusplus.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Notification Service — multi-channel notifications:
 * 1. In-app (DB)
 * 2. WebSocket (real-time)
 * 3. Email (async)
 * 4. Firebase Cloud Messaging (push)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final GroupMemberRepository memberRepository;
    private final NotificationWebSocketHandler webSocketHandler;
    private final EmailService emailService;
    private final FirebaseNotificationService firebaseService;

    // ── Trigger Methods ───────────────────────────────────────

    @Async
    @Transactional
    public void notifyExpenseAdded(Expense expense, User creator) {
        String title   = "New expense added";
        String message = String.format("%s added '%s' (%.2f %s) in group '%s'",
                creator.getName(), expense.getDescription(),
                expense.getAmount(), expense.getCurrency(),
                expense.getGroup().getName());

        List<GroupMember> members = memberRepository.findByGroupIdAndStatus(
                expense.getGroup().getId(), GroupMember.MemberStatus.ACTIVE);

        for (GroupMember member : members) {
            User recipient = member.getUser();
            if (recipient.getId().equals(creator.getId())) continue; // don't notify creator

            Notification notification = saveNotification(
                    recipient, title, message,
                    Notification.NotificationType.EXPENSE_ADDED,
                    expense.getId(), "EXPENSE"
            );

            // Real-time WebSocket
            pushWebSocket(recipient.getId(), notification);

            // Push notification (Firebase)
            if (recipient.isPushNotificationsEnabled()) {
                firebaseService.sendPushNotification(recipient.getId(), title, message);
            }
        }
    }

    @Async
    @Transactional
    public void notifyPaymentMade(Settlement settlement) {
        String title   = "Payment received";
        String message = String.format("%s paid you %.2f %s",
                settlement.getPayer().getName(),
                settlement.getAmount(),
                settlement.getCurrency());

        Notification notification = saveNotification(
                settlement.getReceiver(), title, message,
                Notification.NotificationType.PAYMENT_MADE,
                settlement.getId(), "SETTLEMENT"
        );

        pushWebSocket(settlement.getReceiver().getId(), notification);

        if (settlement.getReceiver().isPushNotificationsEnabled()) {
            firebaseService.sendPushNotification(
                    settlement.getReceiver().getId(), title, message);
        }
    }

    @Async
    @Transactional
    public void sendPaymentReminder(User debtor, User creditor, double amount, String currency, String groupName) {
        String title   = "Payment reminder";
        String message = String.format("You owe %s %.2f %s in group '%s'",
                creditor.getName(), amount, currency, groupName);

        Notification notification = saveNotification(
                debtor, title, message,
                Notification.NotificationType.PAYMENT_REMINDER,
                null, null
        );

        pushWebSocket(debtor.getId(), notification);

        if (debtor.isEmailNotificationsEnabled()) {
            emailService.sendPaymentReminder(debtor.getEmail(), debtor.getName(),
                    creditor.getName(), amount, currency, groupName);
        }

        if (debtor.isPushNotificationsEnabled()) {
            firebaseService.sendPushNotification(debtor.getId(), title, message);
        }
    }

    // ── Read / Query ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<NotificationDTO> getUserNotifications(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Notification> notifPage = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return PagedResponse.<NotificationDTO>builder()
                .content(notifPage.getContent().stream().map(this::mapToDTO).collect(Collectors.toList()))
                .page(page)
                .size(size)
                .totalElements(notifPage.getTotalElements())
                .totalPages(notifPage.getTotalPages())
                .last(notifPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationDTO markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        return mapToDTO(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    // ── Helpers ───────────────────────────────────────────────

    private Notification saveNotification(User user, String title, String message,
                                          Notification.NotificationType type,
                                          Long referenceId, String referenceType) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
        return notificationRepository.save(notification);
    }

    private void pushWebSocket(Long userId, Notification notification) {
        try {
            webSocketHandler.sendToUser(userId, mapToDTO(notification));
        } catch (Exception e) {
            log.warn("WebSocket push failed for user {}: {}", userId, e.getMessage());
        }
    }

    public NotificationDTO mapToDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
