package com.upiiz.platform_api.dto;

import java.util.UUID;

public record JoinVideoMeetingResponse(
        UUID meetingId,
        String provider,
        String domain,
        String roomName,
        String meetingUrl,
        String displayName,
        UUID userId,
        String avatarUrl,
        String role,
        boolean host,
        boolean embedded,
        String appPath,
        String jwt,
        String externalApiUrl,
        String appId
) {}
