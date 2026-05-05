package com.upiiz.platform_api.dto;

public record PushNotificationPayload(
        String title,
        String body,
        String icon,
        String url
) {}
