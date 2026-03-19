package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.ForgotPasswordRequest;
import com.upiiz.platform_api.dto.ResetPasswordRequest;
import com.upiiz.platform_api.services.PasswordRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/upiiz/public/v1/auth")
@RequiredArgsConstructor
public class PasswordRecoveryController {

    private final PasswordRecoveryService service;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequest request
    ) {
        service.requestPasswordReset(request.email());

        return ResponseEntity.ok(Map.of(
                "message", "Si el correo es válido, se enviaron instrucciones."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordRequest request
    ) {
        service.resetPassword(request);

        return ResponseEntity.ok(Map.of(
                "message", "Contraseña actualizada correctamente"
        ));
    }
}
