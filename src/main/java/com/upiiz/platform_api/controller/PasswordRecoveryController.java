package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.dto.ForgotPasswordRequest;
import com.upiiz.platform_api.dto.ResetPasswordRequest;
import com.upiiz.platform_api.services.PasswordRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Recuperacion de contrasena", description = "Flujo publico para solicitar y aplicar restablecimiento de contrasena")
public class PasswordRecoveryController {

    private final PasswordRecoveryService service;

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperacion", description = "Genera un token de recuperacion si el correo existe y envia instrucciones.")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequest request
    ) {
        service.requestPasswordReset(request.email());

        return ResponseEntity.ok(Map.of(
                "message", "Si el correo es válido, se enviaron instrucciones."
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Restablecer contrasena", description = "Valida el token de recuperacion y actualiza la contrasena.")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordRequest request
    ) {
        service.resetPassword(request);

        return ResponseEntity.ok(Map.of(
                "message", "Contraseña actualizada correctamente"
        ));
    }
}
