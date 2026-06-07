package com.upiiz.platform_api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VideoMeetingParticipantResponse(
        UUID userId,
        String displayName,
        String avatarUrl,
        String role,
        LocalDateTime joinedAt
) {}
