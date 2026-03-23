package com.upiiz.platform_api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VideoMeetingResponse(
        Long id,
        Long appointmentId,
        String provider,
        String roomName,
        String meetingUrl,
        UUID hostUserId,
        String status,
        UUID createdBy,
        UUID cancelledBy,
        String cancelReason,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}