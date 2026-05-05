package com.upiiz.platform_api.dto;

import java.util.UUID;

public record CreateNotificationRequest(
        UUID userId,
        String type,
        String title,
        String body,
        String targetType,
        UUID targetId
) {}