package com.upiiz.platform_api.dto;
import java.time.LocalDateTime;

public record VideoMeetingListItemResponse(
        Long id,
        Long appointmentId,
        String roomName,
        String meetingUrl,
        String status,
        LocalDateTime createdAt
) {}
