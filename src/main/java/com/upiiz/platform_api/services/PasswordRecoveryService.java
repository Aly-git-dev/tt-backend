package com.upiiz.platform_api.services;

import com.upiiz.platform_api.dto.ResetPasswordRequest;
import com.upiiz.platform_api.entities.PasswordResetToken;
import com.upiiz.platform_api.entities.User;
import com.upiiz.platform_api.repositories.PasswordResetTokenRepository;
import com.upiiz.platform_api.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Transactional
    public void requestPasswordReset(String email) {

        Optional<User> optionalUser = userRepository.findByEmailInst(email);

        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();

        boolean allowed = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("PROFESOR")
                        || role.getName().equals("ADMIN"));

        if (!allowed) {
            return;
        }

        // Invalidar tokens anteriores
        List<PasswordResetToken> activeTokens =
                tokenRepository.findByUserAndUsedFalse(user);

        for (PasswordResetToken t : activeTokens) {
            t.setUsed(true);
        }

        // Crear nuevo token
        PasswordResetToken token = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        tokenRepository.save(token);

        // Enviar correo
        mailService.sendPasswordResetEmail(
                user.getEmailInst(),
                token.getToken()
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }

        PasswordResetToken token = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (token.isUsed()) {
            throw new RuntimeException("Token ya usado");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        User user = token.getUser();

        boolean allowed = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("PROFESOR")
                        || role.getName().equals("ADMIN"));

        if (!allowed) {
            throw new RuntimeException("Usuario no autorizado");
        }

        token.setUsed(true);
        tokenRepository.save(token);
    }
}