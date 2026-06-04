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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return;
        }

        var optionalUser = userRepository.findByEmailInstIgnoreCase(normalizedEmail);
        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();
        if (!canUseLocalPasswordReset(user)) {
            return;
        }

        List<PasswordResetToken> activeTokens = tokenRepository.findByUserAndUsedFalse(user);
        for (PasswordResetToken activeToken : activeTokens) {
            activeToken.setUsed(true);
        }
        tokenRepository.saveAll(activeTokens);

        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .createdAt(now)
                .expiresAt(now.plusMinutes(30))
                .used(false)
                .build();

        tokenRepository.save(token);

        mailService.sendPasswordResetEmail(user.getEmailInst(), token.getToken());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Solicitud invalida");
        }

        if (!StringUtils.hasText(request.token())) {
            throw new IllegalArgumentException("Token invalido");
        }

        if (!StringUtils.hasText(request.newPassword()) || request.newPassword().length() < 6) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 6 caracteres");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Las contrasenas no coinciden");
        }

        PasswordResetToken token = tokenRepository.findByToken(request.token().trim())
                .orElseThrow(() -> new IllegalArgumentException("Token invalido"));

        if (token.isUsed()) {
            throw new IllegalStateException("Token ya usado");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token expirado");
        }

        User user = token.getUser();
        if (!canUseLocalPasswordReset(user)) {
            throw new IllegalStateException("Usuario no autorizado");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsed(true);

        userRepository.save(user);
        tokenRepository.save(token);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean canUseLocalPasswordReset(User user) {
        return user != null
                && user.isActive()
                && "LOCAL".equalsIgnoreCase(user.getAuthProvider())
                && StringUtils.hasText(user.getPasswordHash());
    }
}
