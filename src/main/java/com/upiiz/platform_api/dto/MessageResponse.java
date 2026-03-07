package com.upiiz.platform_api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MessageResponse {
    public Long id;
    public Long conversationId;
    public UUID senderId;
    public String content;
    public String contentType;
    public String status;
    public Instant createdAt;
    public List<AttachmentResponse> attachments;
}
