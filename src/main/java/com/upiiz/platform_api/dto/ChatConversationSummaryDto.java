package com.upiiz.platform_api.dto;

import java.time.Instant;
import java.util.UUID;

public class ChatConversationSummaryDto {

    private Long id;
    private UUID otherUserId;
    private String otherName;
    private String otherAvatarUrl;
    private String allowedPair;
    private Instant lastMessageAt;
    private String lastMessagePreview;
    private UUID lastMessageSenderId;
    private Long unreadCount;

    public ChatConversationSummaryDto() {}

    public ChatConversationSummaryDto(
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

    public Long getId() { return id; }
    public UUID getOtherUserId() { return otherUserId; }
    public String getOtherName() { return otherName; }
    public String getOtherAvatarUrl() { return otherAvatarUrl; }
    public String getAllowedPair() { return allowedPair; }
    public Instant getLastMessageAt() { return lastMessageAt; }
    public String getLastMessagePreview() { return lastMessagePreview; }
    public UUID getLastMessageSenderId() { return lastMessageSenderId; }
    public Long getUnreadCount() { return unreadCount; }
}