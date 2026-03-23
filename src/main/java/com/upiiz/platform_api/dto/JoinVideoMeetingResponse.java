package com.upiiz.platform_api.dto;

import java.util.UUID;

public record JoinVideoMeetingResponse(
        UUID meetingId,
        String provider,
        String domain,
        String roomName,
        String meetingUrl,
        String displayName
) {}