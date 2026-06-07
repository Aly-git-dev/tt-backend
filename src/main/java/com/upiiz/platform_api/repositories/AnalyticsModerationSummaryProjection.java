package com.upiiz.platform_api.repositories;

public interface AnalyticsModerationSummaryProjection {
    Long getBannedUsers();
    Long getActiveUsers();
    Long getResolvedForumReports();
    Long getDismissedForumReports();
    Long getResolvedMessageReports();
    Long getDismissedMessageReports();
}
