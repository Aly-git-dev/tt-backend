package com.upiiz.platform_api.dto;

import java.time.Instant;

public class ReportSummaryResponse {
    public Long id;
    public String status;
    public String reasonCode;
    public Instant createdAt;
    public Long conversationId;
    public Long reportedMessageId;
}
