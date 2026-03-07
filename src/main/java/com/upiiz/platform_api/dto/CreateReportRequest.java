package com.upiiz.platform_api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CreateReportRequest {
    public Long messageId;
    public String reasonCode;   // SPAM/OFENSIVO/ACOSO/OTRO...
    public String description;
}