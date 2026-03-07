package com.upiiz.platform_api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReportDetailResponse extends ReportSummaryResponse {
    public UUID reporterId;
    public UUID handledBy;
    public Instant handledAt;
    public List<ReportContextItem> context;
}