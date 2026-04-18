package com.splitwiseplusplus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.splitwiseplusplus.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private Notification.NotificationType type;
    private Long referenceId;
    private String referenceType;
    private boolean read;
    private LocalDateTime createdAt;
}

