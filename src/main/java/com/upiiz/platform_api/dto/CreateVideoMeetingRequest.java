package com.upiiz.platform_api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateVideoMeetingRequest(
        @NotNull UUID appointmentId,
        @NotNull UUID hostUserId
) {}