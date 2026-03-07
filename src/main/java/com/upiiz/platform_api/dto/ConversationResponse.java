package com.upiiz.platform_api.dto;

import java.time.Instant;
import java.util.UUID;

public class ConversationResponse {
    public Long id;
    public UUID otherUserId;
    public String allowedPair;
    public Instant lastMessageAt;
}