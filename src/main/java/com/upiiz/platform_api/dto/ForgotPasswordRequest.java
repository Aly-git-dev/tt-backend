package com.upiiz.platform_api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ForgotPasswordRequest(
        @JsonAlias({"emailInst", "correo", "correoInstitucional"})
        String email
) {}
