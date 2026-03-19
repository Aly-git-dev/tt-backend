package com.upiiz.platform_api.dto;

public record ResetPasswordRequest(
        String token,
        String newPassword,
        String confirmPassword
) {}
