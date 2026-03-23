package com.upiiz.platform_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelVideoMeetingRequest(
        @NotBlank
        @Size(max = 500)
        String reason
) {}