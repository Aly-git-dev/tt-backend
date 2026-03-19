package com.upiiz.platform_api.dto;

import java.time.Instant;
import java.util.UUID;

public class ConversationResponse {
    public Long id;
    public UUID otherUserId;
    public String otherName;
    public String otherAvatarUrl;
    public String allowedPair;
    public Instant lastMessageAt;
    public String lastMessagePreview;
    public UUID lastMessageSenderId;
    public Long unreadCount;

    public ConversationResponse() {
    }

    public ConversationResponse(
            Long id,
            UUID otherUserId,
            String otherName,
            String otherAvatarUrl,
            String allowedPair,
            Instant lastMessageAt,
            String lastMessagePreview,
            UUID lastMessageSenderId,
            Long unreadCount
    ) {
        this.id = id;
        this.otherUserId = otherUserId;
        this.otherName = otherName;
        this.otherAvatarUrl = otherAvatarUrl;
        this.allowedPair = allowedPair;
        this.lastMessageAt = lastMessageAt;
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageSenderId = lastMessageSenderId;
        this.unreadCount = unreadCount;
    }
}