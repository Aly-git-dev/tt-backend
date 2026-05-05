package com.upiiz.platform_api.dto;

import com.upiiz.platform_api.entities.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String type,
        String title,
        String body,
        String targetType,
        String targetId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getTargetType(),
                n.getTargetId(),
                n.getReadAt() != null,
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}